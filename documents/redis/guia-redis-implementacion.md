# Guía de implementación de Redis: locks distribuidos, rate limiting y caché

> **Proyecto:** sistema distribuido, mayormente Java/Spring Boot, con posibilidad de servicios en otros lenguajes.
> **Usos de Redis:** (1) locks distribuidos, (2) rate limiting, (3) caché (fase posterior).
> **Despliegue:** contenedores Docker; a futuro servidor propio o nube (autogestionado o servicio gestionado).

---

## 0. Cómo usar este documento (leer primero, también la IA)

Este documento es la **fuente de verdad** para implementar Redis en el proyecto. Está pensado para trabajar con un asistente de IA fase por fase.

### Reglas para el asistente de IA

1. **Implementa una fase a la vez.** No adelantes trabajo de fases posteriores, salvo lo marcado como *"Preparar ahora para el futuro"*.
2. **Respeta las decisiones con estado `DECIDIDO`.** Si una decisión está `PENDIENTE`, **pregunta antes de implementar** lo que dependa de ella; no la tomes por tu cuenta.
3. **Respeta todas las convenciones de la sección 3** (claves, usuarios, configuración, scripts, métricas). Si algo no encaja, propón un cambio a la convención en vez de saltártela.
4. **Nunca:**
   - pongas contraseñas, tokens o hashes reales en el código, en `application.yml` o en la imagen;
   - publiques el puerto de Redis fuera de la red interna de Docker;
   - uses `KEYS`, `SELECT`, `FLUSHALL`/`FLUSHDB` en el código de la aplicación;
   - uses la serialización nativa de Java (`JdkSerializationRedisSerializer`);
   - crees claves sin TTL (salvo las excepciones documentadas en el catálogo de claves);
   - uses la etiqueta `latest` en imágenes;
   - registres en logs tokens de lock, contraseñas ni valores cacheados con datos personales.
5. **Cada fase termina** cuando se cumplen todos sus *criterios de aceptación* y se actualizan los documentos indicados en *"Documentar"*.
6. Al terminar cada fase, **resume qué se hizo, qué decisiones se aplicaron y qué quedó pendiente.**

### Prompt sugerido para cada fase

```
Lee docs/redis/guia-redis-implementacion.md completo.
Implementa únicamente la Fase N.
Antes de escribir código, lista las decisiones de la sección 2 de las que depende esta fase
y su estado. Si alguna está PENDIENTE, pregúntame antes de continuar.
Al terminar, verifica cada criterio de aceptación de la fase y dime cuáles se cumplen.
```

---

## 1. Visión general de la arquitectura

```
                    ┌───────────────────────── red backend (interna) ──────────────────────────┐
                    │                                                                          │
 Internet ──► app (Spring Boot) ──────► redis-coord   (locks + rate limiting)                  │
                    │      │            noeviction · AOF · volumen persistente                 │
                    │      │                                                                   │
                    │      └──────────► redis-cache   (caché, Fase 6)                          │
                    │                   allkeys-lfu · sin persistencia                         │
                    │                                                                          │
 otros servicios (Python, Go, ...) ──► mismos Redis, mismos protocolos (scripts Lua compartidos) │
                    └──────────────────────────────────────────────────────────────────────────┘
```

**Principios rectores:**

- **Dos instancias separadas por propósito**, porque la política de expulsión de memoria es global por instancia: los locks nunca deben ser expulsados; la caché sí debe poder expulsarse.
- **Redis es infraestructura auxiliar:** la garantía final de corrección vive en la base de datos principal. Los locks de Redis evitan trabajo duplicado; no son la única defensa contra inconsistencias.
- **Diseñar hoy como si mañana fuera Redis Cluster**, aunque se arranque con un solo nodo.
- **Interoperable entre lenguajes:** los algoritmos que comparten varios servicios (locks, rate limit) se definen como scripts Lua versionados, no como detalles internos de una librería Java.
- **Todo es configuración externa:** cambiar de Docker local a servidor propio o a la nube solo debe requerir cambiar variables de entorno.

---

## 2. Registro de decisiones

Cada decisión debe quedar documentada como un ADR (*Architecture Decision Record*) en `docs/redis/decisiones/` usando la plantilla del Anexo A.

**Estados:** `DECIDIDO` (aplicar), `PENDIENTE` (el equipo debe decidir; la IA debe preguntar), `RECOMENDADO` (propuesta por defecto si el equipo no indica otra cosa).

| ID | Decisión | Recomendación | Estado | Fase |
|---|---|---|---|---|
| D-01 | Separar `redis-coord` (locks + rate limit) y `redis-cache` | Sí, dos instancias desde el diseño | DECIDIDO | 1 |
| D-02 | Motor y versión | **Redis** (FOSS AGPLv3 desde v8), línea **8.2-alpine**; fijar el patch exacto parcheado al implementar | **DECIDIDO (motor: Redis)** · PENDIENTE (patch exacto) — 2026-10-10 | 1 |
| D-03 | Cliente Java | Lettuce (por defecto en Spring Data Redis), sin pool salvo comandos bloqueantes o `MULTI` | RECOMENDADO | 1 |
| D-04 | Mecanismo de lock | **Opción A:** Redisson (solo Java, watchdog incluido). **Opción B:** protocolo propio (`SET NX PX` + Lua) implementado en cada lenguaje. Recomendado **B** si algún recurso puede bloquearse desde varios lenguajes; A si todo lock será solo Java | **DECIDIDO: Opción B (protocolo propio)** — 2026-10-10. ShedLock descartado (ver §2.1) | 2 |
| D-05 | Garantía de corrección en recursos críticos | Fencing token (`INCR`) y/o bloqueo optimista (`version`) en la base de datos principal | RECOMENDADO | 2 |
| D-06 | Algoritmo de rate limit | Token bucket con script Lua compartido (interoperable). Bucket4j solo si el rate limit será exclusivamente Java | RECOMENDADO | 3 |
| D-07 | Comportamiento si Redis falla (rate limit) | Configurable **por política**: `OPEN` por defecto, `CLOSED` para endpoints sensibles (login, pagos, proveedores pagos) | PENDIENTE (definir qué políticas son CLOSED) | 3 |
| D-08 | Comportamiento si Redis falla (lock) | No ejecutar la sección crítica; devolver error/reintentar. Nunca continuar sin lock salvo decisión explícita por caso | RECOMENDADO | 2 |
| D-09 | Identificación del sujeto en rate limit | API key / usuario autenticado; IP solo para endpoints anónimos, confiando en `X-Forwarded-For` únicamente desde proxies conocidos | PENDIENTE | 3 |
| D-10 | Formato de serialización | JSON (Jackson) con DTOs explícitos, sin metadatos de tipo `@class` en datos compartidos entre lenguajes | DECIDIDO | 6 |
| D-11 | Persistencia `redis-coord` | AOF `everysec` + RDB periódico | RECOMENDADO | 1 / 7 |
| D-12 | Persistencia `redis-cache` | Ninguna | DECIDIDO | 6 |
| D-13 | Política de expulsión | `noeviction` en coord; `allkeys-lfu` (o `allkeys-lru`) en cache | DECIDIDO | 1 / 6 |
| D-14 | Gestión de secretos | Local: archivo `.env` no versionado. Producción: gestor de secretos (Vault / AWS Secrets Manager / Azure Key Vault / GCP Secret Manager) | **DECIDIDO (local: `.env` no versionado)** · PENDIENTE (gestor de prod) — 2026-10-10 | 1 / 7 |
| D-15 | TLS | Opcional en red Docker de un solo host; **obligatorio** entre hosts o en la nube | DECIDIDO | 7 |
| D-16 | Estrategia de alta disponibilidad | Etapa 1: nodo único + backups. Etapa 2: réplica + Sentinel. Etapa 3: Cluster o servicio gestionado | PENDIENTE (definir SLA objetivo) | 8 |
| D-17 | Destino de despliegue a futuro | Servidor propio vs servicio gestionado (ElastiCache/MemoryDB, Azure Managed Redis, Memorystore, Redis Cloud) | PENDIENTE | 8 |
| D-18 | Stack de observabilidad | Actuator + Micrometer + Prometheus + Grafana + `redis_exporter` | RECOMENDADO | 5 |

### 2.1 Adopción en ServiYa — decisiones y aplazamientos (Fase 0, 2026-10-10)

> **Contexto:** este sustrato Redis se adopta para resolver la **deuda #1** del documento canónico
> `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` (lock distribuido para los `@Scheduled`, bloqueante del despliegue
> en ≥2 instancias) y, por fases, las deudas **#6 / M13** (rate limiting distribuido) y **M34** (caché).

**Alcance elegido: Vía B (slice mínimo).** Se levanta solo lo necesario para bloquear los 6 schedulers,
respetando las convenciones de la §3 pero sin desplegar aún lo que no se usa:

- Fase 0 (solo lo que bloquea) + Fase 1 **reducida a `redis-coord`** + Fase 2 (`DistributedLock`) para
  los schedulers. **No** se levanta `redis-cache` hasta la Fase 6 (hoy estaría ocioso), pero **sí** se
  mantiene el contrato de configuración de ambas instancias (§3.3) desde el inicio.
- Schedulers a proteger: `requests` ×4 (`RequestMaintenanceScheduler`), reintento de notificaciones
  (`NotificationRetryScheduler`) y limpieza de reset-tokens (`PasswordResetCleanupScheduler`).

**Decisiones cerradas (2026-10-10):**

- **D-04 = Opción B (protocolo propio `SET NX PX` + Lua).** Un único sistema de locking coherente,
  interoperable y reutilizable. **ShedLock descartado**: usa su propio esquema de claves opaco, no pasa
  por la interfaz `DistributedLock`, no usa los scripts Lua ni la convención `lock:{...}` / ACL `~lock:*`
  → sería un cuerpo extraño y un segundo sistema de lock en paralelo.
- **Fencing token: NO se usa en los schedulers.** Las tareas de mantenimiento son idempotentes y batch
  (no hay escritura protegida por-entidad que versionar), así que basta la exclusión mutua. La API de
  `LockHandle` **expone** `fencingToken()` igualmente (D-05), para cuando se proteja una sección crítica
  por-entidad (p. ej. la carrera de propuestas PENDING, deuda **B8/#5** del ESTADO).
- **D-02 = motor Redis** (FOSS AGPLv3 desde v8), línea **8.2-alpine**. El **patch exacto** queda
  PENDIENTE: fijarlo al implementar verificando la última versión parcheada.
- **D-14 = local `.env` no versionado** (ya es la convención del repo). El **gestor de secretos de
  producción** queda PENDIENTE (Fase 7).

**Compatibilidad de versiones (stack actual: Spring Boot 3.5.5 / Java 21):**

- `spring-boot-starter-data-redis` → **gestionado por el parent 3.5.5** (no se declara versión). Trae
  **Lettuce 6.5.x**, compatible con Java 21 y con Redis 8.2 (RESP2/RESP3).
- Para la Vía B **no** hace falta Redisson ni otro cliente: se usa `StringRedisTemplate` +
  `DefaultRedisScript` para cargar los `.lua` de `redis-scripts/`.
- Redis server: imagen **`redis:8.2-alpine`**; patch exacto a fijar al implementar (D-02).

**Aplazamientos (hacer después — tracker).** _Todo lo que se posterga queda aquí para retomarlo._

| Aplazado | A qué fase | Nota |
|---|---|---|
| Instancia `redis-cache` (contenedor) | Fase 6 | Hoy ociosa; el contrato de config §3.3 sí se crea ya |
| Rate limiting distribuido (deuda #6 / M13) | Fase 3 | Reutiliza `redis-coord` y el mismo patrón Lua; hoy Bucket4j es local |
| Resiliencia (timeouts, circuit breaker, modos de fallo) | Fase 4 | D-07 / D-08 |
| Observabilidad (Micrometer/Prometheus/Grafana/`redis_exporter`) | Fase 5 | D-18 |
| Caché (M34) + serialización JSON | Fase 6 | D-10 / D-12 / D-13 |
| TLS, gestor de secretos de prod, backups, tuning de host, runbooks | Fase 7 | D-14 / D-15 |
| Actuator en puerto de management separado (no publicado al host) | Fase 7 | Hoy el health va sobre el 8080 público; en Fase 1 ya se puso `show-details: when_authorized` en el grupo `redis` para no filtrar detalles. Prod: `management.server.port` propio + sin `ports:` en compose |
| Alta disponibilidad (Sentinel/Cluster/gestionado) | Fase 8 | D-16 / D-17 |
| Interoperabilidad multilenguaje (clientes + pruebas de contrato) | Fase 9 | — |
| Usar fencing (`fence:` + columna `version`/`last_fencing_token`) en una tabla protegida | Al proteger una sección crítica por-entidad | D-05; p. ej. deuda **B8**. La API ya existe desde Fase 2 (`tryAcquireFenced` + `lock_acquire_fenced.lua`); falta el primer uso real |
| Batería ampliada de concurrencia multi-instancia + observabilidad del lock | Fase 5 | El slice (Fase 2) ya trae la prueba básica de exclusión mutua con Testcontainers (§3.7 del ESTADO) |

---

## 3. Estándares y convenciones (obligatorias)

### 3.1 Convención de claves

**Reglas generales:**

- Separador: `:`. Palabras compuestas en `kebab-case`. Todo en minúsculas.
- **Hash tag `{...}`** sobre la entidad principal, para que las claves relacionadas caigan en el mismo slot del Cluster y se puedan usar juntas en un script Lua.
- Sin espacios, sin datos personales en claro (emails, documentos). Si el sujeto es un dato personal o una IP, usar un hash (ej. SHA-256 truncado).
- Longitud máxima recomendada: 200 caracteres.
- **Toda clave tiene TTL**, salvo las excepciones listadas en el catálogo.
- No se usa `SELECT`: todo vive en la base 0 (requisito de Cluster).
- No se agrega prefijo de entorno: cada entorno (local, dev, staging, prod) tiene **sus propias instancias**.

**Gramática por tipo:**

| Uso | Formato | Ejemplo | TTL |
|---|---|---|---|
| Lock | `lock:{<recurso>:<id>}` | `lock:{pedido:123}` | Obligatorio (ver Fase 2) |
| Contador de fencing | `fence:{<recurso>:<id>}` | `fence:{pedido:123}` | **Sin TTL** (debe ser monótono). Excepción documentada |
| Rate limit | `rl:v<N>:{<tipo-sujeto>:<id>}:<politica>` | `rl:v1:{apikey:ab12}:api-default` | Calculado por el script |
| Caché | `cache:<servicio>:v<N>:<cache-name>::<clave>` | `cache:pedidos:v1:productos::42` | Obligatorio, con jitter |

- El número de versión `v<N>` se incrementa cuando cambia el **formato** del valor almacenado. Las claves viejas mueren solas por TTL.
- El formato `<cache-name>::<clave>` coincide con el que genera Spring Cache al configurar `prefixCacheNameWith("cache:<servicio>:v1:")`.

**Catálogo de claves:** toda clave nueva se registra en `docs/redis/catalogo-claves.md` (plantilla en Anexo B) **antes** de usarse en código.

### 3.2 Convención de usuarios (ACL)

| Usuario | Para qué | Patrón de claves | Permisos |
|---|---|---|---|
| `default` | — | — | **Desactivado** (`off`) |
| `admin` | Operación humana de emergencia | `~*` | `+@all`. Solo desde red de administración/bastión. Uso auditado |
| `svc-<servicio>-coord` | Un usuario por servicio que usa locks/rate limit | `~lock:*` `~fence:*` `~rl:*` | Lectura/escritura, scripting, conexión; `-@dangerous` |
| `svc-<servicio>-cache` | Un usuario por servicio que usa caché | `~cache:<servicio>:*` | Lectura/escritura, `SCAN`, conexión; `-@dangerous` |
| `monitor` | `redis_exporter` / monitoreo | `~*` | Solo lectura e `INFO`, `SLOWLOG`, `LATENCY`, `CLIENT LIST`, `CONFIG GET` |
| `healthcheck` | Healthcheck del contenedor | — | Solo `PING` |

**Reglas:**

- **Un usuario por servicio y por instancia**, nunca un usuario compartido. Permite aislar permisos, auditar con `CLIENT LIST` y revocar un servicio sin afectar a otros.
- Las contraseñas son distintas **por usuario y por entorno**, de al menos 32 caracteres aleatorios.
- En el archivo ACL se guardan **hashes SHA-256** (`#<hash>`), nunca la contraseña en claro.
- El archivo ACL real **no se versiona**; se versiona una **plantilla** (`users.acl.tpl`) y se genera en el despliegue (Anexo D).
- Cada servicio conecta con `client-name = <nombre-del-servicio>` para identificarlo en `CLIENT LIST`.
- Durante las pruebas de cada fase, revisar `ACL LOG` para detectar comandos denegados y ajustar permisos al mínimo necesario.
- **Rotación:** las ACL permiten dos contraseñas activas a la vez para el mismo usuario. Procedimiento: agregar la nueva → desplegar servicios con la nueva → quitar la antigua (ver runbook en Fase 7).

### 3.3 Convención de configuración

**Propiedades de la aplicación** (dos conexiones independientes, una por instancia):

```yaml
app:
  redis:
    coord:
      mode: ${REDIS_COORD_MODE:standalone}        # standalone | sentinel | cluster
      host: ${REDIS_COORD_HOST}
      port: ${REDIS_COORD_PORT:6379}
      nodes: ${REDIS_COORD_NODES:}                 # host:port,host:port (sentinel/cluster)
      sentinel-master: ${REDIS_COORD_SENTINEL_MASTER:}
      username: ${REDIS_COORD_USER}
      password: ${REDIS_COORD_PASSWORD}
      ssl: ${REDIS_COORD_SSL:false}
      timeout: ${REDIS_COORD_TIMEOUT:2s}
      connect-timeout: ${REDIS_COORD_CONNECT_TIMEOUT:2s}
    cache:
      mode: ${REDIS_CACHE_MODE:standalone}
      host: ${REDIS_CACHE_HOST}
      port: ${REDIS_CACHE_PORT:6379}
      nodes: ${REDIS_CACHE_NODES:}
      sentinel-master: ${REDIS_CACHE_SENTINEL_MASTER:}
      username: ${REDIS_CACHE_USER}
      password: ${REDIS_CACHE_PASSWORD}
      ssl: ${REDIS_CACHE_SSL:false}
      timeout: ${REDIS_CACHE_TIMEOUT:1s}
      connect-timeout: ${REDIS_CACHE_CONNECT_TIMEOUT:2s}
```

**Reglas:**

- Las variables de entorno siguen el patrón `REDIS_<INSTANCIA>_<PROPIEDAD>`.
- El campo `mode` existe **desde la Fase 1** aunque solo se implemente `standalone`; las demás opciones se implementan en la Fase 8 sin cambiar el contrato de configuración.
- Spring Boot autoconfigura una sola conexión (`spring.data.redis.*`). Como hay dos instancias, se crean **dos `LettuceConnectionFactory`** propios (uno marcado `@Primary`, el de coord) a partir de `app.redis.*`. Al definirlos, la autoconfiguración de Spring se desactiva sola y Actuator crea un health check por cada fábrica.
- Los archivos `redis.conf` y la plantilla ACL viven en el repositorio (`infra/redis/`), con revisión de código como cualquier cambio.

### 3.4 Convención de scripts Lua

- Viven en `redis-scripts/` en la raíz del repositorio: **un solo origen** para todos los lenguajes.
- Nombre: `<dominio>_<accion>.lua` (ej. `lock_release.lua`).
- Cada script inicia con un encabezado obligatorio:
  ```lua
  -- name: lock_release
  -- version: 1
  -- keys: KEYS[1] = lock:{recurso:id}
  -- args: ARGV[1] = token del dueño
  -- returns: 1 si liberó, 0 si el lock no era del token
  -- cluster-safe: sí (una sola clave)
  ```
- Todas las claves que toca un script se pasan en `KEYS` (nunca se construyen dentro del script) y comparten hash tag.
- Un cambio incompatible de comportamiento crea un script nuevo (`version` + 1) o un nuevo prefijo de clave; nunca se modifica en caliente uno en uso por otros lenguajes.
- Se ejecutan con `EVALSHA` (cargados con `SCRIPT LOAD`, con reintento en `NOSCRIPT`). En Spring: `DefaultRedisScript`, que ya maneja esto.
- Cada script tiene **pruebas de contrato** (Fase 5/9) que se ejecutan contra un Redis real.

### 3.5 Convención de métricas y logs

| Métrica | Tipo | Tags |
|---|---|---|
| `redis.lock.acquire` | Timer | `resource`, `result` = `acquired` / `busy` / `error` |
| `redis.lock.held` | Timer | `resource` |
| `redis.lock.renew` | Counter | `resource`, `result` = `ok` / `lost` / `error` |
| `ratelimit.decision` | Counter | `policy`, `result` = `allowed` / `rejected` / `error_open` / `error_closed` |
| Caché | Métricas de Spring Cache (`cache.gets`, `cache.puts`...) | `cache`, `result` = `hit` / `miss` |

- El tag `resource` es el **tipo** de recurso (`pedido`), nunca el ID (evita cardinalidad infinita).
- Logs: nivel `WARN` cuando se pierde un lock o Redis falla; nunca registrar tokens, contraseñas ni valores cacheados.

### 3.6 Convención de interfaces en el código

El código de negocio **nunca** usa `RedisTemplate`, Lettuce o Redisson directamente. Usa estas abstracciones (paquete sugerido `<base>.infra.redis`):

```java
public interface DistributedLock {
    /** Intenta adquirir el lock esperando como máximo `wait`. Vacío si está ocupado. */
    Optional<LockHandle> tryAcquire(String resourceType, String resourceId, Duration ttl, Duration wait);
}

public interface LockHandle extends AutoCloseable {
    long fencingToken();          // número creciente; usar en la escritura protegida
    boolean renew(Duration ttl);  // false si el lock ya no es nuestro
    boolean isValid();
    @Override void close();       // libera de forma segura (compare-and-delete)
}

public interface RateLimiter {
    RateLimitDecision tryConsume(String policy, String subjectType, String subjectId, int tokens);
}

public record RateLimitDecision(boolean allowed, long remaining, Duration retryAfter) {}
```

Así se puede cambiar de librería (Redisson ↔ protocolo propio, Redis ↔ Valkey ↔ servicio gestionado) sin tocar la lógica de negocio.

---

## 4. Estructura del repositorio

> **Nota de este repo:** la guía usa `docs/redis/` en los ejemplos; en el monorepo de ServiYa equivale a
> **`documents/redis/`** (convención del proyecto). La estructura real es la de abajo.

```
infra/redis/
  docker-compose.redis.yml       # Fase 1
  conf/
    coord.conf                   # Fase 1
    cache.conf                   # Fase 6
  acl/
    users.acl.tpl                # plantilla versionada (Fase 1)
    render-acl.sh                # genera users.acl a partir de secretos (no versionar el resultado)
  .env.example                   # nombres de variables, sin valores reales  ✅ Fase 0
  README.md                      # ✅ Fase 0
redis-scripts/                   # spec/README; los .lua canónicos viven en el classpath del backend (ver abajo)
  README.md                      # ✅ Fase 0 (actualizado en Fase 2: ubicación real de los scripts)
backend/serviya/src/main/resources/redis-scripts/   # ubicación REAL (contexto de build Docker + classpath)
  lock_acquire.lua               # ✅ Fase 2 (sin fencing)
  lock_acquire_fenced.lua        # ✅ Fase 2 (con fencing, para B8)
  lock_release.lua               # ✅ Fase 2
  lock_renew.lua                 # ✅ Fase 2
  ratelimit_token_bucket.lua     # Fase 3
backend/serviya/src/main/java/.../shared/lock/       # ✅ Fase 2: DistributedLock, LockHandle, RedisDistributedLock
documents/redis/
  guia-redis-implementacion.md   # este documento
  catalogo-claves.md             # ✅ Fase 0
  servicios-y-usuarios.md        # ✅ Fase 0 (servicios + usuarios ACL)
  protocolo-lock.md              # ✅ Fase 2
  protocolo-rate-limit.md        # Fase 3
  decisiones/                    # ✅ Fase 0 (ADR-001..004)
    ADR-001-instancias-separadas.md
    ADR-002-motor-y-version.md
    ADR-003-mecanismo-de-lock.md
    ADR-004-gestion-de-secretos.md
  runbooks/                      # Fase 7 (README.md ya creado)
    redis-caido.md
    memoria-llena.md
    rotar-credenciales.md
    restaurar-backup.md
    actualizar-version.md
```

Agregar a `.gitignore`: `infra/redis/acl/users.acl`, `infra/redis/.env`, `*.rdb`, `*.aof`.

---

## 5. Fases de implementación

Resumen de madurez:

| Fase | Nombre | Resultado |
|---|---|---|
| 0 | Fundamentos y decisiones | Decisiones tomadas, estructura y documentos base |
| 1 | Entorno local seguro mínimo | Redis en Docker con ACL, app Spring conectada a dos instancias |
| 2 | Lock distribuido | Lock seguro con token, TTL, renovación y fencing |
| 3 | Rate limiting | Token bucket atómico, políticas configurables, 429 |
| 4 | Resiliencia de la aplicación | Timeouts, circuit breaker, modos de fallo probados |
| 5 | Pruebas y observabilidad | Testcontainers, pruebas de concurrencia, métricas y dashboards |
| 6 | Caché | Spring Cache sobre `redis-cache` con JSON, TTL y mitigaciones |
| 7 | Endurecimiento para producción | TLS, secretos, backups, tuning de host, runbooks |
| 8 | Alta disponibilidad y escalado | Sentinel, Cluster o servicio gestionado |
| 9 | Interoperabilidad multilenguaje | Clientes en otros lenguajes con pruebas de contrato |

> Las fases 2 y 3 son independientes entre sí; el orden puede invertirse.

---

### Fase 0 — Fundamentos y decisiones

**Objetivo:** que el equipo y la IA tengan el mismo marco antes de escribir código.

**Tareas:**
1. Crear la estructura de carpetas de la sección 4.
2. Revisar la tabla de decisiones (sección 2) y cerrar al menos D-02, D-04 y D-14. Crear un ADR por cada decisión cerrada.
3. Crear `catalogo-claves.md` (Anexo B) vacío con las reglas de la sección 3.1.
4. Crear `.env.example` con todos los nombres de variables de la sección 3.3 y de los usuarios ACL.
5. Listar los servicios iniciales que usarán Redis para definir sus usuarios ACL.

**Criterios de aceptación:**
- [ ] Estructura de carpetas creada.
- [ ] D-02, D-04 y D-14 en estado `DECIDIDO` con su ADR.
- [ ] Catálogo de claves y `.env.example` creados.
- [ ] `.gitignore` actualizado.

**Documentar:** ADRs, catálogo de claves (vacío), lista de servicios y sus usuarios.

---

### Fase 1 — Entorno local seguro mínimo

**Objetivo:** las dos instancias corriendo en Docker, con ACL desde el primer día, y la aplicación Spring Boot conectándose a ambas.

> La seguridad básica (ACL, red interna, sin puertos expuestos) se implementa desde aquí porque cuesta poco y evita que la aplicación se construya con supuestos inseguros (por ejemplo, usar el usuario `default` o comandos peligrosos).

**Tareas:**

1. **Configuración de Redis** (ver Anexo C): `coord.conf` y `cache.conf`.
2. **Plantilla ACL** y script de generación (Anexo D).
3. **Docker Compose** (Anexo E):
   - Red `backend` interna; **sin `ports:`** en los servicios Redis.
   - Volumen persistente solo para `redis-coord`.
   - Healthcheck con el usuario `healthcheck`.
   - Límite de memoria del contenedor; `maxmemory` ≈ 60–75 % de ese límite.
   - Imagen con versión fija (D-02).
   - `read_only: true` y `no-new-privileges`.
4. **Aplicación Spring Boot:**
   - Propiedades `app.redis.coord.*` y `app.redis.cache.*` (sección 3.3).
   - Clase `@ConfigurationProperties` + configuración que crea dos `LettuceConnectionFactory` (coord `@Primary`) solo con `mode=standalone`; si `mode` es otro valor, fallar al arrancar con un mensaje claro ("modo no implementado aún").
   - Dos `StringRedisTemplate` (o `RedisTemplate` con serializadores de texto/JSON), nombrados `coordRedisTemplate` y `cacheRedisTemplate`.
   - `client-name` = `spring.application.name`.
   - Actuator con health de ambas instancias.
5. Para acceso de desarrollo local con `redis-cli`, usar `docker compose exec redis-coord redis-cli --user admin`, sin publicar puertos.

**Preparar ahora para el futuro:**
- Campo `mode` en la configuración (standalone/sentinel/cluster).
- Bandera `ssl` en la configuración, aunque esté en `false`.
- Usuarios por servicio desde el inicio.

**Criterios de aceptación:**
- [ ] `docker compose up` levanta ambas instancias en estado `healthy`.
- [ ] Conectar con el usuario `default` falla.
- [ ] El usuario del servicio no puede ejecutar `FLUSHALL`, `KEYS`, `CONFIG SET` (verificado y visible en `ACL LOG`).
- [ ] El usuario de coord no puede escribir claves `cache:*` y viceversa.
- [ ] Redis no es accesible desde el host ni desde fuera de la red `backend`.
- [ ] `/actuator/health` muestra ambas conexiones `UP`.
- [ ] Ninguna contraseña real en el repositorio.

**Documentar:** ADR de versión/motor, `.env.example` completo, README de cómo levantar el entorno local.

---

### Fase 2 — Lock distribuido

**Objetivo:** implementar `DistributedLock` según el protocolo común y usarlo en un primer caso real.

**Depende de:** D-04 (mecanismo), D-05 (fencing), D-08 (fallo).

#### Protocolo de lock (documentar en `protocolo-lock.md`)

1. **Adquirir:** generar un token único (UUID v4) por adquisición. Ejecutar `lock_acquire.lua` con `KEYS = [lock:{r:id}, fence:{r:id}]`. Si devuelve un número > 0, ese es el **fencing token**; si devuelve 0, el lock está ocupado.
2. **Esperar:** si está ocupado y hay tiempo de espera, reintentar con backoff exponencial con jitter (ej. 50 ms → 100 → 200, máx. 500 ms) hasta agotar `wait`.
3. **Renovar:** si la sección crítica puede superar el TTL, renovar cada `TTL/3` con `lock_renew.lua`. Si la renovación devuelve 0, el lock **se perdió**: abortar la operación tan pronto sea posible y registrar `WARN`.
4. **Liberar:** siempre en `finally`/`close()` con `lock_release.lua` (compare-and-delete).
5. **Usar el fencing token:** en recursos críticos, la escritura en la base de datos incluye el token y se rechaza si es menor que el último aplicado (o se usa bloqueo optimista con columna `version`).

**TTL recomendado:** al menos 2–3 veces el p99 de duración de la sección crítica, con renovación si hay variabilidad alta. Tiempo de espera (`wait`) corto: unos pocos segundos como máximo.

#### Scripts

```lua
-- name: lock_acquire
-- version: 1
-- keys: KEYS[1] = lock:{recurso:id}, KEYS[2] = fence:{recurso:id}
-- args: ARGV[1] = token, ARGV[2] = ttl en milisegundos
-- returns: fencing token (>0) si adquirió, 0 si está ocupado
-- cluster-safe: sí (ambas claves comparten hash tag)
if redis.call("SET", KEYS[1], ARGV[1], "NX", "PX", ARGV[2]) then
  return redis.call("INCR", KEYS[2])
end
return 0
```

```lua
-- name: lock_release
-- version: 1
-- keys: KEYS[1] = lock:{recurso:id}
-- args: ARGV[1] = token
-- returns: 1 si liberó, 0 si el lock no pertenecía al token
-- cluster-safe: sí
if redis.call("GET", KEYS[1]) == ARGV[1] then
  return redis.call("DEL", KEYS[1])
end
return 0
```

```lua
-- name: lock_renew
-- version: 1
-- keys: KEYS[1] = lock:{recurso:id}
-- args: ARGV[1] = token, ARGV[2] = nuevo ttl en milisegundos
-- returns: 1 si renovó, 0 si el lock ya no pertenece al token
-- cluster-safe: sí
if redis.call("GET", KEYS[1]) == ARGV[1] then
  return redis.call("PEXPIRE", KEYS[1], ARGV[2])
end
return 0
```

#### Implementación según D-04

- **Opción B (protocolo propio, recomendada si hay multilenguaje):** implementar `DistributedLock` con `coordRedisTemplate` + `DefaultRedisScript` cargando los `.lua` de `redis-scripts/` (copiarlos al classpath en el build). Renovación con un `ScheduledExecutorService` compartido.
- **Opción A (Redisson, solo Java):** implementar `DistributedLock` envolviendo `RLock` (`tryLock(wait, unit)` sin `leaseTime` para activar el watchdog). El fencing token se obtiene aparte con `INCR fence:{...}` al adquirir. Si se usa Redisson, el usuario ACL necesita además permiso sobre los canales pub/sub de Redisson (`&redisson_lock__channel:*`). **Advertencia:** los locks de Redisson no son interoperables con otros lenguajes.

#### Uso para tareas programadas

Para que un `@Scheduled` corra en un solo nodo, evaluar **ShedLock** con su proveedor Redis, o envolver el job con `DistributedLock`. Registrar la elección en un ADR.

**Preparar ahora para el futuro:**
- Claves con hash tag (cluster-safe).
- Fencing token disponible en la API aunque el primer caso de uso no lo necesite.
- En las tablas que se protejan con lock, agregar columna `version` (bloqueo optimista) o `last_fencing_token`.

**Criterios de aceptación:**
- [ ] Prueba de concurrencia: N hilos/instancias compitiendo por el mismo recurso → solo uno ejecuta la sección crítica a la vez.
- [ ] Un proceso no puede liberar un lock ajeno.
- [ ] Si el proceso muere, el lock expira solo tras el TTL.
- [ ] La renovación mantiene el lock en operaciones largas y detecta pérdida.
- [ ] Los fencing tokens son estrictamente crecientes por recurso.
- [ ] Si Redis no responde, la operación no se ejecuta y se registra el error (D-08).
- [ ] Claves registradas en el catálogo; `protocolo-lock.md` escrito.

---

### Fase 3 — Rate limiting

**Objetivo:** implementar `RateLimiter` con token bucket atómico y políticas configurables.

**Depende de:** D-06 (algoritmo), D-07 (modo de fallo), D-09 (sujeto).

#### Políticas configurables

```yaml
app:
  ratelimit:
    policies:
      api-default:
        capacity: 100            # máximo de ráfaga
        refill-per-second: 10    # tasa sostenida
        failure-mode: OPEN       # OPEN | CLOSED
      login:
        capacity: 5
        refill-per-second: 0.1   # 1 intento cada 10 s sostenido
        failure-mode: CLOSED
```

#### Script

```lua
-- name: ratelimit_token_bucket
-- version: 1
-- keys: KEYS[1] = rl:v1:{tipo-sujeto:id}:politica
-- args: ARGV[1] = capacidad, ARGV[2] = tokens repuestos por segundo, ARGV[3] = tokens solicitados
-- returns: { permitido (1/0), tokens restantes (entero), retry_after en ms }
-- cluster-safe: sí (una sola clave)
-- nota: usa el reloj de Redis (TIME), no el de los clientes
local capacity  = tonumber(ARGV[1])
local rate      = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])

local t   = redis.call("TIME")
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local data   = redis.call("HMGET", KEYS[1], "tokens", "ts")
local tokens = tonumber(data[1])
local ts     = tonumber(data[2])
if tokens == nil or ts == nil then
  tokens = capacity
  ts = now
end

local elapsed = math.max(0, now - ts)
tokens = math.min(capacity, tokens + (elapsed * rate / 1000))

local allowed = 0
local retry_after = 0
if tokens >= requested then
  tokens = tokens - requested
  allowed = 1
else
  retry_after = math.ceil((requested - tokens) * 1000 / rate)
end

redis.call("HSET", KEYS[1], "tokens", tostring(tokens), "ts", now)
-- TTL: tiempo de rellenar el bucket completo x2; luego la clave se borra sola
redis.call("PEXPIRE", KEYS[1], math.ceil(capacity * 1000 / rate) * 2)

return { allowed, math.floor(tokens), retry_after }
```

#### Integración HTTP

- Filtro (`OncePerRequestFilter` o `HandlerInterceptor`) que resuelve la política del endpoint y el sujeto (D-09).
- Respuesta rechazada: `429 Too Many Requests` + `Retry-After` (segundos) + opcional `X-RateLimit-Remaining`.
- Si se identifica por IP detrás de un proxy: configurar `server.forward-headers-strategy` y confiar en `X-Forwarded-For` solo desde proxies conocidos.
- Si en el futuro se usa Spring Cloud Gateway, evaluar mover el rate limit al gateway reutilizando el mismo script/convención.

**Preparar ahora para el futuro:**
- Versión en la clave (`rl:v1:`) para poder cambiar el algoritmo sin colisiones.
- Políticas en configuración, no en código, para ajustarlas sin redeploy si se externaliza la configuración.

**Criterios de aceptación:**
- [ ] Con `capacity=5`, la sexta petición inmediata se rechaza con 429 y `Retry-After` correcto.
- [ ] Los tokens se reponen según la tasa configurada.
- [ ] Prueba concurrente: con N peticiones simultáneas nunca se permiten más que la capacidad.
- [ ] Con Redis caído: políticas `OPEN` dejan pasar y `CLOSED` rechazan (con métrica `error_open`/`error_closed`).
- [ ] La clave expira sola tras inactividad.
- [ ] Claves en el catálogo; `protocolo-rate-limit.md` escrito.

---

### Fase 4 — Resiliencia de la aplicación

**Objetivo:** que un problema en Redis degrade el servicio de forma controlada en vez de tumbarlo.

**Tareas:**
1. Timeouts explícitos en ambos clientes (comando y conexión), según sección 3.3.
2. Circuit breaker (Resilience4j) alrededor de las implementaciones de `DistributedLock` y `RateLimiter`; al abrirse, aplicar el modo de fallo definido (D-07, D-08) sin esperar timeouts.
3. Reconexión automática de Lettuce verificada (reiniciar el contenedor Redis con la app corriendo).
4. Arranque de la aplicación: decidir y documentar si la app puede arrancar con Redis caído (recomendado: sí, con health `DOWN`/degradado, para no generar caídas en cascada).

**Criterios de aceptación:**
- [ ] Con Redis detenido, los endpoints responden en < timeout + margen y según el modo de fallo.
- [ ] Al volver Redis, la app se recupera sin reiniciarse.
- [ ] Comportamiento de arranque documentado.

---

### Fase 5 — Pruebas y observabilidad

**Objetivo:** confianza automatizada y visibilidad operativa.

**Pruebas:**
- Testcontainers con **la misma imagen y versión** que producción, cargando los mismos `coord.conf`/ACL (o equivalentes de prueba).
- Pruebas de contrato de cada script Lua (entradas → salidas esperadas).
- Pruebas de concurrencia de locks y rate limit (varios hilos y, si es posible, varias instancias de la app).
- Pruebas de fallo (contenedor detenido, latencia inyectada con Toxiproxy opcional).

**Observabilidad:**
- Métricas de la aplicación según la sección 3.5.
- `redis_exporter` (usuario `monitor`) + Prometheus + Grafana (D-18).
- Alertas mínimas:

| Alerta | Condición sugerida |
|---|---|
| Memoria alta | `used_memory / maxmemory` > 80 % |
| Expulsiones en coord | `evicted_keys` > 0 en `redis-coord` (debe ser siempre 0) |
| Escrituras rechazadas | errores OOM en coord |
| Latencia | p99 de comandos > umbral definido |
| Conexiones | `rejected_connections` > 0 o salto brusco en `connected_clients` |
| Instancia caída | health `DOWN` / exporter sin datos |
| Locks perdidos | `redis.lock.renew{result=lost}` > 0 |

**Criterios de aceptación:**
- [ ] Pruebas corren en CI.
- [ ] Dashboard con memoria, ops/s, latencia, clientes, expulsiones, decisiones de rate limit y locks.
- [ ] Alertas configuradas y probadas al menos una vez.

---

### Fase 6 — Caché

**Objetivo:** caché con Spring Cache sobre `redis-cache`, segura ante cambios de formato y fallos.

**Depende de:** D-10, D-12, D-13.

**Tareas:**
1. Activar `redis-cache` (si no estaba ya) con `allkeys-lfu` y sin persistencia.
2. `RedisCacheManager` sobre la fábrica de cache:
   - `prefixCacheNameWith("cache:<servicio>:v1:")`.
   - TTL por defecto + TTL por caché; **jitter** de ±10 % (implementar con un `TtlFunction` / `RedisCacheWriter.TtlFunction` según la versión de Spring Data Redis).
   - Serializador **JSON tipado por caché** (Jackson); no usar `JdkSerializationRedisSerializer` ni metadatos `@class` en cachés que lean otros lenguajes. Verificar nombres de clases según la versión (Spring Boot 4 / Jackson 3 cambia los serializadores).
   - Jackson con `FAIL_ON_UNKNOWN_PROPERTIES = false`.
   - Estrategia de borrado masivo con `SCAN` (`BatchStrategies.scan(...)`), porque la de por defecto usa `KEYS`.
   - Estadísticas habilitadas (`enableStatistics()`) para métricas.
3. Cachear **DTOs**, no entidades JPA.
4. Manejo de errores: implementar `CacheErrorHandler` que registre y continúe (si Redis falla, se consulta la fuente original).
5. Mitigaciones:
   - **Stampede:** para claves muy calientes, reconstrucción protegida con `DistributedLock` (`lock:{cache-rebuild:<cache>:<clave>}`); `@Cacheable(sync=true)` solo protege dentro de una JVM.
   - **Penetración:** cachear "no encontrado" con TTL corto donde aplique.
   - **Avalancha:** jitter de TTL.
6. Invalidación: `@CacheEvict`/`@CachePut` en escrituras. Si varios servicios modifican el mismo dato, documentar la estrategia de invalidación por eventos.

**Preparar ahora para el futuro:**
- Versión en el prefijo (`v1`) para cambios de formato sin migraciones.
- Usuarios ACL de cache por servicio (`~cache:<servicio>:*`).

**Criterios de aceptación:**
- [ ] Segunda lectura sirve desde caché (métrica de hit).
- [ ] Al detener `redis-cache`, la aplicación sigue funcionando (más lenta).
- [ ] Los valores en Redis son JSON legible.
- [ ] Al cambiar `v1` → `v2`, no hay errores de deserialización.
- [ ] Ninguna clave de caché sin TTL.
- [ ] Cachés registradas en el catálogo con su TTL e invalidación.

---

### Fase 7 — Endurecimiento para producción

**Objetivo:** cumplir los requisitos de calidad para un primer despliegue real en servidor propio o VM en la nube.

#### Seguridad
- [ ] **TLS** (D-15) entre hosts: `tls-port 6379`, `port 0`, certificados montados como secretos; clientes con `ssl=true` y validación de CA.
- [ ] Secretos desde el gestor elegido (D-14); `users.acl` generado en el despliegue.
- [ ] Red privada / security groups: solo los servicios autorizados alcanzan Redis; acceso `admin` solo desde bastión.
- [ ] Imagen con versión fija y **parcheada**; procedimiento de actualización periódica (incluye revisar avisos de seguridad, especialmente del motor Lua).
- [ ] Escaneo de vulnerabilidades de la imagen en CI.
- [ ] Rotación de credenciales probada (runbook).
- [ ] **Actuator (endpoints de management) fuera del puerto público**: `management.server.port` dedicado y **no** publicado en el compose (solo alcanzable dentro de la red del backend), o protegido tras Spring Security. En Fase 1 el grupo `redis` ya quedó en `show-details: when_authorized` para no filtrar detalles por el 8080.

#### Confiabilidad
- [ ] `redis-coord`: AOF `everysec` + RDB; volumen en disco persistente.
- [ ] **Backups** del RDB a almacenamiento externo (otro servidor / bucket), con retención definida.
- [ ] **Restauración probada** al menos una vez.
- [ ] Ajustes del **host** (no del contenedor): `vm.overcommit_memory=1`, Transparent Huge Pages desactivado, `net.core.somaxconn` adecuado.
- [ ] Margen de memoria: `maxmemory` ≤ 75 % del límite del contenedor.

#### Operación
- [ ] Runbooks escritos y revisados: Redis caído, memoria llena, rotar credenciales, restaurar backup, actualizar versión.
- [ ] Prueba de carga con el tráfico esperado ×2 para dimensionar memoria y CPU.

**Criterios de aceptación:** todos los checks anteriores marcados y revisados por una segunda persona.

---

### Fase 8 — Alta disponibilidad y escalado

**Objetivo:** cumplir el SLA definido en D-16 y preparar el crecimiento.

**Ruta según necesidad:**

| Etapa | Cuándo | Qué implica |
|---|---|---|
| Nodo único + backups | SLA moderado, tolerancia a minutos de caída | Ya cubierto en Fase 7 |
| Réplica + Sentinel (3 Sentinels) | Se necesita failover automático | Implementar `mode=sentinel` en la configuración; Lettuce/Redisson lo soportan |
| Redis Cluster | Los datos o la carga superan un nodo | Implementar `mode=cluster`; las claves ya son cluster-safe |
| Servicio gestionado (D-17) | Se prefiere no operar Redis | Cambiar variables de entorno; validar restricciones del proveedor |

**Consideraciones obligatorias:**
- La replicación es **asíncrona**: en un failover pueden perderse escrituras recientes, incluidos locks. Las garantías de corrección siguen en la base de datos (D-05).
- Servicios gestionados suelen: bloquear `CONFIG SET`, exigir TLS, limitar ACL o módulos y, en algunos casos, operar solo en modo Cluster. Validar que el proyecto solo usa comandos estándar.
- Mantener Redis en la misma región/red que las aplicaciones.
- Revisar periódicamente big keys (`redis-cli --bigkeys` o `MEMORY USAGE`) antes de escalar.

**Criterios de aceptación:**
- [ ] Prueba de failover: se detiene el primario y la aplicación se recupera dentro del objetivo.
- [ ] Las pruebas de contrato pasan contra la topología nueva.
- [ ] ADR de la topología elegida.

---

### Fase 9 — Interoperabilidad multilenguaje

**Objetivo:** que servicios en otros lenguajes usen locks y rate limit de forma compatible.

**Tareas:**
1. Implementar en el nuevo lenguaje las mismas interfaces conceptuales (sección 3.6) usando su cliente oficial/recomendado.
2. Cargar los scripts desde `redis-scripts/` (mismo archivo, misma versión).
3. Respetar la convención de claves y crear el usuario ACL del servicio.
4. **Pruebas de contrato cruzadas:** un lock adquirido desde Java bloquea al servicio en el otro lenguaje y viceversa; ambos consumen del mismo bucket de rate limit.

**Criterios de aceptación:**
- [ ] Pruebas cruzadas Java ↔ otro lenguaje pasando en CI.
- [ ] Ningún uso de formatos propietarios de librería (ej. locks de Redisson) en recursos compartidos.

---

## 6. Resumen de "hacer ahora para escalar después"

| Qué se hace desde el inicio | Por qué |
|---|---|
| Dos instancias separadas (coord / cache) | Distintas políticas de memoria y persistencia; escalan por separado |
| Hash tags `{...}` en las claves | Migrar a Cluster sin renombrar claves ni reescribir scripts |
| Sin `SELECT`, sin `KEYS` | Requisitos de Cluster y de servicios gestionados |
| Versión en prefijos (`v1`) y en scripts | Cambiar formatos y algoritmos sin migraciones ni colisiones |
| Campo `mode` y bandera `ssl` en configuración | Pasar a Sentinel/Cluster/TLS/nube cambiando variables, no código |
| Interfaces propias (`DistributedLock`, `RateLimiter`) | Cambiar librería o proveedor sin tocar negocio |
| Scripts Lua compartidos | Otros lenguajes se integran sin reimplementar algoritmos |
| Usuario ACL por servicio | Seguridad y auditoría crecen con el número de servicios |
| Fencing token / bloqueo optimista en BD | Corrección incluso con failovers o pausas de GC |
| Configuración y ACL como código | Reproducible en cualquier entorno |
| Métricas y catálogo de claves | Entender el uso real antes de escalar |

---

## Anexo A — Plantilla de ADR

```markdown
# ADR-NNN: <título corto>

- **Estado:** Propuesto | Aceptado | Reemplazado por ADR-XXX
- **Fecha:** AAAA-MM-DD
- **Decisión relacionada:** D-XX

## Contexto
<problema y restricciones>

## Opciones consideradas
1. <opción A> — pros / contras
2. <opción B> — pros / contras

## Decisión
<qué se decidió>

## Consecuencias
<qué implica, qué se gana, qué se pierde, qué hay que revisar a futuro>
```

## Anexo B — Plantilla del catálogo de claves

```markdown
| Patrón de clave | Instancia | Tipo Redis | TTL | Dueño (servicio) | Escrita por | Leída por | Propósito | Versión de formato |
|---|---|---|---|---|---|---|---|---|
| lock:{pedido:<id>} | coord | string | 30 s + renovación | pedidos | pedidos | pedidos | Evitar procesamiento doble de un pedido | — |
| fence:{pedido:<id>} | coord | string (int) | **Sin TTL (excepción)** | pedidos | pedidos | pedidos | Fencing token monótono | — |
| rl:v1:{apikey:<hash>}:api-default | coord | hash | Calculado | gateway | todos | todos | Rate limit general por API key | v1 |
| cache:pedidos:v1:productos::<id> | cache | string (JSON) | 1 h ± 10 % | pedidos | pedidos | pedidos | Detalle de producto | v1 |
```

## Anexo C — Configuración de Redis

```conf
# infra/redis/conf/coord.conf — locks y rate limiting
bind 0.0.0.0                  # aceptable solo porque la red Docker es interna y no hay puertos publicados
protected-mode yes
port 6379
aclfile /usr/local/etc/redis/users.acl

maxmemory 700mb               # ≈ 70 % del límite del contenedor (1g)
maxmemory-policy noeviction   # los locks nunca se expulsan

appendonly yes
appendfsync everysec
save 3600 1 300 100 60 10000
dir /data

timeout 300
tcp-keepalive 60
loglevel notice

# Producción con TLS (Fase 7):
# port 0
# tls-port 6379
# tls-cert-file /run/secrets/redis.crt
# tls-key-file  /run/secrets/redis.key
# tls-ca-cert-file /run/secrets/ca.crt
# tls-auth-clients optional
```

```conf
# infra/redis/conf/cache.conf — caché
bind 0.0.0.0
protected-mode yes
port 6379
aclfile /usr/local/etc/redis/users.acl

maxmemory 1400mb              # ≈ 70 % del límite del contenedor (2g)
maxmemory-policy allkeys-lfu

save ""
appendonly no

timeout 300
tcp-keepalive 60
loglevel notice
```

## Anexo D — Plantilla ACL y generación

```conf
# infra/redis/acl/users.acl.tpl  (se versiona; el users.acl generado NO)
user default off
user admin on #${ADMIN_HASH} ~* &* +@all
user healthcheck on #${HEALTHCHECK_HASH} +ping
user monitor on #${MONITOR_HASH} ~* +@connection +ping +info +client|list +slowlog +latency +config|get +memory|usage +scan +type +ttl +pttl +strlen +llen +scard +zcard +hlen +xlen

# Un bloque por servicio (coord). Ajustar al mínimo revisando ACL LOG en pruebas.
user svc-pedidos-coord on #${SVC_PEDIDOS_COORD_HASH} ~lock:* ~fence:* ~rl:* +@connection +ping +get +set +del +incr +pexpire +pttl +hmget +hset +eval +evalsha +script|load +script|exists

# Un bloque por servicio (cache). Va en el ACL de la instancia redis-cache.
user svc-pedidos-cache on #${SVC_PEDIDOS_CACHE_HASH} ~cache:pedidos:* +@connection +ping +get +set +del +unlink +exists +pexpire +pttl +mget +scan
```

> Recomendación: generar **un archivo ACL por instancia** (coord y cache), con solo los usuarios que necesita cada una.

```sh
#!/usr/bin/env sh
# infra/redis/acl/render-acl.sh
# Uso: exportar las contraseñas como variables de entorno (desde .env o el gestor de secretos)
#      y ejecutar: ./render-acl.sh users.acl.tpl users.acl
set -eu

hash() { printf '%s' "$1" | sha256sum | cut -d' ' -f1; }

export ADMIN_HASH="$(hash "$REDIS_ADMIN_PASSWORD")"
export HEALTHCHECK_HASH="$(hash "$REDIS_HEALTHCHECK_PASSWORD")"
export MONITOR_HASH="$(hash "$REDIS_MONITOR_PASSWORD")"
export SVC_PEDIDOS_COORD_HASH="$(hash "$REDIS_SVC_PEDIDOS_COORD_PASSWORD")"
export SVC_PEDIDOS_CACHE_HASH="$(hash "$REDIS_SVC_PEDIDOS_CACHE_PASSWORD")"

envsubst < "$1" > "$2"
chmod 640 "$2"
```

## Anexo E — Docker Compose base

```yaml
# infra/redis/docker-compose.redis.yml
services:
  redis-coord:
    image: redis:8.2-alpine          # D-02: fijar la versión exacta validada y parcheada
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    volumes:
      - ./conf/coord.conf:/usr/local/etc/redis/redis.conf:ro
      - ./acl/users.coord.acl:/usr/local/etc/redis/users.acl:ro
      - redis-coord-data:/data
    environment:
      REDISCLI_AUTH: ${REDIS_HEALTHCHECK_PASSWORD}
    healthcheck:
      test: ["CMD", "redis-cli", "--user", "healthcheck", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    mem_limit: 1g
    restart: unless-stopped
    read_only: true
    security_opt: ["no-new-privileges:true"]
    networks: [backend]
    # SIN "ports:" — Redis no se expone fuera de la red backend

  redis-cache:
    image: redis:8.2-alpine
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    volumes:
      - ./conf/cache.conf:/usr/local/etc/redis/redis.conf:ro
      - ./acl/users.cache.acl:/usr/local/etc/redis/users.acl:ro
    environment:
      REDISCLI_AUTH: ${REDIS_HEALTHCHECK_PASSWORD}
    healthcheck:
      test: ["CMD", "redis-cli", "--user", "healthcheck", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    mem_limit: 2g
    restart: unless-stopped
    read_only: true
    tmpfs: ["/data"]                 # sin persistencia, pero Redis necesita un dir escribible
    security_opt: ["no-new-privileges:true"]
    networks: [backend]

  app:
    image: mi-app-spring:1.0.0
    environment:
      REDIS_COORD_HOST: redis-coord
      REDIS_COORD_USER: svc-pedidos-coord
      REDIS_COORD_PASSWORD: ${REDIS_SVC_PEDIDOS_COORD_PASSWORD}
      REDIS_CACHE_HOST: redis-cache
      REDIS_CACHE_USER: svc-pedidos-cache
      REDIS_CACHE_PASSWORD: ${REDIS_SVC_PEDIDOS_CACHE_PASSWORD}
    depends_on:
      redis-coord:
        condition: service_healthy
    networks: [backend, frontend]
    ports: ["8080:8080"]

networks:
  backend:
    internal: true                   # sin salida a Internet
  frontend:

volumes:
  redis-coord-data:
```

## Anexo F — Checklist rápido de revisión de código (para la IA y el equipo)

- [ ] ¿La clave sigue la convención y está en el catálogo?
- [ ] ¿Tiene TTL (o es una excepción documentada)?
- [ ] ¿Las claves usadas juntas comparten hash tag?
- [ ] ¿Se usa la interfaz (`DistributedLock`, `RateLimiter`, Spring Cache) y no Redis directo?
- [ ] ¿Operaciones de leer-calcular-escribir son atómicas (script Lua)?
- [ ] ¿Hay timeout y comportamiento definido si Redis falla?
- [ ] ¿La serialización es JSON y tolerante a campos nuevos?
- [ ] ¿Sin `KEYS`, `SELECT`, `FLUSH*`, ni serialización nativa de Java?
- [ ] ¿Sin secretos, tokens ni datos personales en código o logs?
- [ ] ¿Métricas con tags de baja cardinalidad?
- [ ] ¿Se actualizó la documentación (catálogo, ADR, protocolo)?
