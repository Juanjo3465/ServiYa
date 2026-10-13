# Servicios que usan Redis y sus usuarios ACL — ServiYa

> Tarea 5 de la Fase 0. **Un usuario ACL por servicio y por instancia** (regla §3.2). Contraseñas
> distintas por usuario y entorno (>= 32 chars aleatorios), gestionadas por `.env` (local) / gestor de
> secretos (prod, D-14 / ADR-004). El `users.acl` real **no** se versiona; sí la plantilla `users.acl.tpl`.

## Servicios iniciales (Vía B)

Hoy el sistema es un **monolito** → un solo servicio lógico: **`serviya`** (backend Spring Boot).
Al migrar a microservicios, cada servicio tendrá su propio par de usuarios con el mismo patrón
(`svc-<servicio>-coord`, `svc-<servicio>-cache`).

**Primer microservicio (2026-10-12): `notifications-service`** → usuario `svc-notifications-coord`. No usa locks
ni claves (su reintento se coordina con `SKIP LOCKED` en su propia BD); solo hace `PING` para saber si
`redis-coord` está caído y diferir los correos. Ver `documents/microservicios/notifications-service.md`.

## Usuarios ACL

### Instancia `redis-coord` (locks + rate limit)

| Usuario | Para qué | Patrón de claves | Permisos (resumen) |
|---|---|---|---|
| `default` | — | — | **Desactivado** (`off`) |
| `admin` | Operación humana de emergencia | `~*` | `+@all`; solo desde bastión; auditado |
| `healthcheck` | Healthcheck del contenedor | — | Solo `+ping` |
| `monitor` | `redis_exporter` (Fase 5) | `~*` | Solo lectura + `INFO`/`SLOWLOG`/`LATENCY`/... |
| `svc-serviya-coord` | El backend: locks (Fase 2) + rate limit (Fase 3) | `~lock:*` `~fence:*` `~rl:*` | R/W + scripting; `-@dangerous` |
| `svc-notifications-coord` | `notifications-service`: sondeo de disponibilidad (modo degradado D-07/1a) | ninguno (`resetkeys`) | Solo `+@connection +ping` |

> Comandos exactos de `svc-serviya-coord` (allowlist, deny-by-default) en `users.coord.acl.tpl`:
> `+@connection +ping +get +set +del +incr +pexpire +pttl +hmget +hset +time +zadd +zcard +zrange`
> `+zremrangebyscore +eval +evalsha +script|load +script|exists`. Los de rate limit (`+time` y los
> `z*` de la ventana deslizante) se añadieron en Fase 3: el ACL del slice los exige. **Al desplegar hay
> que re-renderizar** `users.coord.acl` desde el `.tpl` y recargar (`ACL LOAD` o reiniciar el contenedor).

### Instancia `redis-cache` (caché; se activa en Fase 6)

| Usuario | Para qué | Patrón de claves | Permisos (resumen) |
|---|---|---|---|
| `svc-serviya-cache` | El backend: caché | `~cache:serviya:*` | R/W + `SCAN`; `-@dangerous` |

> `client-name` de la app = `spring.application.name` (para identificar la conexión en `CLIENT LIST`).
> Durante las pruebas de cada fase, revisar `ACL LOG` y recortar permisos al mínimo necesario.
