# Guía de implementación: centralización de autenticación y extracción del servicio de identidad

> **Propósito:** este documento es la guía de trabajo para implementar, por fases, la centralización de la autenticación en el API Gateway y la extracción del módulo `users` del monolito como servicio de identidad.
> Está pensado para ser leído por personas **y** por agentes de IA que implementen las tareas.
>
> **Cómo usarlo:**
> - Cada fase tiene: objetivo, prerrequisitos, **pendientes bloqueantes** (decisiones o deudas que deben cerrarse antes de implementar), tareas, criterios de aceptación y notas técnicas.
> - Una fase **no se empieza** mientras tenga pendientes bloqueantes abiertos (`- [ ]`). Al cerrar uno, se marca `- [x]` y se anota la decisión tomada en la línea `Decisión:`.
> - Las fases 1 a 6 se desarrollan y prueban en staging, y salen a producción **juntas** en el corte de la Fase 7 (big-bang). La Fase 8 es un release **separado y posterior**.

---

## Índice

1. [Contexto actual](#1-contexto-actual)
2. [Decisiones de arquitectura tomadas](#2-decisiones-de-arquitectura-tomadas)
3. [Arquitectura objetivo](#3-arquitectura-objetivo)
4. [Contratos](#4-contratos)
5. [Reglas para el agente de IA](#5-reglas-para-el-agente-de-ia)
6. [Fase 1: Nuevo token ES256 + JWKS](#fase-1-nuevo-token-es256--jwks)
7. [Fase 2: Sesión: refresh tokens y revocación (`min_iat`)](#fase-2-sesión-refresh-tokens-y-revocación-min_iat)
8. [Fase 3: API Gateway](#fase-3-api-gateway)
9. [Fase 4: Servicios como resource servers estándar](#fase-4-servicios-como-resource-servers-estándar)
10. [Fase 5: Identidad de máquina (servicio a servicio)](#fase-5-identidad-de-máquina-servicio-a-servicio)
11. [Fase 6: Observabilidad, frontend y pruebas end-to-end](#fase-6-observabilidad-frontend-y-pruebas-end-to-end)
12. [Fase 7: Corte big-bang](#fase-7-corte-big-bang)
13. [Fase 8: Extracción del servicio de identidad](#fase-8-extracción-del-servicio-de-identidad)
14. [Registro consolidado de pendientes](#14-registro-consolidado-de-pendientes)

---

## 1. Contexto actual

**Aplicación:** marketplace. Monolito Spring con arquitectura hexagonal, más un microservicio `notifications-service` ya extraído. API Gateway con Spring Cloud Gateway.

**Problema:** el monolito y `notifications-service` validan cada uno por su cuenta el token, el usuario y los roles, con código propio. Esto genera inconsistencias y dificulta el mantenimiento.

**Emisión actual de tokens (monolito, módulo `users`):**

| Elemento | Ubicación | Detalle |
|---|---|---|
| Puerto de salida | `users/application/ports/output/TokenProviderPort.java:13` | `IssuedToken issue(Long userId, List<RoleName> roles);` |
| Invocador | `users/application/services/UserAuthenticationService.java:252` | Helper `toAuthResult(...)`, usado por `login` (:80), `register` (:113) y `acquireRole` (:144, `@Transactional`, reemite el token tras releer roles) |
| Emisor concreto | `users/infrastructure/security/JwtService.java:73` | `@Component` que implementa `TokenProviderPort`. **HS256**, secreto `JWT_SECRET` derivado con SHA-256, fail-fast ante secretos débiles (:48-:71) |
| Verificación (monolito) | `JwtService.resolve(String)` (:95) + `JwtAuthenticationFilter` | Valida firma y expiración y construye `UsernamePasswordAuthenticationToken` con authorities `ROLE_<NAME>` |
| Verificación (notif) | `JwtTokenVerifier` | Solo verifica; no emite |
| Reset de contraseña | `PasswordResetTokenService` | Mecanismo distinto (hash SHA-256, no JWT) |

**Token actual:** `sub` = userId, `roles` = `["CLIENT", ...]`, `iat`, `exp` (1 h, `JWT_EXPIRATION_MS`). Sin `iss`, `aud`, `jti` ni `kid`. Sin refresh token.

**Debilidades que motivan el plan:**
1. HS256 con secreto compartido: cualquiera que verifique puede falsificar tokens.
2. El baneo y la eliminación solo se revisan en el login. Un usuario baneado o un rol revocado siguen activos hasta 1 h.
3. Faltan claims estándar (`iss`, `aud`, `jti`, `kid`).
4. Hay un verificador propio por servicio, que es la fuente de las inconsistencias.

---

## 2. Decisiones de arquitectura tomadas

| # | Aspecto | Decisión | Resumen |
|---|---|---|---|
| 1 | Algoritmo de firma | **ES256 + JWKS** | Solo el emisor tiene la clave privada. Los verificadores usan las claves públicas del JWKS. Rotación con `kid` |
| 2 | Qué extraer | **Users/identidad completo** | Cuentas, credenciales, roles, baneos, emisión, refresh y JWKS en un servicio de identidad (Fase 8) |
| 3 | Estado del usuario | **`min_iat` por usuario en Redis** | El gateway rechaza tokens con `iat < min_iat` |
| 4 | Expiración | **Access de 10–15 min + refresh opaco con rotación** | El refresh revisa baneo y roles actuales |
| 5 | Contenido del token | **`alg`, `kid`, `typ` + `iss`, `aud`, `sub`, `roles`, `iat`, `exp`, `jti`** | Ver [contrato del token](#41-access-token-jwt) |
| 6 | Verificación en servicios | **Spring Security resource server con JWKS** | Misma configuración en gateway, monolito y notif. Se elimina el código propio |
| 7 | Propagación de identidad | **Reenviar el JWT original** | Los servicios lo revalidan con JWKS (defensa en profundidad) |
| 8 | Autorización | **Rol por ruta SOLO en el gateway** | El gateway aplica el gate de rol (p. ej. `/admin/** → ADMIN`). Los servicios **NO** verifican roles: solo reglas de dominio (propiedad/participación de recursos). Decisión 2026-11-08; ver la fila 14 (aislamiento + mTLS) |
| 9 | Migración | **Big-bang** | El proyecto **aún no está desplegado al público** (no hay usuarios reales), así que el re-login forzado del corte no afecta a nadie → big-bang es lo simple y correcto; **no** se necesita ventana de doble validación HS256/ES256. El corte del token y la extracción de identidad (Fase 8) van en **releases separados** |
| 10 | Rate limiting | **Reutilizar el `RateLimiter` del proyecto (no el nativo de SCG)** | El techo global `api-default` (hoy `GlobalRateLimitFilter` en el monolito, sobre `redis-coord` con Lua) **se mueve al gateway** y **se retira del monolito**. NO se usa `RequestRateLimiter` de Spring Cloud Gateway: sería un segundo sistema (precedente **D-04**, "no dos sistemas"). Las cuotas por endpoint (`@RateLimit`: endpoints que notifican / subidas / geocoding) **se quedan en los servicios** (son domain-aware) |
| 11 | Sustrato de revocación | **`min_iat` en `redis-coord` (ya existe)** | Misma instancia que locks + rate limit, con **usuario ACL propio** del gateway (patrón `render-acl.sh`, D-14) y detrás del **circuit breaker Resilience4j** que ya protege `redis-coord`. No inventar resiliencia nueva |
| 12 | Semántica de errores | **401 = auth; 403 = autorización** | **Corrige** la convención actual del proyecto, que mapea ownership/participación a **401** (deuda **M10**, memoria `unauthorized-maps-to-401`). Es un cambio de comportamiento del backend: el interceptor del frontend NO debe refrescar ante un 403 |
| 13 | Variante de SCG | **WebFlux (reactiva)** — ya en uso | El gateway actual ya es `spring-cloud-starter-gateway-server-webflux`. Cierra P3.2 → APIs reactivas (`ReactiveRedisTemplate`, `Mono`/`Flux`) en todo chequeo del gateway |
| 14 | Aislamiento de servicios | **Servicios inalcanzables desde internet + mTLS interno (futuro)** | Como el rol-por-ruta vive solo en el gateway (#8), la exposición de red (P3.8) pasa a ser un **invariante de seguridad duro**: los servicios solo aceptan tráfico del gateway. Control compensatorio a futuro: **mTLS interno** gateway↔servicios. **Preservar esta decisión** |

**Notas sobre ES256 (aplican a todas las fases):**
- Spring Security asume **RS256 por defecto** en `NimbusJwtDecoder` y `NimbusReactiveJwtDecoder`. Hay que configurar ES256 explícitamente en **todos** los verificadores (`jws-algorithms: ES256`), o fallarán con errores poco claros.
- JWS usa la firma ECDSA en formato `R||S`, no DER. jjwt y Nimbus lo manejan; **nunca** se firma ni se verifica con `java.security.Signature` a mano.
- CVE-2022-21449: los JDK 15 a 18 sin parchear aceptaban firmas ECDSA vacías. Todos los servicios deben correr un JDK 17 o 21 actualizado.

### 2.1 Alineación con la infraestructura y las decisiones ya existentes

Este plan **no es greenfield**: se apoya en infraestructura y decisiones ya tomadas en el proyecto (fuente de verdad: `ESTADO-ACTUAL-Y-PLAN-FUTURO.md`). Reutilizarlas es decisión explícita (prima lo ya usado y decidido):

- **Redis ya existe** como `redis-coord` (locks de `@Scheduled` + rate limiting distribuido), con **ACL por servicio** (`infra/redis/acl/render-acl.sh`, D-14) y un **circuit breaker Resilience4j** alrededor (hot-path, timeout 300 ms). `min_iat` vive ahí, con usuario ACL propio del gateway, reusando ese breaker.
- **El rate limiter ya está implementado** (`RateLimiter`/`RedisRateLimiter`, Lua, claves `rl:*`; Bucket4j fue eliminado). Se **reutiliza** moviendo el techo global al gateway; no se introduce el `RequestRateLimiter` nativo de SCG (decisión #10, precedente D-04).
- **Coordinación con el microservicio de chat (otro equipo):** el chat es greenfield y sus docs (`documents/microservicios/chat/`) estaban alineados a HS256. Se actualizaron (2026-11-08) para que **nazca ya como resource server ES256/JWKS** (DP-03 reabierta y recerrada). No necesita migración: apunta a JWKS desde el inicio.

**Interacciones a tener presentes (anotaciones, no bloquean este plan):**

- **`min_iat` resuelve M34 de paso.** Hoy el monolito **relee roles + `isActive` de BD en cada request** (fix B15, `JwtAuthenticationFilter.revalidateRoles`). Con access tokens cortos + `min_iat`, esa consulta por-request desaparece: la revocación es inmediata vía `min_iat` y los roles se refrescan en el refresh. La Fase 4 **elimina** esa relectura.
- **Divergencia de `notifications-service` resuelta.** Hoy notif no puede mirar `isActive` (no es su tabla) → un baneado conserva su bandeja hasta expirar el token. Con `min_iat` en el gateway, eso se cierra sin que notif consulte nada.
- **Bug sistémico de `@Version` (ESTADO §8, CRÍTICO abierto).** `acquireRole` y los nuevos writes de `refresh_tokens` tocan el módulo `users`: cuidado con el `version = null` en `save(mapper.toEntity(dominio))`. No lo resuelve este plan, pero las tareas de Fase 1/2 que escriban en `users` deben contemplarlo.
- **JDK (P1.7):** el repo tiene una situación conocida de JDK (target 21, `java` del PATH es JDK 20); los contenedores usan `eclipse-temurin:21`. Como el CVE-2022-21449 afecta JDK 15–18, el runtime (21) está a salvo; solo confirmar que ningún entorno firme/verifique con un JDK 15–18.

---

## 3. Arquitectura objetivo

```
                         ┌────────────────────────────┐
  Cliente (web/móvil) ──►│      API Gateway (SCG)     │
   Authorization: Bearer │ 1. Valida JWT (ES256/JWKS) │
   Cookie refresh (solo  │ 2. Chequea min_iat (Redis) │──── lee ────► Redis
   en /auth/refresh)     │ 3. Rol por ruta            │               auth:min_iat:{userId}
                         │ 4. Rate limit, CORS, corr. │                    ▲
                         └──────────┬─────────────────┘                    │ escribe
                                    │ reenvía el JWT original              │
          ┌─────────────────────────┼──────────────────────────┐           │
          ▼                         ▼                          ▼           │
 ┌─────────────────┐     ┌────────────────────┐     ┌────────────────────┐ │
 │ Identidad       │     │ Monolito           │     │ notifications-svc  │ │
 │ (users; en el   │     │ (resource server)  │     │ (resource server)  │ │
 │ monolito hasta  │     │ autorización de    │     │ autorización de    │ │
 │ la Fase 8)      │     │ dominio            │     │ dominio            │ │
 │ - login/register│     └────────────────────┘     └────────────────────┘ │
 │ - refresh/logout│                ▲                          ▲           │
 │ - JWKS          │──── eventos (outbox) ─────────────────────┘           │
 │ - client_creds  │────────────────────────────────────────────────────────┘
 └─────────────────┘
```

---

## 4. Contratos

Estos contratos son compartidos entre componentes. **Cualquier cambio en ellos requiere actualizar este documento y a todos los consumidores.**

### 4.1 Access token (JWT)

**Header:**
```json
{ "alg": "ES256", "kid": "<id-de-clave>", "typ": "at+jwt" }
```

**Payload:**
```json
{
  "iss": "<URL lógica del emisor, p. ej. https://auth.<dominio>>",
  "aud": "marketplace-api",
  "sub": "123",
  "roles": ["CLIENT", "SELLER"],
  "iat": 1762632000,
  "exp": 1762632900,
  "jti": "5f0c2b8e-..."
}
```

Reglas:
- `iss` es una **URL lógica estable** que no cambia cuando identidad se extraiga en la Fase 8. No debe ser la URL interna del monolito.
- `sub` es el userId como string. Para tokens de servicio, ver la Fase 5.
- **Prohibido** incluir datos personales (email, nombre, teléfono) o datos que cambien con frecuencia.
- Vida del access token: definida en la Fase 1 (entre 10 y 15 min).

### 4.2 Refresh token

- Valor opaco y aleatorio (≥ 256 bits), **no** JWT.
- Se guarda **hasheado** (SHA-256) en la base de datos de identidad, igual que el reset de contraseña.
- Transporte y almacenamiento en el cliente: según la decisión de la Fase 2.

### 4.3 Revocación (`min_iat`)

| Propiedad | Valor |
|---|---|
| Clave | `auth:min_iat:{userId}` |
| Valor | Epoch en **segundos**, calculado como `ahora + 1` |
| TTL | Vida máxima del access token + tolerancia de reloj |
| Escritor | Solo identidad (el monolito hasta la Fase 8) |
| Lector | Solo el gateway |
| Regla | Rechazar con 401 si `token.iat < min_iat` |

### 4.4 Formato de error (gateway y servicios)

```json
{ "status": 401, "error": "unauthorized", "code": "TOKEN_EXPIRED", "message": "...", "correlationId": "..." }
```
- **401:** token ausente, inválido, expirado o revocado. El cliente debe refrescar, o reautenticar si el refresh falla.
- **403:** autenticado pero sin permiso. El cliente **no** debe refrescar.
- Los valores de `code` se definen en la Fase 3.

### 4.5 Eventos de identidad (Fase 8)

`UserRegistered`, `UserBanned`, `UserUnbanned`, `UserDeleted`, `RoleGranted`, `RoleRevoked`, `ProfileUpdated`. Los esquemas se definen en la Fase 8.

---

## 5. Reglas para el agente de IA

1. **No empieces una fase con pendientes bloqueantes abiertos.** Si encuentras uno sin decisión, detente y pregunta. No asumas una decisión de negocio.
2. **Respeta la arquitectura hexagonal.** La capa de aplicación solo conoce puertos (`TokenProviderPort`, `RefreshTokenPort`, `TokenRevocationPort`, etc.). jjwt, Nimbus, Redis y Spring Security viven en `infrastructure`.
3. **No escribas criptografía propia.** Firma y verificación siempre con jjwt (emisión) y Spring Security / Nimbus (verificación).
4. **No vuelvas a introducir HS256** ni secretos compartidos para firmar tokens de acceso.
5. **Nunca registres tokens en logs** (ni access ni refresh), ni completos ni parciales. Se puede registrar `jti`, `sub` y `kid`.
6. **Configura ES256 explícitamente** en cada verificador. No dependas de defaults.
7. **En el gateway, si la variante es WebFlux, no hagas llamadas bloqueantes.** Usa `ReactiveRedisTemplate` y `Mono`/`Flux`.
8. **Al terminar una tarea**, marca su casilla en este documento y anota cualquier desviación en la sección "Notas de implementación" de la fase.
9. **Los contratos de la sección 4 son la fuente de verdad.** Si una tarea requiere cambiarlos, detente y pide aprobación.
10. Cada tarea que toque seguridad debe incluir pruebas automatizadas (ver los criterios de aceptación de cada fase).

---

## Fase 1: Nuevo token ES256 + JWKS

**Objetivo:** el monolito emite access tokens ES256 con los claims del contrato y expone el JWKS. Gracias a `TokenProviderPort`, el cambio queda contenido en `infrastructure`.

**Prerrequisitos:** ninguno.

### Pendientes bloqueantes

- [x] **P1.1. Almacenamiento de la clave privada.**
  Decisión (2026-11-09): **`.env` local + inyección por `docker-compose`** (mismo patrón que `JWT_SECRET`/`ENCRYPTION_KEY`), nunca versionada. El **gestor de secretos de prod (Vault/KMS) queda como deuda** (P1.1 / D-14 "gestor de prod pendiente", ESTADO §6.6). Formato: base64 del DER (o PEM) en `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`.
- [x] **P1.2. Procedimiento de rotación de claves.**
  Decisión: documentado (publicar la nueva en el JWKS → esperar caché (`max-age=300`) → firmar con la nueva → retirar la vieja al expirar el último token). El JWKS y `JwtKeys` ya se diseñaron para **varias claves**. Periodicidad y procedimiento de emergencia: se concretan al montar el gestor de prod (deuda).
- [x] **P1.3. Valor de `iss`.**
  Decisión: **`https://auth.serviya.local`** (URL lógica estable; sobrevive a la Fase 8). Configurable por `JWT_ISSUER`.
- [x] **P1.4. Valor de `aud`.**
  Decisión: **`marketplace-api`**. Configurable por `JWT_AUDIENCE`.
- [x] **P1.5. Vida del access token.**
  Decisión: **15 min**, en variable **`JWT_ACCESS_TTL`** (default `15m` en `application.yaml`).
- [x] **P1.6. Usar `typ: at+jwt`.**
  Decisión: **Sí** (`JWT_TYP`, default `at+jwt`). Implica que en la Fase 3 los verificadores Nimbus deben aceptar ese `typ` (T3.3).
- [x] **P1.7. Versión del JDK** (CVE-2022-21449).
  Decisión: los contenedores usan `eclipse-temurin:21` → **a salvo** (el CVE afecta JDK 15–18). Compilación local con JDK 21 (CLAUDE.md). Confirmado.
- [x] **P1.8. Coordinación con el equipo de chat (otro equipo).** El chat es greenfield y sus docs apuntaban a HS256; el cambio a ES256/JWKS los afecta antes de que construyan.
  Decisión (2026-11-08): **coordinado.** Se actualizaron `documents/microservicios/chat/` (DP-03 → **ES256/JWKS**, variables de entorno, claims `iss`/`aud`/`kid`/`typ`, el chat no chequea `min_iat`). El chat nace como resource server ES256; no hay migración. Mensaje de aviso enviado al equipo.

### Tareas

- [x] T1.1. Par EC P-256 generado con `openssl` (dev/test). Claves reales por `.env` (prod = deuda, P1.1). `.env.example` trae los comandos de generación.
- [x] T1.2. Config migrada: `JwtProperties` (`@ConfigurationProperties serviya.security.jwt`): `private-key`/`public-key`/`key-id`/`issuer`/`audience`/`access-ttl`/`typ`, mapeadas a `JWT_*` en `application.yaml`, `.env.example` y `docker-compose.yml`. `JWT_SECRET`/`JWT_EXPIRATION_MS` eliminadas del monolito.
- [x] T1.3. Fail-fast en `JwtKeys` (nuevo `@Component`): aborta si falta la clave, no decodifica (base64/PEM), no es EC P-256 (field size 256) o falta el `kid`. `isWeakJwtSecret`/SHA-256 eliminados.
- [x] T1.4. `JwtService.issue(...)` reescrito a ES256 (header `kid`+`typ`; `iss`/`aud`/`sub`/`roles`/`jti`/`iat`/`exp`; `signWith(privateKey, Jwts.SIG.ES256)`).
- [x] T1.5. `JwksController` expone `GET /.well-known/jwks.json` (público en `SecurityConfig`, `Cache-Control: max-age=300`), construido con Nimbus; multi-clave para rotación. Dep `com.nimbusds:nimbus-jose-jwt` añadida.
- [x] T1.6. `JwtService.resolve(...)` mantenido para el `JwtAuthenticationFilter` del monolito, ahora verificando ES256 con la clave pública + `requireIssuer`. Se eliminará en la Fase 4.

### Criterios de aceptación

- [x] El token contiene exactamente los claims del contrato 4.1 (`alg`/`kid`/`typ` + `iss`/`aud`/`sub`/`roles`/`jti`/`iat`/`exp`). Verificado en `JwtServiceTest#issuedTokenCarriesContractClaims`.
- [x] El JWKS no expone el parámetro privado `d`. Verificado en `JwksControllerTest`.
- [x] El arranque falla con clave ausente/ inválida/ de otra curva o `kid` vacío. Verificado en `JwtKeysTest` (6 casos).
- [x] `UserAuthenticationService` **no cambió** (el refresh llega en la Fase 2); `TokenProviderPort.issue` conserva su firma.
- [ ] _Pendiente e2e (manual):_ validar en jwt.io un token de login/register/acquireRole con la clave pública del JWKS (requiere el stack arriba con claves en `.env`).

### Notas de implementación

- **Implementado y verificado 2026-11-09** (rama `api-gateway`). Build + tests: **BUILD SUCCESS**; 12 tests nuevos (`JwtServiceTest` 4, `JwtKeysTest` 6, `JwksControllerTest` 2) + `ServiyaApplicationTests` (context-load con las claves ES256) + `JwtAuthenticationFilterTest` en verde.
- **Archivos:** nuevos `JwtProperties`, `JwtKeys`, `JwksController`, tests (`JwtKeysTest`, `JwksControllerTest`, `JwtTestSupport`); reescritos `JwtService`, `JwtServiceTest`; tocados `SecurityConfig` (permit JWKS), `pom.xml` (Nimbus), `application.yaml`, `.env.example`, `docker-compose.yml`, `ServiyaApplicationTests`.
- **Decisión de formato de clave:** se acepta base64 del DER (una línea, cómodo para `.env`) **o** PEM con armadura; `JwtKeys` limpia la armadura y los espacios antes de decodificar.
- **⚠️ Consistencia del stack durante la transición:** el monolito ya emite **ES256**, pero `notifications-service` aún verifica **HS256** (su conversión a resource server es la **Fase 4**). Hasta entonces, un token nuevo NO validará en la bandeja de notificaciones vía gateway. Es el modelo big-bang/staging del plan (Fases 1–6 juntas al corte); el monolito **solo** sí queda consistente. No desplegar Fase 1 aislada a un entorno con el stack completo esperando funcionar.
- **Interacción `@Version` (ESTADO §8):** no tocó la emisión; pendiente de vigilancia cuando la Fase 2 escriba `refresh_tokens` y cambie `acquireRole`.

---

## Fase 2: Sesión: refresh tokens y revocación (`min_iat`)

**Objetivo:** tokens de acceso cortos con refresh rotativo, y revocación inmediata vía `min_iat` en Redis.

**Prerrequisitos:** Fase 1 completa.

### Pendientes bloqueantes

- [x] **P2.1. Dónde guarda el cliente el refresh token.**
  Decisión (2026-11-09): **cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh`**. El access token corto sigue en el cuerpo JSON (como hoy); el refresh solo viaja en la cookie (XSS no lo lee). Sin app móvil → no hace falta otro mecanismo.
- [x] **P2.2. Protección CSRF.**
  Decisión: **`SameSite=Strict` + verificación de `Origin`** en `/auth/refresh` y `/auth/logout` (no hace falta token CSRF aparte con Strict + Origin). El `Path` acota la cookie a los endpoints de refresh.
- [x] **P2.3. Vida del refresh token.**
  Decisión: **30 días absolutos** aunque se rote (expiración absoluta por `family_id`), configurable (`JWT_REFRESH_TTL`, default `30d`).
- [x] **P2.4. Período de gracia ante reuso.**
  Decisión: **15 s** (`JWT_REFRESH_GRACE`, default `15s`). Un refresh recién rotado reusado dentro de la gracia no dispara la alarma (varias pestañas); fuera de la gracia = reuso → revoca la familia.
- [x] **P2.5. Lista de eventos que escriben `min_iat`.**
  Decisión: **baneo, eliminación de cuenta, quita de rol, cambio de contraseña, reset de contraseña, logout-all**. **No** `acquireRole` (ya reemite el token).
- [x] **P2.6. Infraestructura de Redis.**
  Decisión: **`redis-coord`** (instancia existente, fila 11), con ACL/ breaker ya montados. HA (Sentinel/gestionado) = deuda de prod, no bloquea.
- [x] **P2.7. Bloqueo de cuenta por intentos fallidos de login.**
  Decisión (2026-11-09): **NO se implementa lockout** (duro ni blando) en la Fase 2. El lockout por cuenta habilita un **DoS de bloqueo de la víctima** (contra D-09). Nos apoyamos en el **rate limit por IP** ya existente (`login-ip`). El endurecimiento real —**backoff progresivo por cuenta + MFA de admins**— se trackea en **M29** (ESTADO §6.5), fuera de esta fase. **T2.10 se omite.**

### Tareas

> **Implementación en dos sub-slices:** **(A) refresh tokens** ✅ y **(B) `min_iat`** ✅. Ambos hechos y
> verificados (2026-11-09). Fase 2 completa; falta la verificación e2e (se hace junto con la Fase 6).

**Refresh tokens (Axis A)**
- [x] T2.1. Tabla `refresh_tokens` vía `RefreshTokenEntity` (`ddl-auto`): `id`, `version`, `user_id`, `family_id`, `token_hash` (único CHAR(64)), `issued_at`, `expires_at`, `revoked_at`, `replaced_by_id`, `user_agent`, `ip`.
- [x] T2.2. `RefreshTokenPort` + `RefreshTokenPersistenceAdapter` + repo + mapper MapStruct.
- [x] T2.3. Login y register abren sesión (`startSession` → access + refresh, familia nueva). **acquireRole NO rota** el refresh (deja la cookie; la próxima rotación releerá el rol). `AuthResult` ampliado con `refreshToken`/`refreshExpiresAt`.
- [x] T2.4. `POST /auth/refresh` (`RefreshTokenService.refresh`): lookup por hash; revocado/rotado → reuso ⇒ revoca familia ⇒ 401; expirado → 401; relee cuenta (baneado/eliminado ⇒ revoca familia) y **roles** de BD; dentro de la gracia reemite solo access (mismo refresh); fuera, rota (nuevo refresh en la familia, caducidad absoluta) + `markReplaced` (bulk, evita el bug @Version §8).
- [x] T2.5. `POST /auth/logout`: revoca la familia y limpia la cookie.
- [x] T2.6. `POST /auth/logout-all`: revoca todas las familias del usuario y escribe `min_iat`. **(Axis B)** Endpoint **autenticado** (identidad del access token, no de la cookie); limpia la cookie de este dispositivo.
- [x] T2.7. `RefreshTokenCleanupScheduler` (`@Scheduled` + `DistributedLock`, clave `sched:refresh-token-cleanup`).

**Revocación `min_iat` (Axis B)**
- [x] T2.8. Puerto `TokenRevocationPort` con `revokeAllIssuedBefore(userId, instant)` y adaptador Redis (`RedisTokenRevocationAdapter` sobre `redis-coord`) según el contrato 4.3 (clave `auth:min_iat:{userId}`, valor `ahora + 1 s`, TTL `access-ttl + revocation-clock-skew`). Reusa el breaker `redis-coord`; **mejor esfuerzo** (si Redis cae, no tumba el baneo: la revocación durable es la de familias en BD).
- [x] T2.9. `SessionRevocationService` (`revokeAllSessions` = revoca familias de refresh **+** escribe `min_iat`) invocado en los casos de P2.5: `UserService.banUser`/`changePassword`, `UserDeletionService.deleteUser`, `UserRoleService.revokeRole`, `UserAuthenticationService.confirmPasswordReset` y `logout-all`.
- [x] ~~T2.10. Bloqueo por intentos fallidos.~~ **OMITIDO** (P2.7): se trackea en **M29** (backoff progresivo + MFA admins).

### Criterios de aceptación

- [x] Test: el refresh rota y el anterior deja de servir; dentro de la gracia reemite access sin rotar (`RefreshTokenServiceTest`). **(A)**
- [x] Test: reusar un refresh rotado revoca toda la familia. **(A)**
- [x] Test: el refresh de un usuario baneado/eliminado devuelve 401 y revoca la familia. **(A)**
- [x] Test: el refresh relee los roles desde BD (access con los roles actuales, no los del token). **(A)**
- [x] Test: cada caso de P2.5 escribe `min_iat` con el valor y el TTL correctos. **(B)** `RedisTokenRevocationAdapterTest` (clave/valor/TTL + mejor esfuerzo), `SessionRevocationServiceTest` (orden familias→min_iat, corte `ahora+1s`) y verificación de `revokeAllSessions` en los tests de cada servicio llamador.
- [x] Los refresh tokens nunca se guardan en texto plano (solo SHA-256). Verificado en `RefreshTokenServiceTest`. **(A)**

### Notas de implementación

- **Axis A implementado y verificado 2026-11-09** (rama `api-gateway`): **486 tests, BUILD SUCCESS** (8 nuevos en `RefreshTokenServiceTest` + `UserAuthenticationServiceTest` ajustado + ArchUnit y context-load en verde). Sin regresiones.
- **Cookie (P2.1/P2.2):** el refresh viaja en cookie `HttpOnly; SameSite=Strict; Path=/api/v1/auth` (el Path cubre refresh **y** logout; se amplió respecto a `/auth/refresh` del plan para que logout reciba la cookie). `Secure` configurable (`false` en dev http). El access token sigue en el cuerpo. **Origin check** (P2.2) diferido: SameSite=Strict es la defensa CSRF en esta fase; el allowlist de orígenes lo aplicará el **gateway** (Fase 3, que ya centraliza CORS).
- **Hexagonal/ArchUnit:** `RefreshTokenService` (application) NO importa `JwtProperties` (infra); recibe `refresh-ttl`/`refresh-grace` por `@Value`. Rotación con `@Modifying` bulk (`markReplaced`/`revokeFamily`) para no caer en el bug sistémico de `@Version` (§8).
- **Axis B implementado y verificado 2026-11-09** (rama `api-gateway`): **494 tests, BUILD SUCCESS** (nuevos `SessionRevocationServiceTest` + `RedisTokenRevocationAdapterTest`; extendidos los de `UserService`/`UserRoleService`/`UserAuthenticationService`). `TokenRevocationPort` + `RedisTokenRevocationAdapter` (`auth:min_iat:{userId}`, valor `ahora+1s`, TTL `access-ttl + revocation-clock-skew`, breaker `redis-coord`, **mejor esfuerzo**). `SessionRevocationService` (familias de refresh **+** `min_iat`) cableado en los 5 eventos de P2.5 + `logout-all`.
- **Decisión de diseño (quita de rol):** se usa `revokeAllSessions` (min_iat **+** revoca familias ⇒ re-login completo), más conservador. La alternativa (min_iat-solo, refrescando sin re-login) se descartó por simplicidad y seguridad.
- **`logout-all`** quedó en `POST /api/v1/auth/logout-all` (matcher `authenticated()` **antes** de la regla pública `/auth/**`), no bajo `/users/me`, para agrupar la limpieza de cookie con el resto de auth. La identidad sale del access token (`CurrentUser.id()`).
- **Mejor esfuerzo vs. fail-closed:** el `min_iat` traga errores de Redis (a diferencia del lock, D-08) porque la garantía durable es la revocación de familias en BD; bloquear un baneo porque `redis-coord` está caído sería peor que tolerar una ventana ≤ `access-ttl`.
- **Config nueva:** `serviya.security.jwt.revocation-clock-skew` (`JWT_REVOCATION_CLOCK_SKEW`, default `60s`). Pasadas al `docker-compose.yml` las `JWT_REFRESH_*` + `JWT_REVOCATION_CLOCK_SKEW`.

---

## Fase 3: API Gateway

**Objetivo:** el gateway valida el JWT, aplica `min_iat` y los roles por ruta, y centraliza CORS, rate limiting, el formato de error y el correlation ID.

**Prerrequisitos:** Fase 1 completa (JWKS disponible en staging). La Fase 2 debe estar completa para probar `min_iat`.

### Pendientes bloqueantes

- [x] **P3.1. Política si Redis no responde** (chequeo de `min_iat`). *Fail-closed* (rechazar, seguro pero tumba la app) o *fail-open* (dejar pasar con alerta; ventana máxima igual a la vida del access token). Es una decisión de negocio.
  Decisión (2026-11-09, confirmada): **fail-open con alerta**, apoyado en el TTL corto del access token (15 min) como ventana máxima de exposición. Reusa el circuit breaker Resilience4j que ya envuelve `redis-coord` (fila 11) y emite métrica/alerta al abrir. Fail-closed tumbaría todo el tráfico autenticado ante un parpadeo de Redis; la revocación durable (familias de refresh en BD) no depende de esto. Alineado con D-07.
- [x] **P3.2. Variante de Spring Cloud Gateway.** `spring-cloud-starter-gateway-server-webflux` (reactiva/Netty) o `-server-webmvc` (servlet).
  Decisión: **WebFlux (reactiva)** — ya es la del gateway actual (`backend/api-gateway/pom.xml`). Decisión #13. Implica APIs reactivas (`ReactiveRedisTemplate`, `Mono`/`Flux`) en todo lo que sigue; nada bloqueante (regla #7).
- [x] **P3.3. Matriz de rutas públicas** (sin token). Mínimo: `/auth/login`, `/auth/register`, `/auth/refresh`, `/.well-known/jwks.json`, password reset. ¿El catálogo público del marketplace?
  Decisión (2026-11-09): se **derivan del `SecurityConfig` actual del monolito** (ya codifica público/privado), para mantener gateway y servicios en sync. **Públicas:** `POST /auth/login`, `/auth/register`, `/auth/refresh`, `/auth/password-reset`, `/auth/password-reset/validate`, `/auth/password-reset/confirm`; y el **catálogo público de solo-lectura**: `GET /services`, `GET /services/*`, `GET /categories`, `GET /offerers/*` (perfil público RF-027). Swagger/openapi **cerrado en prod**. `logout-all` es **autenticada**.
  **Excepción (corrección 2026-11-09): `/.well-known/jwks.json` NO es pública.** El gateway **no** la expone a internet; solo la consumen el gateway y los servicios por la **red interna** (gateway↔monolito, servicio↔monolito). Va atada a P3.8 (aislamiento de red): nadie de fuera llega al emisor. El `permitAll` del monolito sobre esa ruta se mantiene (es permitAll en la red interna, no en el borde).
- [x] **P3.4. Matriz de rol por ruta** (autorización gruesa). Ejemplo: `/admin/** → ADMIN`, `/seller/** → SELLER`. Debe salir de un inventario de los endpoints actuales.
  Decisión (2026-11-09): **solo gates ADMIN** en el gateway (grano grueso): `/api/v1/admin/** → ADMIN`, `/api/v1/reports/*/actions/** → ADMIN`, `GET /api/v1/reports` y `GET /api/v1/reports/* → ADMIN`, `POST /api/v1/categories → ADMIN`. El resto (no público) = `authenticated()`. La autorización **fina** (propiedad/participación, bypass admin) **se queda en los servicios** (patrón `role-security-split`). No hay rutas OFFERER/SELLER-gated hoy (usan owner-check en servicio). Inventario de endpoints = primera tarea de implementación para confirmar la matriz.
- [x] **P3.5. Catálogo de códigos de error** (`TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_REVOKED`, `FORBIDDEN`, `RATE_LIMITED`, ...).
  Decisión (2026-11-09): enum compartido alineado al contrato 4.4. **401:** `TOKEN_MISSING`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_REVOKED` (incluye `iat < min_iat` y familia revocada). **403:** `FORBIDDEN`. **429:** `RATE_LIMITED`. Body `{ status, error, code, message, correlationId }`. El interceptor del frontend decide refrescar (401) vs no (403) por `code`; gateway y servicios (Fase 4) comparten el enum.
- [x] **P3.6. Orígenes CORS permitidos** por entorno.
  Decisión (2026-11-09): **allowlist por entorno vía env** (`GATEWAY_CORS_ALLOWED_ORIGINS`, ya presente en el `application.yaml` del gateway), centralizada en el gateway, `allow-credentials: true`, **sin `*`** (la cookie de refresh con `credentials:'include'` exige eco de origen concreto). dev: `http://localhost:5173`. **Dominio de staging/prod: placeholder por ahora** (`https://TODO-prod-domain` — rellenar antes del corte de Fase 7).
- [x] **P3.7. Rate limiting: qué sistema y qué se mueve.** Decisión (fila #10): **reutilizar el `RateLimiter`/`RedisRateLimiter` del proyecto** (Lua sobre `redis-coord`, claves `rl:*`), **no** el `RequestRateLimiter` nativo de SCG (evita dos sistemas, precedente D-04). El **techo global `api-default`** se **mueve del monolito al gateway** (se retira `GlobalRateLimitFilter` del monolito en la Fase 4). Las **cuotas por endpoint** (`@RateLimit`: endpoints que notifican, subidas multipart, geocoding) **se quedan en los servicios** (son domain-aware; el gateway no sabe que una petición dispara una notificación).
  **Hecho (2026-11-09):** las cuotas de auth por IP (login/registro/reset, ventana deslizante) se portaron al gateway (`AuthRateLimitWebFilter` + `serviya.ratelimit.auth`), reutilizando el mismo `.lua` y las claves `rl:*`; el `RateLimitingFilter` del monolito se eliminó (T4.9). `reset-email` (por correo) se queda en el monolito (domain-aware).
- [x] **P3.8. Exposición de red.** Confirmar que el monolito, notif e identidad **no** son accesibles desde internet; solo el gateway.
  Decisión (2026-11-09): **solo el gateway se expone**; monolito, notif y (futuro) identidad quedan en la red interna. Refuerza P3.3 (JWKS interno) y es la base del `min_iat` solo-en-gateway (P4.1) y del mTLS futuro (fila 14). La materialización concreta (reglas de red / compose / orquestador) se detalla en el despliegue (Fase 7); aquí se fija como invariante de arquitectura.

### Tareas

- [x] T3.1. Configurar el resource server (asumiendo WebFlux; adaptar según P3.2): **(Slice 3A)** implementado **programáticamente** (`GatewaySecurityConfig.jwtDecoder`), no por las propiedades Boot de abajo, para poder fijar el `jwtProcessorCustomizer` (typ) y conservar la carga perezosa del JWKS. Valida keys (JWKS)+`iss`+`aud`+timestamps; `aud` con `JwtAudienceValidator` propio. Config en `serviya.security.jwt.*`.
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            jwk-set-uri: http://<identidad>/.well-known/jwks.json
            issuer-uri: <valor de P1.3>      # con jwk-set-uri presente se usa para validar iss (verificar comportamiento en la versión de Boot)
            audiences: marketplace-api
            jws-algorithms: ES256            # OBLIGATORIO: el default es RS256
  ```
- [x] T3.2. Tolerancia de reloj de 30–60 s (`JwtTimestampValidator` con `clockSkew`). **(Slice 3A)** Configurable (`JWT_CLOCK_SKEW`, default `60s`).
- [x] T3.3. Si P1.6 = sí: aceptar `typ: at+jwt`. Nimbus solo acepta `JWT` o ausencia de `typ` por defecto. Personalizarlo con `jwtProcessorCustomizer` y `DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("at+jwt"))` (verificar la API en la versión de Spring Security usada). **(Slice 3A)** Acepta `at+jwt`, `JWT` y ausente.
- [x] T3.4. Convertidor de `roles` a authorities `ROLE_*` (`JwtGrantedAuthoritiesConverter` con `authoritiesClaimName("roles")` y `authorityPrefix("ROLE_")`; en reactivo, envuelto en `ReactiveJwtAuthenticationConverterAdapter`). **(Slice 3A)** `ReactiveJwtAuthenticationConverter` + adaptación síncrono→`Flux`.
- [x] T3.5. **Chequeo de `min_iat`:** implementarlo como **decorador del `ReactiveJwtDecoder`** (decodifica, luego `flatMap` a Redis, y si `iat < min_iat` emite `JwtValidationException`). Así el 401 sale por el flujo estándar de Spring Security. No usar `OAuth2TokenValidator` para esto, porque es síncrono. Aplicar P3.1 ante errores de Redis. **(Slice 3B)** `MinIatReactiveJwtDecoder` sobre `ReactiveStringRedisTemplate` (`redis-coord`, `spring.data.redis.*`, timeout 250ms, conexión perezosa). **Fail-open** con métrica `gateway.min_iat.check{result=...}`. El código `TOKEN_REVOKED` concreto llega con T3.7.
- [x] T3.6. Reglas de autorización por ruta según P3.3 y P3.4. **Todo lo no listado requiere autenticación** (`anyExchange().authenticated()`). **(Slice 3A)** Públicas (auth + catálogo read-only), ADMIN (admin/reports-actions/reports-GET/categories-POST), `offerers/me/**` autenticado antes del perfil público, OPTIONS permitido.
- [x] T3.7. Manejo de errores 401/403 con el formato 4.4 y el header `WWW-Authenticate`. **(Slice 3C)** `GatewayAuthenticationEntryPoint` (401) + `GatewayAccessDeniedHandler` (403) escriben el cuerpo `{status,error,code,message,correlationId}` vía `GatewayErrorWriter`; enum `GatewayErrorCode` (`TOKEN_MISSING/EXPIRED/INVALID/REVOKED`, `FORBIDDEN`, `RATE_LIMITED`). El código 401 se deduce del `OAuth2Error` (emparejado por texto: `min_iat`→REVOKED, `expired`→EXPIRED, resto→INVALID; sin token→MISSING). `WWW-Authenticate: Bearer[ error=...]`.
- [x] T3.8. CORS global en el gateway según P3.6. **(Slice 3E/cierre)** `globalcors` con allowlist por entorno (`GATEWAY_CORS_ALLOWED_ORIGINS`), `allow-credentials: true`, sin `*`. Dominio de prod = placeholder (P3.6). Verificado: origen permitido recibe `Access-Control-Allow-Origin`; origen fuera de la lista no (preflight → 403).
- [x] T3.9. **Techo global de rate limiting en el gateway reutilizando el `RateLimiter` del proyecto** (no el `RequestRateLimiter` de SCG). Portar el `RedisRateLimiter` (Lua sobre `redis-coord`, claves `rl:*`, política `api-default`) a un `GlobalFilter`/`WebFilter` reactivo del gateway, con el mismo sujeto (usuario autenticado del JWT ya validado, o IP si es anónimo). Equivale a mover aquí el `GlobalRateLimitFilter` que hoy vive en el monolito. Usar `ReactiveRedisTemplate` (regla #7). El retiro del filtro del monolito es T4.9. **(Slice 3E)** `ReactiveRedisRateLimiter` (mismo `.lua` token bucket, misma clave `rl:v1:{...}:api-default`, capacidad 200/5s, modo OPEN) + `GlobalRateLimitWebFilter` (`@Order(0)`, tras Security). 429 con cuerpo 4.4 `RATE_LIMITED` + `Retry-After`. Reusa el breaker `redis-coord` (comparte apertura con `min_iat`). Nota: subject por IP usa `getRemoteAddress` (XFF de confianza = refinamiento).
- [x] T3.10. Filtro de correlation ID: generar `X-Correlation-Id` si no viene, propagarlo a los servicios y devolverlo en la respuesta. **(Slice 3D)** `CorrelationIdWebFilter` (máxima precedencia, antes de Security ⇒ el cuerpo de error 4.4 lleva el mismo id). Reescribe la cabecera de la request (se reenvía a los servicios), la fija en la respuesta (`beforeCommit`) y la pone en el contexto reactivo; respeta el id del cliente si viene.
- [x] T3.11. Verificar que el header `Authorization` se reenvía intacto a los servicios. **(Slice 3D)** Verificado en `ApiGatewayRoutingTest` (el backend stub recibe `Bearer <token>` intacto).
- [x] T3.12. Arranque tolerante: el gateway debe arrancar aunque el JWKS no esté disponible todavía, y reintentar. **(Slice 3A)** `NimbusReactiveJwtDecoder.withJwkSetUri` carga las claves de forma perezosa (primer token), así el gateway arranca sin JWKS disponible. Reintento explícito/caché = refinamiento posterior.

### Criterios de aceptación

- [x] Tests de integración: token válido → 200. Expirado, mala firma, `iss` o `aud` incorrecto, o algoritmo distinto de ES256 → 401. (`ApiGatewayRoutingTest`.)
- [x] Test: token con `alg: HS256` o `alg: none` → 401. HS256 verificado; `none`/sin firma lo rechaza la restricción explícita `jwsAlgorithm(ES256)`.
- [x] Test: `iat < min_iat` → 401 (`MinIatReactiveJwtDecoderTest`); el código `TOKEN_REVOKED` del cuerpo 4.4 en `GatewayAuthenticationEntryPointTest`.
- [x] Test: comportamiento ante Redis caído según P3.1 (fail-open verificado en `MinIatReactiveJwtDecoderTest`).
- [x] Test: ruta con rol insuficiente → 403 (no 401). (`ApiGatewayRoutingTest`: CLIENT en `/reports` → 403 `FORBIDDEN`.)
- [x] Test: las rutas públicas funcionan sin token. (`ApiGatewayRoutingTest`: `/services`, `/categories` sin token → 200.)
- [x] Test: el rate limit responde 429 al superar el límite (`GlobalRateLimitWebFilterTest` + `ReactiveRedisRateLimiterTest`: decisión allow/reject/fail-open).
- [ ] Los servicios no responden headers CORS propios (verificado en la Fase 4).

### Notas de implementación

- **Slice 3A implementado y verificado 2026-11-09** (rama `api-gateway`, módulo `backend/api-gateway`): **12 tests, BUILD SUCCESS**. Resource server reactivo ES256 (`GatewaySecurityConfig` + `JwtAudienceValidator`), roles→authorities, matriz de rutas (P3.3/P3.4) y arranque tolerante. Test e2e (`ApiGatewayRoutingTest`): backends stub + `MockWebServer` sirviendo el JWKS + tokens ES256 firmados (`TestJwks`); cubre token válido→enruta, sin token/HS256/expirado/`aud`/`iss` incorrectos→401, CLIENT en ruta ADMIN→403, ADMIN→200, públicas sin token→200, CORS en el borde. Deps nuevas: `spring-boot-starter-security`, `-oauth2-resource-server`, `spring-security-test` + `mockwebserver` (test).
- **Slice 3B implementado y verificado 2026-11-09** (módulo `backend/api-gateway`): **19 tests, BUILD SUCCESS** (7 nuevos en `MinIatReactiveJwtDecoderTest` + los 12 e2e siguen verdes). `MinIatReactiveJwtDecoder` decora el decoder base: tras validar el token, consulta `auth:min_iat:{sub}` en `redis-coord` y rechaza con `JwtValidationException` si `iat < min_iat`. **Con esto, la revocación de la Fase 2B ya surte efecto de extremo a extremo.** Deps nuevas: `spring-boot-starter-data-redis-reactive` (+ `reactor-test`) y `spring-cloud-starter-circuitbreaker-reactor-resilience4j`. Config `spring.data.redis.*` (host/port/timeout; creds del ACL `svc-gateway-coord` por env en prod).
- **P3.1 con circuit breaker (implementado):** la consulta a `min_iat` va envuelta en un `CircuitBreaker` Resilience4j (`redis-coord`: ventana 10, mínimo 5, 50% de fallo, 5 s abierto) con la fuente en `Mono.defer`, así al abrir **corta al instante sin intentar la conexión** (no paga el timeout por request en el hot-path). **Fail-open** tanto ante error como con el breaker abierto, con métrica de alerta `gateway.min_iat.check{result=error|breaker-open|revoked|allowed|no-key}`. Verificado que el breaker abierto no vuelve a tocar Redis.
- **Slice 3C implementado y verificado 2026-11-09** (módulo `backend/api-gateway`): **23 tests, BUILD SUCCESS**. Formato de error 4.4 uniforme en 401/403 con enum `GatewayErrorCode` y `WWW-Authenticate`. Tests: e2e cubre `TOKEN_MISSING`/`TOKEN_EXPIRED`/`TOKEN_INVALID`/`FORBIDDEN` con forma del cuerpo; unit (`GatewayAuthenticationEntryPointTest`) cubre `TOKEN_REVOKED` y el reto Bearer. El `correlationId` se lee de `X-Correlation-Id` si viene (lo pondrá T3.10) o se genera.
- **Slice 3D implementado y verificado 2026-11-09** (módulo `backend/api-gateway`): **26 tests, BUILD SUCCESS**. `CorrelationIdWebFilter` (T3.10) + verificación del passthrough de `Authorization` (T3.11). El correlation id corre antes de Security, así el cuerpo de error 4.4 queda correlacionado con los logs de los servicios.
- **Slice 3E implementado y verificado 2026-11-09** (módulo `backend/api-gateway`): **34 tests, BUILD SUCCESS**. Techo global `api-default` portado al gateway (`ReactiveRedisRateLimiter` + `GlobalRateLimitWebFilter`), mismo `.lua`/clave/política que el monolito, modo OPEN, breaker `redis-coord` compartido. 429 con cuerpo 4.4. El retiro del `GlobalRateLimitFilter` del monolito es T4.9 (Fase 4).
- **Fase 3 (código) COMPLETA 2026-11-09** (módulo `backend/api-gateway`, **35 tests, BUILD SUCCESS**): T3.1–T3.12 hechos. T3.8 cerrado con el `globalcors` por entorno (placeholder de prod pendiente de dominio real, P3.6). **Queda solo la verificación e2e en staging, que el plan agrupa en la Fase 6.** Siguiente: **Fase 4** (servicios como resource servers estándar: el monolito y notif validan el JWT con la misma config JWKS/ES256, se elimina su verificación propia y se retira el `GlobalRateLimitFilter` del monolito, T4.9).

---

## Fase 4: Servicios como resource servers estándar

**Objetivo:** el monolito y notif verifican el JWT con la misma configuración estándar (JWKS + ES256). Se elimina todo el código de verificación propio. Los servicios conservan solo la autorización de dominio.

**Prerrequisitos:** Fases 1 y 3 completas.

### Pendientes bloqueantes

- [x] **P4.1. ¿Los servicios también chequean `min_iat`?**
  Decisión: **No.** El chequeo de `min_iat` vive solo en el gateway (fila 11). Los servicios no tocan `redis-coord` para auth. El aislamiento de red (P3.8) + mTLS futuro (fila 14) es la garantía de que nadie llega a un servicio sin pasar por el gateway.
- [x] **P4.2. ¿Se mantienen las reglas de rol por ruta en los servicios?**
  Decisión (2026-11-08, fila #8): **No.** Los servicios **dejan de verificar roles**; el rol-por-ruta vive **solo** en el gateway. Los servicios conservan **solo las reglas de dominio** (propiedad/participación de recursos: "el oferente solo edita su servicio", "solo el participante ve la solicitud"). **Control compensatorio:** los servicios quedan inalcanzables desde internet (P3.8) y, a futuro, **mTLS interno** gateway↔servicios (fila 14). Esta decisión se **preserva**. *Nota:* quita la defensa en profundidad de autorización → P3.8 pasa de recomendación a invariante duro; por eso el mTLS.
- [x] **P4.3. Inventario de reglas de autorización de dominio** existentes (por ejemplo, "el vendedor solo edita sus productos", "solo el comprador califica su pedido"), para confirmar que ninguna se pierde al quitar el filtro propio.
  Decisión/inventario (2026-11-09, verificado en código — todas usan `CurrentUser.id()` = `sub` del JWT y **se quedan** en los servicios; ninguna es rol-por-ruta):
  - **services / `MarketplaceService`:** editar/activar/desactivar/borrar servicio → dueño (`requesterId == service.offererId`).
  - **profiles / `AddressService`:** editar/borrar dirección → dueño (`requesterId == address.userId`).
  - **profiles / `OffererAvailabilityService`:** editar/borrar franja → dueño (`requesterId == slot.offererId`).
  - **requests / `ServiceRequestCommandService`:** `requireOwnership(ownerId, actorId)` + `requireParticipant`; cancel distingue cliente/oferente.
  - **requests / `ServiceRequestQueryService`:** ver detalle → participante **o admin** (`!isAdmin && !isParticipant`).
  - **requests / `RescheduleProposalService`:** `requireOwnership` + participante (cliente/oferente).
  - **reports / `RequestReportService`:** crear reporte sobre una solicitud → participante.
  - **feedback / `ServiceFeedbackService`:** feedback de servicio → solo el cliente de la solicitud.
  - **feedback / `ClientFeedbackService`:** feedback al cliente → solo el oferente de la solicitud.
  - **admin / `AdminActionGuard`:** `requireNotSelf` (un admin no se banea/elimina) + "debe quedar otro admin activo".
  - **Bypass admin de grano fino:** `CurrentUser.isAdmin()` (lee `ROLE_ADMIN` de las authorities) se usa en `ServiceRequestQueryService` → el converter del resource server **debe** seguir mapeando `roles`→`ROLE_*` (igual que el gateway) para que siga funcionando.
  - **Hallazgo para T4.6:** hoy TODAS estas reglas lanzan `UnauthorizedException`→**401**. Separar ownership/participación a **403** (decisión #12/M10) exige una **excepción distinta** (p. ej. `ForbiddenException`→403) y remapear ~10 sitios, **distinguiéndolas** de las `UnauthorizedException` que SÍ son autenticación (login inválido, baneado, refresh) y siguen en 401.

- [x] **P4.4 (añadido 2026-11-09). Exposición de red durante la Fase 4.** Decisión del usuario: **cablear el gateway como única entrada ahora** (no diferir a Fase 7). El gateway entra en `docker-compose.yml` como único puerto publicado; monolito y notif dejan de mapearse al host (solo red interna). Así P3.8 (control compensatorio de la decisión #8) es real desde que el monolito pierde su rol-gating, no una promesa futura.

### Tareas

- [x] T4.1. Monolito: agregar `spring-boot-starter-oauth2-resource-server` y la misma configuración de T3.1 a T3.4 (versión servlet). **(4A)** Decoder ES256 construido con la **clave pública local** del emisor (`JwtKeys`, sin auto-llamada HTTP al JWKS) + `typ`/`iss`/`aud`/timestamps; converter `roles→ROLE_*`.
- [x] T4.2. Monolito: **eliminar** `JwtService.resolve(...)` y `JwtAuthenticationFilter`. `JwtService` queda solo como emisor (hasta la Fase 8). **Eliminar también** las reglas de **rol por ruta** del `SecurityConfig`. **(4A)** Hechas: `JwtAuthenticationFilter` y su test borrados, `resolve()` eliminado; los `hasRole("ADMIN")` (admin/**, reports actions, GET reports, POST categories) pasan a `authenticated()`. La revalidación de roles/estado por petición (M34) desaparece con el filtro.
- [x] T4.3. Notif: misma configuración y **eliminar** `JwtTokenVerifier` y su filtro. **(4B)** Notif pasa de **HS256 a resource server ES256** leyendo el JWKS del emisor (`withJwkSetUri`, como el gateway; notif no es el emisor). Eliminados `JwtTokenVerifier`, `JwtAuthenticationFilter` (+`JwtTokenVerifierTest`) y las deps jjwt. `CurrentUser.id()` lee `jwt.getSubject()` (fallback `Long`). Config `serviya.security.jwt.*` (sin `JWT_SECRET`). El test de integración migró a `jwt()` (post-procesador de `spring-security-test`). **Importante:** notif estaba roto para los tokens ES256 (seguía en HS256 desde la Fase 1); esto lo arregla.
- [x] T4.4. Obtener el userId en el código de dominio desde `Jwt#getSubject()` mediante un único helper o adaptador por servicio. **(4A)** Se mantiene la **misma API `CurrentUser.id()`** cambiando sus tripas para leer `jwt.getSubject()` (con fallback a un principal `Long` para los tests de slice web) → los ~N controladores y las ~10 reglas de dominio no se tocaron. `RateLimitSubject.of` también actualizado para el principal `Jwt`.
- [x] T4.5. Quitar la configuración CORS de los servicios (queda en el gateway). **(4A)** CORS retirado del `SecurityConfig` del monolito.
- [x] T4.6. Aplicar el formato de error 4.4 en las respuestas 401/403 de los servicios. **Cambio de comportamiento (decisión #12 / M10).** **(4C)** Nueva `ForbiddenException`→**403** (`code: FORBIDDEN`); los ~10 sitios de propiedad/participación (MarketplaceService, AddressService, OffererAvailabilityService, ServiceRequest*Service, RescheduleProposalService, RequestReportService, Service/ClientFeedbackService) migraron de `UnauthorizedException` a `ForbiddenException`. La autenticación (login/baneo/refresh) **sigue 401** (`UnauthorizedException`, `code: UNAUTHORIZED`). `ErrorResponse` ampliado con `code`+`correlationId` (4.4; conserva `path`/`timestamp`); `GlobalExceptionHandler` y `RestAccessErrorHandler` alineados. Memoria `unauthorized-maps-to-401` actualizada. **Pendiente: avisar al frontend** (no refrescar ante 403). Nota: notif usa 404 para IDOR; su handler aún sin `code` 4.4 (menor).
- [x] T4.7. Migrar los tests a `spring-security-test` (`jwt()`/`mockJwt()`), sin depender del emisor real. **(4A — no hizo falta migrar)** Los tests de slice web (`AccountControllerIntegrationTest`, `OffererAvailabilityControllerIntegrationTest`) usan `@AutoConfigureMockMvc(addFilters=false)` + un principal `Long` directo; el fallback de `CurrentUser.id()`/`RateLimitSubject` los mantiene verdes sin cambios. Si en 4B/4C se añaden tests que ejerciten la cadena real, se usará `jwt()`.
- [x] T4.8. Revisar y cubrir con tests las reglas de dominio de P4.3. **(4C)** Las ~10 reglas ya tenían tests (asserts de la excepción de autorización); migrados de `UnauthorizedException` a `ForbiddenException` (servicios + `OffererAvailabilityControllerIntegrationTest` ahora espera **403**). **487 tests, BUILD SUCCESS.**
- [x] T4.9. **Retirar el rate limiting global del monolito.** **(4A + rate-limit slice 2026-11-09)** `GlobalRateLimitFilter` retirado en 4A. **Ahora también el `RateLimitingFilter`**: las cuotas de auth por IP (login/registro/reset, ventana deslizante) se **portaron al gateway** (`ReactiveRedisRateLimiter.tryConsumeSliding` + `AuthRateLimitWebFilter`, `serviya.ratelimit.auth`; `.lua` de ventana deslizante + ACL `z*` para `svc-gateway-coord`). El gateway ve la IP real del cliente en el borde → es el sitio correcto. En el monolito queda solo el rate limiting **domain-aware**: `reset-email` (por correo, en `AuthController`) y el interceptor `@RateLimit` por endpoint. Cierra P3.7. **gateway 46 · monolito 489 tests.**

### Criterios de aceptación

- [x] No queda ninguna clase propia de verificación de JWT en el monolito ni en notif. **Verificado** (grep de `verifyWith`/`JwtTokenVerifier`/`JwtAuthenticationFilter` en `*/src/main` = vacío). `JwtService` solo emite.
- [x] La configuración de seguridad JWT es **idéntica** en ambos servicios, salvo URLs. ES256 + `iss`/`aud`/`typ`/timestamps + converter `roles→ROLE_*`; difieren solo en el origen de la clave (monolito: clave pública local porque es el emisor; gateway y notif: JWKS del emisor).
- [x] Todas las reglas de P4.3 tienen tests. (Migrados a `ForbiddenException`/403 en 4C.)
- [x] Las suites de tests existentes pasan. Monolito **487**, notif **30**, gateway **35** — todo BUILD SUCCESS.

> **🎉 FASE 4 COMPLETA (2026-11-09).** Monolito y notif son resource servers ES256 estándar sin verificación propia; rol-por-ruta y CORS centralizados en el gateway; ownership/participación → 403; red aislada (solo el gateway expuesto).
> **Pendientes menores:** (a) ✅ **frontend 401→403:** sin cambio necesario — `api.js` ya distingue (solo el **401** limpia el token y redirige; el **403** cae a un error normal sin desloguear). La Fase 6 (T6.5–T6.7) añadirá el interceptor de refresh. (c) ✅ **`code` 4.4 en notif:** `ErrorResponse`/handlers de notif alineados (`code` + `correlationId` de `X-Request-Id`). (b) ✅ **retiro total de `RateLimitingFilter`:** hecho — las cuotas de auth por IP se portaron al gateway (`AuthRateLimitWebFilter` + ventana deslizante en `redis-coord`) y el filtro del monolito se eliminó (cierra T4.9/P3.7).
> **Siguiente: Fase 5** (identidad de máquina / `client_credentials` + mTLS interno) — tiene pendientes bloqueantes propios (P5.1–P5.6) a revisar antes.

### Notas de implementación

- **Slice 4-infra hecho 2026-11-09** (P4.4 materializado, **35 tests del gateway en verde**): el `docker-compose.yml` ya tenía al gateway como **único puerto** (`8080`) y a `backend`/`notif` en `expose` (red interna); faltaba hacer **funcional** al gateway en Docker. Añadido:
  - Gateway en `docker-compose.yml`: env de validación JWT (`JWT_JWKS_URI`→`http://backend:8080/.well-known/jwks.json`, `JWT_ISSUER`/`JWT_AUDIENCE`/`JWT_CLOCK_SKEW`) + acceso a `redis-coord` (`REDIS_COORD_*`, usuario `svc-gateway-coord`) + red `redis-net`.
  - Gateway `application.yaml`: `spring.data.redis.username/password` desde `REDIS_COORD_USER/PASSWORD`.
  - Redis ACL: nuevo usuario `svc-gateway-coord` (`~auth:min_iat:* ~rl:*` + comandos del token bucket; sin `z*`/`lock:*`) en `users.coord.acl.tpl` + `render-acl.sh` + `.env.example` (raíz e `infra/redis`).
  - **Operación:** antes del `up` hay que **re-renderizar** el ACL (`infra/redis/acl/render-acl.sh`) para incluir el nuevo usuario; el frontend sigue llamando a `:8080` (el gateway heredó ese puerto del monolito, así que su config no cambia).
- **Slice 4A hecho y verificado 2026-11-09** (monolito, **487 tests, BUILD SUCCESS**): el monolito es resource server estándar. `pom`: `spring-boot-starter-oauth2-resource-server`. `SecurityConfig` reescrito (decoder ES256 con clave pública local + converter roles, sin CORS, `hasRole`→`authenticated()`, resource server; `GlobalRateLimitFilter` fuera, `RateLimitingFilter` dentro). `CurrentUser.id()` lee `jwt.getSubject()` (fallback `Long`); `RateLimitSubject` idem. `JwtService.resolve()` y `JwtAuthenticationFilter`(+test) **eliminados**; `JwtServiceTest` recortado a la emisión. Nuevo `JwtAudienceValidator` (servlet). Las ~10 reglas de dominio (P4.3) y los tests de slice web pasan sin cambios.
- **Decisión de implementación (decoder del monolito):** valida con la **clave pública local** (`JwtKeys`), no vía `jwk-set-uri` a su propio JWKS, para evitar una auto-llamada HTTP y el acople de arranque. Config equivalente a la del gateway salvo el origen de la clave (criterio "idéntica salvo URLs").
- **Slice 4B hecho y verificado 2026-11-09** (notifications-service, **30 tests, BUILD SUCCESS**, incl. integración Testcontainers): notif es resource server ES256 vía JWKS del emisor. `pom`: fuera jjwt, dentro `oauth2-resource-server`. `SecurityConfig` con decoder `withJwkSetUri` + converter + validadores (mismo patrón que el gateway); `CurrentUser.id()` lee `sub`; nuevo `JwtAudienceValidator`. Eliminados `JwtTokenVerifier`/`JwtAuthenticationFilter`/test; `SecretKeys` se conserva (lo usa `PiiAttributeConverter`). `docker-compose` del notif: `JWT_JWKS_URI`/`ISSUER`/`AUDIENCE` en vez de `JWT_SECRET`. Test de integración migrado a `jwt()`. **Config JWT idéntica en gateway y notif salvo que ambos leen el JWKS; el monolito usa su clave local** (es el emisor).

---

## Integración con el microservicio de chat (merge de `main`, 2026-11-09)

Al traer `origin/main` (PR #56, el equipo de chat) a la rama `api-gateway` aparecieron solapes con el gateway/compose/ACL. Hallazgos y resoluciones:

1. **WebSocket del chat autentica con TICKET en la query, no Bearer.** El handshake `GET /api/chat/v1/ws?ticket=...` no lleva `Authorization` (los navegadores no pueden ponerlo en WS). Nuestra cadena `anyExchange().authenticated()` lo rechazaría con 401. **Resolución:** `/api/chat/v1/ws` → `permitAll` en el gateway (el chat-service valida el ticket de un solo uso). ⚠️ *Gap:* el gateway NO aplica `min_iat` al handshake WS; la revocación en vivo de una conexión WS larga es responsabilidad del chat (TTL del ticket + ciclo de vida de la conexión).
2. **JWKS del chat (P3.3 se mantiene).** El chat leía el JWKS en `gateway:8080/.well-known/jwks.json`, pero **P3.3 dice que el JWKS es privado y no se expone en el borde**. **Resolución (decisión del usuario):** NO se abre el JWKS en el gateway; el chat lo lee **directo del monolito** (`backend:8080/.well-known/jwks.json`) por la red interna, igual que notif. Cambiado el default en `chat-service/application.yaml`, `docker-compose.yml` y `.env.example`. **Verificado:** el chat ya valida completo (`JwtDecoderConfig`: ES256 + `typ=at+jwt` + `iss` + `aud` + timestamps; `JwtTokenValidationTest` en verde). Está tan alineado como notif/gateway.
3. **Correlación duplicada (dos filtros, dos cabeceras).** El chat añadió `CorrelationIdFilter` (`GlobalFilter`, `X-Request-Id`) y nosotros `CorrelationIdWebFilter` (`WebFilter`, `X-Correlation-Id`). **Resolución:** unificado en **`X-Request-Id`**; se **conserva nuestro `WebFilter`** (corre antes de Security ⇒ cubre los 401/403 del borde, que el `GlobalFilter` del chat no alcanza) renombrando su cabecera, se **elimina** el `CorrelationIdFilter` del chat, y los escritores de error (`GatewayErrorWriter`, `GlobalExceptionHandler`, `RestAccessErrorHandler`) leen `X-Request-Id`.
4. **Origin:** se adopta el `OriginHeaderFilter` del chat (retira `Origin` salvo en el handshake WS, que valida origen) en vez del `RemoveRequestHeader=Origin`. Cubre parcialmente P2.2.
5. **ACL / compose / env:** fusionados ambos usuarios ACL (`svc-gateway-coord` + `chat`), ambos servicios y contraseñas. `render-acl.sh` del chat ya corrige el `chmod` 640→644 (el contenedor lee el ACL como uid 999).
6. **Monolito:** `banUser`/`deleteUser` ahora hacen **ambos** `sessionRevocationService.revokeAllSessions(...)` (nuestro) **y** `chatEventPublisher.user{Suspended,Deleted}(...)` (chat). Coexisten sin conflicto.
7. **Alineación OK:** el chat es resource server ES256/JWKS (sus docs ya se habían alineado), consistente con la Fase 4.

**Decisiones aplicadas:** (1) WS → `permitAll` en el gateway (aprobado); (2) JWKS **NO** se expone en el gateway (P3.3 se mantiene) → el chat lo lee del monolito. Falta: build + tests del merge completo y commit.

## Fase 5: Identidad de máquina (servicio a servicio)

**Objetivo:** que las llamadas entre servicios sin un usuario detrás (el monolito llamando a notif, jobs, procesos internos) se autentiquen con tokens de servicio, sin endpoints internos abiertos.

**Prerrequisitos:** Fases 1 y 4 completas.

### Pendientes bloqueantes

- [ ] **P5.1. Inventario de llamadas servicio a servicio actuales:** quién llama a quién, con qué endpoint y cómo se autentica hoy.
  Decisión: _(nota)_ hoy la superficie síncrona es **pequeña**: el monolito→notif ya es **asíncrono** (outbox transaccional → RabbitMQ, no llamada REST). La mayoría del tráfico interno es gateway→servicios. **Pero** esta superficie **crecerá** con la separación de auth (el gateway/servicios consultando a identidad, `client_credentials`, el chat, etc.), así que la Fase 5 se mantiene como inversión a futuro aunque hoy toque poco.
- [ ] **P5.2. Formato del token de servicio.** Propuesta: mismo emisor y misma clave, `sub = "service:<nombre>"`, claim `scope` (por ejemplo `notifications:send`), sin `roles` de usuario y con vida corta.
  Decisión:
- [ ] **P5.3. Autenticación del cliente ante identidad:** `client_secret` (más simple) o `private_key_jwt` (más seguro). Dónde se guardan los secretos (igual que P1.1).
  Decisión:
- [ ] **P5.4. Llamadas internas:** ¿pasan por el gateway o van directas por la red interna? Si van directas, el servicio receptor valida el token (ya lo hace con la Fase 4).
  Decisión:
- [ ] **P5.5. Mensajería asíncrona** (si existe o se introduce en la Fase 8): el productor incluye `userId` en el mensaje y el broker se protege con sus propias credenciales. No se reenvían JWT en los mensajes.
  Decisión:
- [ ] **P5.6. mTLS interno gateway↔servicios (decisión #14, a preservar).** Como el rol-por-ruta vive solo en el gateway y los servicios ya no verifican roles, el aislamiento de red es un invariante duro (P3.8). mTLS interno es el control compensatorio: autentica mutuamente gateway y servicios y cifra el tráfico interno, de modo que un atacante en la red interna no pueda saltarse el gateway. Definir CA interna, emisión/rotación de certificados y punto de terminación (sidecar/malla o nativo de Spring). *No bloquea las Fases 1–4; es endurecimiento posterior.*
  Decisión:

### Tareas

- [ ] T5.1. Endpoint `POST /oauth/token` con `grant_type=client_credentials` en el módulo de identidad (registro de clientes en base de datos o configuración, con el secreto hasheado).
- [ ] T5.2. En los servicios llamadores: cliente HTTP que obtiene y cachea el token de servicio, y lo renueva antes de que expire (Spring `OAuth2AuthorizedClientManager` con `client_credentials`).
- [ ] T5.3. En los receptores: reglas que exigen el scope correspondiente en los endpoints internos (`hasAuthority("SCOPE_notifications:send")`).
- [ ] T5.4. Asegurar que un token de **usuario** no puede llamar a endpoints de servicio, y viceversa.

### Criterios de aceptación

- [ ] Cada llamada de P5.1 funciona con un token de servicio.
- [ ] Test: un endpoint interno sin token, o con token de usuario, responde 401/403.
- [ ] No queda ningún endpoint "interno" sin autenticación.

### Notas de implementación
_(completar durante la implementación)_

---

## Fase 6: Observabilidad, frontend y pruebas end-to-end

**Objetivo:** tener todo lo necesario para operar y verificar el sistema antes del corte.

**Prerrequisitos:** Fases 1 a 5 completas en staging.

### Pendientes bloqueantes

- [x] **P6.1. Dónde se guarda la auditoría de seguridad.**
  Decisión (2026-11-09): **tabla propia `security_audit`** en el monolito **+ logs estructurados JSON** con `correlationId` (recogibles luego por un colector). Ligero y suficiente pre-prod; SIEM y política de retención formal se definen en prod.
- [x] **P6.2. Herramienta de métricas y alertas** y umbrales.
  Decisión (2026-11-09): **Micrometer → Prometheus** (actuator `/prometheus`) + **Grafana**. Umbrales: tasa de 401/403 por código, fallos de Redis (breaker `redis-coord` abierto), errores al obtener el JWKS, reusos de refresh, latencia del chequeo `min_iat`. (El gateway ya emite `gateway.min_iat.check` y `gateway.ratelimit.decision`.)
- [x] **P6.3. Responsable del cambio en el frontend.**
  Decisión (2026-11-09): el propio equipo (quien lleva esta migración). El slice de frontend se aborda **primero** en la Fase 6.

### Tareas

**Auditoría y observabilidad**
- [x] T6.1. Eventos de auditoría con userId, IP, user agent, fecha y resultado. **(6-obs-B)** Tabla `security_audit` (`ddl-auto`) + `SecurityAuditService` (mejor esfuerzo, no rompe el negocio) + adaptador `REQUIRES_NEW` (el evento se persiste aunque la operación de negocio lance). IP/UA/correlationId del contexto de la petición (`AuditContextFilter`). **Cableado** en: `LOGIN_SUCCESS`/`LOGIN_FAILURE`, `REFRESH`, **`REFRESH_REUSE_DETECTED`**, `LOGOUT`, `LOGOUT_ALL`, `BAN`, `ROLE_REVOKE`, `PASSWORD_CHANGE`, `PASSWORD_RESET`, `ACCOUNT_DELETE`. No se registran tokens ni contraseñas (regla #5). (`jti` en el evento: pendiente menor.)
- [~] T6.2. Métricas: tasa de 401 y 403 por código, latencia del chequeo `min_iat`, errores de Redis, errores al obtener el JWKS, refresh por minuto y reusos detectados. **(6-obs-A parcial)** Endpoint **Prometheus** (`/actuator/prometheus`) en los 3 servicios Java, en un **puerto de gestión separado 9090 NO publicado** (solo red interna; no filtra métricas por el borde). **Tasa de 401/403/429 por código** vía `gateway.auth.error{status,code}` (en `GatewayErrorWriter`). **Errores de Redis** ya cubiertos (`gateway.min_iat.check{result=error|breaker-open}`, `gateway.ratelimit.decision`). **Pendiente (6-obs-B):** latencia de `min_iat`, errores de JWKS, refresh/minuto y reusos detectados (van con la tabla de auditoría, en las mismas rutas del monolito).
- [ ] T6.3. Alertas según P6.2.
- [ ] T6.4. El correlation ID aparece en los logs de todos los servicios.

**Frontend** — **(slice 6-frontend hecho 2026-11-09; `api.js` + sidebars; lint/build en verde)**
- [x] T6.5. Interceptor HTTP: ante un 401, refrescar **una sola vez** y reintentar. Si el refresh falla, ir al login. **(`doRequest` con `isRetry`: 401 autenticado → `tryRefresh()` → reintento; si falla → `clearToken`+`/login`.)**
- [x] T6.6. **Refresh único compartido:** si varias peticiones reciben 401 a la vez, todas esperan la misma promesa de refresh. **(`refreshPromise` a nivel de módulo, single-flight.)**
- [x] T6.7. Distinguir 401 (refrescar) de 403 (mostrar "sin permiso"). **(El 403 cae al error genérico con `code=FORBIDDEN`; no refresca ni desloguea.)**
- [x] T6.8. Manejo del refresh según P2.1 (cookie automática). **(`credentials:'include'` en `fetch`: recibe la cookie en login/register y la envía en `/auth/refresh` y `/auth/logout`.) Además: `authApi.logout()` (revoca la familia + limpia cookie) cableado en las dos sidebars.**

**Pruebas end-to-end (en staging)**
- [ ] T6.9. Login → navegación → expiración del access → refresh transparente.
- [ ] T6.10. Banear un usuario con sesión activa → su siguiente petición devuelve 401 y el refresh falla.
- [ ] T6.11. Quitar un rol → 401 inmediato y, tras el refresh, el rol ya no está.
- [ ] T6.12. acquireRole → acceso inmediato al nuevo rol.
- [ ] T6.13. Reuso de refresh → sesión revocada y alerta disparada.
- [ ] T6.14. Rotación de clave completa (P1.2) sin errores para los usuarios.
- [ ] T6.15. Redis caído → comportamiento según P3.1.
- [ ] T6.16. Llamadas servicio a servicio de P5.1.
- [ ] T6.17. Prueba de carga básica del gateway con el chequeo de `min_iat` activo.

**Contract tests (M24 — red de seguridad de la migración)**

> ESTADO §6.1/M24(e) pide contract tests entre el gateway y cada servicio y sobre los esquemas de evento. Esta migración crea justo esa costura (el gateway valida y enruta; los servicios asumen un contrato de token y de headers), así que el contrato se fija como **artefacto versionado**, no solo como tests ad-hoc.

- [ ] T6.18. Elegir herramienta (**Spring Cloud Contract** para el lado Java; **Pact** si se quiere incluir al chat en Python) y dejarla en el `pom`/CI.
- [ ] T6.19. **Contrato del token y de los headers del borde** como artefacto compartido: claims obligatorios del access token (contrato 4.1), `typ: at+jwt`, `alg: ES256`, y los headers que el gateway garantiza/inyecta (`Authorization` intacto, `X-Correlation-Id`). Un test por servicio verifica que rechaza un token que viole el contrato (alg != ES256, `aud` incorrecto, firma inválida) y acepta uno válido.
- [ ] T6.20. **Contrato del JWKS**: el emisor publica un JWKS con el shape esperado (ES256, `kid`, sin parámetro privado `d`); los verificadores (gateway, monolito, notif, chat) tienen un test que consume ese contrato.
- [ ] T6.21. **Contrato de error 4.4**: gateway y servicios responden el mismo JSON de error y los mismos códigos (`TOKEN_EXPIRED`, `TOKEN_REVOKED`, `FORBIDDEN`, `RATE_LIMITED`, …) para cada caso.
- [ ] T6.22. Integrar los contract tests en CI como gate (fallan el build si un productor rompe el contrato de un consumidor).

### Criterios de aceptación

- [ ] Todos los escenarios T6.9 a T6.17 pasan en staging.
- [ ] Los dashboards y alertas están activos en staging.
- [ ] Los contract tests (T6.18–T6.22) pasan y están integrados en CI como gate; romper el contrato del token, del JWKS o del error falla el build.

### Notas de implementación

- **Prerrequisito matizado (2026-11-09):** aunque el plan pide "Fases 1–5", **la mayor parte de la Fase 6 NO depende de la Fase 5** (identidad de máquina). Frontend (T6.5–T6.8), observabilidad (T6.1–T6.4) y contract tests (T6.18–T6.22) dependen solo de Fases 1–4 (hechas). Solo el e2e en staging (T6.9–T6.17, en especial T6.16 servicio-a-servicio) necesita Fase 5 + un entorno de staging desplegado. **Orden acordado:** (1) frontend → (2) observabilidad → (3) contract tests → (4) e2e cuando haya staging.
- **El chat entra en la Fase 6:** ya es parte del sistema, así que los contract tests (T6.19–T6.21: token/JWKS/error) y el e2e incluyen su auth (REST Bearer + `min_iat`; handshake WS por ticket). Su validación JWT ya está completa (`JwtDecoderConfig`: ES256+`typ`+`iss`+`aud`+timestamps, verificado).

**Slice 6-frontend (arranque, acordado — depende solo de Fases 1–4):**
Estado actual (`frontend/serviya/src/shared/api.js`): token en `localStorage`; en **401** hace `clearToken()`+redirect a `/login`; **no** hay refresh y **no** usa `credentials:'include'` (la cookie HttpOnly de refresh ni se envía ni se recibe). Ya lee `code` de los errores (para el chat) → compatible con el `code` 4.4 que ahora mandan monolito y notif.
- **T6.8 (cookie):** `fetch` con `credentials:'include'` en login/register (para **recibir** el Set-Cookie del refresh) y en `/auth/refresh` y `/auth/logout` (para **enviarla**). La cookie es `SameSite=Strict; Path=/api/v1/auth`; en dev `localhost:5173→:8080` es same-site, así que viaja.
- **T6.5 (interceptor):** ante un **401** en petición autenticada, en vez de desloguear: llamar **una vez** a `POST /api/v1/auth/refresh`; si devuelve un access nuevo → `saveToken` + **reintentar** la petición original; si el refresh falla → `clearToken` + ir a `/login`.
- **T6.6 (refresh único compartido):** una `refreshPromise` a nivel de módulo; si hay un refresh en vuelo, los 401 concurrentes esperan la MISMA promesa (evita tormenta de refresh y rotaciones en carrera).
- **T6.7 (401 vs 403):** el **403** NO refresca ni desloguea (ya es el comportamiento: cae a error normal con `code=FORBIDDEN`); mostrar "sin permiso". Solo el **401** dispara el refresh.
- **logout:** `POST /api/v1/auth/logout` (envía la cookie; el backend revoca la familia y limpia la cookie) + `clearToken()` local. Opcional: `logout-all` → `POST /api/v1/users/.../logout-all`? (en realidad `/api/v1/auth/logout-all`, autenticado).
- **Verificación:** manual en el navegador (login → esperar expiración del access 15m o forzar un 401 → ver el refresh transparente; banear al usuario → el siguiente refresh falla → login).

**Observabilidad en sub-slices (acordado 2026-11-09, enfoque ligero P6.1/P6.2):**
- **6-obs-A hecho 2026-11-09** (gateway 47 · notif 30 tests): endpoint Prometheus en los 3 servicios Java (puerto de gestión 9090 no publicado) + contador `gateway.auth.error{status,code}` (tasa 401/403/429). Dep `micrometer-registry-prometheus` en notif y gateway (el monolito ya la tenía).
- **6-obs-B hecho 2026-11-09** (monolito **491 tests**, ArchUnit en verde): tabla `security_audit` + `SecurityAuditService` + adaptador (`REQUIRES_NEW`) con la métrica `security.audit.event{type,result}` (de aquí salen "refresh/minuto" = eventos `REFRESH` y "reusos detectados" = `REFRESH_REUSE_DETECTED`, cerrando esa parte de T6.2). Cableado en los 11 eventos de auth. Contexto IP/UA/correlationId vía `AuditContextFilter`. Todo en `shared/audit` (como `shared/ratelimit`). Tests: `SecurityAuditServiceTest` + verify del reuso en `RefreshTokenServiceTest`; 4 tests `@InjectMocks` ganaron el mock.
  - **✅ Bug de rollback ARREGLADO 2026-11-09:** `RefreshTokenService.refresh` lleva ahora `@Transactional(noRollbackFor = UnauthorizedException.class)`. Antes, los caminos que **revocan la familia y luego lanzan** (reuso detectado, cuenta baneada/eliminada) revertían la revocación al propagarse la `UnauthorizedException` → la familia robada no quedaba revocada. Con `noRollbackFor` la revocación se **confirma**; los throws que no tocan BD (token ausente/inválido/caducado) confirman una tx vacía. *(Pendiente menor: un test de integración transaccional lo reforzaría; la correctness la da la semántica de la anotación.)*
- **6-obs-C (siguiente):** latencia de `min_iat` + errores de JWKS en el gateway; reglas de alerta Prometheus (T6.3) + correlation-id en los logs (T6.4). Grafana/dashboards y despliegue del stack van con staging (Fase 7).

---

## Fase 7: Corte big-bang

**Objetivo:** pasar producción al nuevo esquema en un solo despliegue coordinado. **No incluye la extracción de identidad (Fase 8).**

**Prerrequisitos:** Fases 1 a 6 completas y verificadas en staging.

### Pendientes bloqueantes

- [ ] **P7.1. Fecha y ventana del corte:** fuera de horas y días de alto tráfico del marketplace.
  Decisión:
- [ ] **P7.2. Comunicación a usuarios:** aviso previo de que tendrán que volver a iniciar sesión.
  Decisión:
- [ ] **P7.3. Plan de reversión escrito y probado en staging:** cómo volver a HS256 y a las versiones anteriores, con un tiempo objetivo de reversión.
  Decisión:
- [ ] **P7.4. Criterios de go/no-go y de reversión:** por ejemplo, tasa de 401 o de errores 5xx por encima de un umbral durante N minutos.
  Decisión:
- [ ] **P7.5. Claves de producción generadas y cargadas** según P1.1, y verificadas.
  Decisión:

### Tareas: orden de despliegue

- [ ] T7.1. Desplegar el **frontend** con soporte de refresh (tolerante a la ausencia del refresh token).
- [ ] T7.2. Desplegar el **monolito** (emisor ES256, JWKS, refresh, `min_iat`, resource server). Verificar que `/.well-known/jwks.json` responde.
- [ ] T7.3. Desplegar **notif** (resource server).
- [ ] T7.4. Desplegar el **gateway**.
- [ ] T7.5. Pruebas de humo en producción: login, refresh, una ruta por rol, una llamada servicio a servicio.
- [ ] T7.6. Monitorear durante la ventana según P7.4.
- [ ] T7.7. Retirar `JWT_SECRET` de todos los entornos tras el periodo de estabilización.

### Criterios de aceptación

- [ ] Las pruebas de humo pasan.
- [ ] Las métricas están dentro de los umbrales de P7.4 durante la ventana.
- [ ] `JWT_SECRET` ya no existe en ningún entorno.

### Notas de implementación
_(completar durante la implementación)_

---

## Fase 8: Extracción del servicio de identidad

**Objetivo:** mover el módulo `users` completo (cuentas, credenciales, roles, baneos, emisión, refresh, JWKS, `client_credentials`, reset de contraseña y gestión de usuarios para administradores) a un servicio de identidad independiente con su propia base de datos.

**Prerrequisitos:** Fase 7 estable en producción. **Release separado.**

### Pendientes bloqueantes

- [ ] **P8.1. Efectos de dominio del baneo.** ¿Qué pasa con las publicaciones activas, los pedidos en curso y los pagos pendientes de un vendedor baneado? ¿Y de un comprador?
  Decisión:
- [ ] **P8.2. Orquestación de `acquireRole`.** Si convertirse en `SELLER` requiere onboarding del marketplace (datos de tienda, verificación, términos), ¿quién orquesta? Propuesta: el monolito valida el onboarding y luego llama a identidad para otorgar el rol (con token de servicio). Definir la compensación si falla a mitad de camino.
  Decisión:
- [ ] **P8.3. Broker de mensajería** para eventos (Kafka, RabbitMQ u otro) y garantías de entrega.
  Decisión:
- [ ] **P8.4. Esquemas de eventos** del contrato 4.5: campos, versionado y política de compatibilidad.
  Decisión:
- [ ] **P8.5. Contenido de la réplica de lectura** de usuarios en el monolito (por ejemplo, `userId`, nombre visible, estado, roles). Lo mínimo necesario.
  Decisión:
- [ ] **P8.6. Estrategia de migración de datos:** cómo se mueven usuarios, credenciales, roles y refresh tokens a la nueva base de datos, y cómo se hace el corte (ventana, sincronización previa, verificación).
  Decisión:
- [ ] **P8.7. Política de eliminación de cuenta (Ley 1581 de 2012, Habeas Data):** ¿qué se borra, qué se anonimiza y qué se conserva (por ejemplo, pedidos por obligaciones contables)? ¿Cómo se propaga a las réplicas?
  Decisión:
- [ ] **P8.8. ¿Mantener el emisor propio o migrar a Keycloak?** Si se elige Keycloak, esta fase se rediseña.
  Decisión:

### Tareas

**Servicio de identidad**
- [ ] T8.1. Crear el servicio moviendo las capas hexagonales del módulo `users` (aplicación, puertos y adaptadores) con cambios mínimos.
- [ ] T8.2. Base de datos propia de identidad y migración según P8.6.
- [ ] T8.3. Mover `JwtService` (emisor), JWKS, refresh, `min_iat` (escritor), `client_credentials`, `PasswordResetTokenService` y la gestión de usuarios para administradores.
- [ ] T8.4. **Mantener el mismo `iss`** (URL lógica de P1.3) y la misma clave de firma, o rotarla con el procedimiento de P1.2.
- [ ] T8.5. Patrón **outbox** para publicar los eventos de 4.5 de forma confiable.
- [ ] T8.6. Envío de emails (reset de contraseña, bienvenida): identidad publica un evento y notif envía.

**Monolito**
- [ ] T8.7. Eliminar las FK hacia la tabla de usuarios. `user_id`, `seller_id` y `buyer_id` pasan a ser referencias lógicas.
- [ ] T8.8. Réplica de lectura según P8.5, alimentada por eventos, con consumidores idempotentes.
- [ ] T8.9. Reacciones de dominio según P8.1 (por ejemplo, ante `UserBanned`, despublicar productos).
- [ ] T8.10. Flujo de `acquireRole` según P8.2.
- [ ] T8.11. Eliminar el módulo `users` del monolito.

**Gateway e infraestructura**
- [ ] T8.12. Enrutar `/auth/**`, `/oauth/token` y `/.well-known/jwks.json` al nuevo servicio.
- [ ] T8.13. Actualizar `jwk-set-uri` en el gateway y los servicios si cambia la URL interna.
- [ ] T8.14. Actualizar los clientes de `client_credentials` para que apunten al nuevo servicio.

### Criterios de aceptación

- [ ] Las pruebas E2E de la Fase 6 pasan contra el nuevo servicio.
- [ ] El monolito no tiene acceso a la base de datos de identidad ni código del módulo `users`.
- [ ] Los eventos se entregan aunque el broker falle temporalmente (prueba de outbox).
- [ ] La réplica de lectura del monolito es consistente tras un reprocesamiento completo de eventos.
- [ ] El flujo de eliminación de cuenta cumple P8.7.

### Notas de implementación
_(completar durante la implementación)_

---

## 14. Registro consolidado de pendientes

| ID | Fase | Pendiente | Tipo | Estado |
|---|---|---|---|---|
| P1.1 | 1 | Almacenamiento de la clave privada | Técnico/operación | **Cerrado: `.env`+compose** (gestor de prod = deuda) |
| P1.2 | 1 | Procedimiento de rotación de claves | Operación | **Cerrado** (JWKS multi-clave; periodicidad al montar prod) |
| P1.3 | 1 | Valor de `iss` | Técnico | **Cerrado: `https://auth.serviya.local`** |
| P1.4 | 1 | Valor de `aud` | Técnico | **Cerrado: `marketplace-api`** |
| P1.5 | 1 | Vida del access token | Técnico | **Cerrado: 15m** (`JWT_ACCESS_TTL`) |
| P1.6 | 1 | Usar `typ: at+jwt` | Técnico | **Cerrado: Sí** |
| P1.7 | 1 | Versión del JDK (CVE-2022-21449) | Técnico | **Cerrado:** temurin:21, a salvo |
| P1.8 | 1 | Coordinación con el equipo de chat (HS256→ES256) | Organización | **Cerrado** (docs del chat actualizados + aviso) |
| P2.1 | 2 | Almacenamiento del refresh en el cliente | Técnico/seguridad | Abierto |
| P2.2 | 2 | Protección CSRF | Seguridad | Abierto |
| P2.3 | 2 | Vida del refresh y expiración absoluta | Técnico/negocio | Abierto |
| P2.4 | 2 | Período de gracia ante reuso | Técnico | Abierto |
| P2.5 | 2 | Eventos que escriben `min_iat` | Técnico/negocio | Abierto |
| P2.6 | 2 | Infraestructura de Redis | Operación | Abierto |
| P2.7 | 2 | Bloqueo por intentos fallidos | Seguridad/negocio | Abierto |
| P3.1 | 3 | Fail-open / fail-closed ante Redis caído | **Negocio** | Recomendado **fail-open** + TTL corto (alineado D-07); confirmar |
| P3.2 | 3 | Variante de SCG (WebFlux / WebMVC) | Técnico | **Cerrado: WebFlux** (ya en uso) |
| P3.3 | 3 | Rutas públicas | Técnico/negocio | Abierto |
| P3.4 | 3 | Matriz de rol por ruta | Técnico | Abierto |
| P3.5 | 3 | Catálogo de códigos de error | Técnico | Abierto |
| P3.6 | 3 | Orígenes CORS | Técnico | Abierto |
| P3.7 | 3 | Sistema de rate limiting (reutilizar vs SCG; mover techo al gateway) | Técnico/seguridad | **Cerrado:** reutilizar `RedisRateLimiter` del proyecto; techo global al gateway; `@RateLimit` se queda en servicios |
| P3.8 | 3 | Exposición de red de los servicios | Operación/seguridad | Abierto |
| P4.1 | 4 | `min_iat` en los servicios | Técnico | **Cerrado: No** (solo el gateway) |
| P4.2 | 4 | Reglas de rol en los servicios | Técnico/seguridad | **Cerrado: No** (rol solo en el gateway; servicios solo dominio; compensa mTLS P5.6) |
| P4.3 | 4 | Inventario de reglas de dominio | Técnico | Abierto |
| P5.1 | 5 | Inventario de llamadas servicio a servicio | Técnico | Abierto |
| P5.2 | 5 | Formato del token de servicio | Técnico | Abierto |
| P5.3 | 5 | Autenticación de clientes de servicio | Seguridad | Abierto |
| P5.4 | 5 | Ruta de las llamadas internas | Técnico | Abierto |
| P5.5 | 5 | Mensajería asíncrona | Técnico | Abierto |
| P5.6 | 5 | mTLS interno gateway↔servicios (compensa la quita de rol en servicios) | Seguridad/infra | Abierto (endurecimiento posterior) |
| P6.1 | 6 | Almacenamiento de la auditoría | Operación | Abierto |
| P6.2 | 6 | Métricas y alertas | Operación | Abierto |
| P6.3 | 6 | Responsable y fecha del frontend | Organización | Abierto |
| P7.1 | 7 | Fecha y ventana del corte | **Negocio** | Abierto |
| P7.2 | 7 | Comunicación a usuarios | **Negocio** | Abierto |
| P7.3 | 7 | Plan de reversión | Operación | Abierto |
| P7.4 | 7 | Criterios go/no-go | Operación | Abierto |
| P7.5 | 7 | Claves de producción | Operación | Abierto |
| P8.1 | 8 | Efectos de dominio del baneo | **Negocio** | Abierto |
| P8.2 | 8 | Orquestación de `acquireRole` | Técnico/negocio | Abierto |
| P8.3 | 8 | Broker de mensajería | Técnico | Abierto |
| P8.4 | 8 | Esquemas de eventos | Técnico | Abierto |
| P8.5 | 8 | Contenido de la réplica de usuarios | Técnico | Abierto |
| P8.6 | 8 | Migración de datos | Técnico/operación | Abierto |
| P8.7 | 8 | Eliminación de cuenta (Ley 1581) | **Negocio/legal** | Abierto |
| P8.8 | 8 | Emisor propio vs. Keycloak | Técnico | Abierto |
