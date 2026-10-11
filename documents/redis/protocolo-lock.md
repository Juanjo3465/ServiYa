# Protocolo de lock distribuido — ServiYa

> Implementa la deuda #1 (exclusión mutua de los `@Scheduled` para desplegar ≥2 instancias sin doble
> ejecución). Mecanismo: **protocolo propio** `SET NX PX` + Lua tras la interfaz `DistributedLock`
> (ADR-003 / D-04). **No** ShedLock, **no** Redisson.

## Piezas

| Pieza | Dónde |
|---|---|
| Interfaz `DistributedLock` + `LockHandle` | `backend/.../shared/lock/` |
| Implementación `RedisDistributedLock` (sobre `coordRedisTemplate`) | `backend/.../shared/lock/` |
| Scripts Lua | `backend/serviya/src/main/resources/redis-scripts/*.lua` (classpath) |
| Catálogo de claves | `documents/redis/catalogo-claves.md` |
| Prueba de concurrencia (Testcontainers) | `backend/.../shared/lock/RedisDistributedLockTest.java` |

> **Ubicación de los scripts:** van en el **classpath del backend**, no en el `redis-scripts/` de la
> raíz, porque el contexto de build de la imagen Docker es `./backend/serviya` (todo lo de fuera no entra
> en la imagen) y se cargan con `ClassPathResource`. El `redis-scripts/` raíz queda como spec/README y como
> futuro origen compartido cuando haya servicios en otros lenguajes (Fase 9).

## Protocolo

1. **Adquirir:** generar un token único (UUID v4) por adquisición. Ejecutar `lock_acquire.lua`
   (`KEYS=[lock:{recurso}]`, `ARGV=[token, ttlMs]`). Devuelve 1 si adquirió, 0 si está ocupado.
   - Variante con **fencing** (`lock_acquire_fenced.lua`, `KEYS=[lock:{recurso}, fence:{recurso}]`):
     devuelve un fencing token estrictamente creciente (>0) o 0. Solo para secciones críticas por-entidad.
2. **Renovar** (operaciones largas): `lock_renew.lua` cada ~TTL/3 (`GET`+`PEXPIRE` si el token coincide).
   Si devuelve 0, el lock **se perdió**: abortar cuanto antes y registrar `WARN`.
3. **Liberar:** siempre al cerrar el `LockHandle` (`AutoCloseable`) con `lock_release.lua`
   (**compare-and-delete**: solo borra si el valor sigue siendo nuestro token). Nunca libera un lock ajeno.
4. **Fencing (solo si aplica):** la escritura protegida incluye el token y se rechaza si es menor que el
   último aplicado (o bloqueo optimista con columna `version`).

**TTL:** 2–3× el p99 de la sección crítica. Espera (`wait`): corta o nula.

## Modo de fallo (D-08) — fallo cerrado

Si Redis no responde, `tryAcquire`/`tryAcquireFenced` devuelven **vacío** (no se concede el lock) y
registran el error; la sección crítica **no se ejecuta**. Para los `@Scheduled` es lo correcto: mejor no
ejecutar que ejecutar dos veces. La tarea se retoma en el siguiente cron.

## Fencing en este slice (ADR-003)

Los 6 `@Scheduled` son **mantenimiento idempotente** (no hay escritura por-entidad que versionar): usan
`tryAcquire` **sin** fencing, por eso **no** crean claves `fence:`. El fencing está **disponible en la
API** (`tryAcquireFenced` + `LockHandle.fencingToken()`) para el primer caso por-entidad —la carrera de
propuestas PENDING duplicadas, deuda **B8**—, donde además tocará añadir columna `version`/`last_fencing_token`.

## Uso en los schedulers

Cada método `@Scheduled` envuelve su llamada en `distributedLock.runIfAcquired("sched:<tarea>", ttl, task)`:
adquiere → ejecuta → libera siempre. Con ≥2 instancias, solo la que gana el lock ejecuta; las demás omiten.
Las 6 claves están en el catálogo (`lock:{sched:...}`).

## Criterios de aceptación (Fase 2) — estado

- [x] Concurrencia: N hilos compitiendo por el mismo recurso → solo uno entra a la vez (`bajoConcurrenciaSoloUnoGana`).
- [x] Un proceso no puede liberar un lock ajeno (compare-and-delete) (`noSePuedeLiberarUnLockAjeno`).
- [x] Si el proceso muere, el lock expira solo tras el TTL (SET PX; verificado por el TTL en el catálogo).
- [x] La renovación mantiene el lock y detecta la pérdida (`renovarMantieneYDetectaPerdida`).
- [x] Fencing tokens estrictamente crecientes por recurso (`fencingTokensEstrictamenteCrecientes`).
- [x] Si Redis no responde, no se ejecuta y se registra el error (`fallaCerradoSiRedisNoResponde`, D-08).
- [x] Claves en el catálogo; este `protocolo-lock.md` escrito.

> La batería ampliada de concurrencia multi-instancia y la observabilidad del lock se abordan en la Fase 5
> (tracker de aplazamientos de la guía §2.1).
