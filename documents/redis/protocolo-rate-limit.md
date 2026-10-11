# Protocolo de rate limiting distribuido — ServiYa

> Deuda **#6 / M13** (Fase 3). Reemplaza el contador en memoria (Bucket4j local) por un algoritmo
> atómico sobre `redis-coord`, de modo que la cuota se comparte entre ≥2 instancias. Detrás de la
> interfaz `RateLimiter` hay scripts Lua versionados (interoperables entre lenguajes).

## Piezas

| Pieza | Dónde | Rol |
|---|---|---|
| `RateLimiter` (interfaz) | `shared/ratelimit` | API: `tryConsume(policy, subjectType, subjectId, tokens)` → `RateLimitDecision` |
| `RedisRateLimiter` | `shared/ratelimit` | Implementación sobre `coordRedisTemplate` + scripts Lua; métricas y modo de fallo |
| `RateLimitProperties` | `shared/ratelimit` | Políticas desde `app.ratelimit.*` (no en código) |
| `ratelimit_token_bucket.lua` | `backend/.../resources/redis-scripts` | Nivel 1: recarga continua, admite ráfaga |
| `ratelimit_sliding_window.lua` | `backend/.../resources/redis-scripts` | Nivel 2: ventana deslizante exacta (ZSET), sin ráfaga de borde |
| `GlobalRateLimitFilter` | `shared/ratelimit` | Nivel 1: techo global `api-default` a todo `/api/v1/**`, tras el filtro JWT |
| `RateLimitingFilter` | `users/.../security` | Cuotas por IP de `/api/v1/auth/**`, **antes** del JWT (corta el abuso en la puerta) |
| `@RateLimit` + `RateLimitInterceptor` | `shared/ratelimit` | Nivel 2: cuota estricta por endpoint anotado |
| `RateLimitSubject` | `shared/ratelimit` | Resolución estándar del sujeto (D-09) |

## Clave

`rl:v1:{<tipo-sujeto>:<id>}:<politica>` — hash tag `{<tipo>:<id>}` (cluster-safe), versión `v1` para
poder migrar el algoritmo sin colisiones. TTL calculado por el script; la clave se autolimpia tras
inactividad.

## Algoritmos (D-06)

- **Token bucket** (`TOKEN_BUCKET`): `capacity` (ráfaga) + `refill-per-second` (tasa sostenida). Para el
  **techo global** (`api-default`): admite ráfagas, ideal para throughput general. Usa el reloj de Redis
  (`TIME`), no el del cliente.
- **Ventana deslizante** (`SLIDING_WINDOW`): `capacity` dentro de `window` (ms). ZSET con el timestamp de
  cada intento; exacta y **sin ráfaga de borde**. Para **auth y endpoints costosos**, donde interesa
  "no más de N en cualquier ventana" y cortar la fuerza bruta.

> Se descartó el token bucket para auth: su goteo sostenido es justo lo que la ventana evita.

## Sujeto (D-09)

| Caso | Sujeto | Por qué |
|---|---|---|
| Endpoint **anónimo** (no hay JWT) | `ip` | único dato disponible |
| Endpoint **autenticado** | `user` (id del JWT) | estable, no falsificable, justo por persona |
| Ataque **dirigido a una cuenta** desde muchas IPs (password-reset) | `email` del body | la IP rota; el objetivo es el correo |

- **La IP** sale de `request.getRemoteAddr()` con `server.forward-headers-strategy=native`: el contenedor
  resuelve la IP real desde `X-Forwarded-For` **solo** si el salto inmediato es un proxy de confianza. NO
  se parsea la cabecera a mano (sería falsificable con el backend expuesto directo). En prod, declarar los
  proxies con `server.tomcat.remoteip.*` si no están en un rango privado.
- **Login NO lleva cuota por correo** a propósito: habilitaría un DoS de bloqueo de la cuenta de la
  víctima. La fuerza bruta por origen la frena la cuota por IP.

## Modo de fallo por política (D-07)

Si `redis-coord` no responde al evaluar, la decisión la dicta la política:

- `OPEN`: deja pasar (prioriza disponibilidad). Para el techo global y login.
- `CLOSED`: rechaza 429 (prioriza protección). Para registro, password-reset y endpoints costosos.

Mixto por política (decisión del usuario): login `OPEN`; registro/reset `CLOSED`. Métrica
`ratelimit.decision{result=allowed|rejected|error_open|error_closed}`.

### Compensación del modo OPEN en endpoints que notifican (D-07 / protocolo 1a)

Los endpoints que notifican por correo (request-write, proposal-write, report-create) son `OPEN`. Para no
amplificar abuso durante una caída de Redis, el envío de correo se **difiere**: si `redis-coord` está
caído (`CoordinationAvailability`), `NotificationDeliveryService` NO llama a `EmailPort`, marca la entrega
`FAILED` y la reenvía el `NotificationRetryScheduler` cuando Redis vuelve (ese scheduler está lock-guarded,
así que tampoco corre durante la caída: el backlog espera y se vacía al recuperarse). Canal INTERNAL
intacto. Pendiente (deuda, opción b): análisis de abuso antes de vaciar el backlog (p. ej. tope por
destinatario).

## Políticas (ver `application.yaml` → `app.ratelimit.policies`)

| Política | Algoritmo | Cap. | Ventana / tasa | Fallo | Sujeto | Endpoints |
|---|---|---|---|---|---|---|
| `api-default` | token bucket | 200 | 5/s | OPEN | user/ip | Techo global de `/api/v1/**` |
| `login-ip` | sliding | 10 | 15 min | OPEN | ip | `POST /auth/login` |
| `register-ip` | sliding | 5 | 60 min | CLOSED | ip | `POST /auth/register` |
| `reset-ip` | sliding | 5 | 15 min | CLOSED | ip | `POST /auth/password-reset` |
| `reset-email` | sliding | 3 | 60 min | CLOSED | email | `POST /auth/password-reset` |
| `reset-validate-ip` | sliding | 20 | 15 min | CLOSED | ip | `POST /auth/password-reset/validate` |
| `reset-confirm-ip` | sliding | 10 | 15 min | CLOSED | ip | `POST /auth/password-reset/confirm` |
| `request-write` | sliding | 30 | 10 min | OPEN | user | `POST /service-requests` + transiciones |
| `proposal-write` | sliding | 20 | 15 min | OPEN | user | crear/aceptar/rechazar/cancelar propuestas |
| `report-create` | sliding | 10 | 60 min | OPEN | user | `POST /reports/**` *(política lista, sin anotar aún)* |
| `service-write` | sliding | 20 | 60 min | CLOSED | user | `POST/PATCH /services` (multipart) |
| `profile-photo` | sliding | 10 | 60 min | CLOSED | user | `PATCH /users/me/profile/photo` |
| `address-verify` | sliding | 20 | 60 min | CLOSED | user | verify + create/update de direcciones |
| `account-security` | sliding | 5 | 15 min | CLOSED | user | change-password/email *(política lista, sin anotar aún)* |
| `search` | sliding | 60 | 1 min | OPEN | ip/user | `GET /services/search` *(política lista, sin anotar aún)* |
| `stats` | sliding | 30 | 1 min | OPEN | ip/user | `GET /platform/stats` |

## Respuesta al rechazar

`429 Too Many Requests` + cabecera `Retry-After` (segundos) + cuerpo `ErrorResponse` uniforme. Mensaje
deliberadamente vago (no revela si la cuota fue por IP o por correo → no es oráculo de enumeración).

## Criterios de aceptación (Fase 3)

- [x] Con `capacity=N`, la petición N+1 inmediata se rechaza con 429 y `Retry-After` correcto.
- [x] Los tokens se reponen según la tasa configurada (token bucket).
- [x] Prueba concurrente: con N peticiones simultáneas nunca se permiten más que la capacidad.
- [x] Con Redis caído: `OPEN` deja pasar y `CLOSED` rechaza (métrica `error_open`/`error_closed`).
- [x] La clave expira sola tras inactividad.
- [x] Claves en el catálogo; este documento escrito.
- [x] Verificado e2e: login 429 al 11º; `/platform/stats` 429 al 31º; claves `rl:*` en `redis-coord`.

## Aplazado (deuda, anotado también en la guía §2.1 y ESTADO §6.6)

- Anotar `@RateLimit` en `search`, `report-create`, `account-security` (políticas ya definidas; eran
  prioridad media, fuera del alcance 🔴 elegido).
- Análisis de abuso antes de vaciar el backlog de notificaciones diferidas (opción b).
- Caché de `/platform/stats` (Fase 6): hoy recomputa en cada llamada.
- Batería de carga/observabilidad del rate limit (Fase 5).
