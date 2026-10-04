# ServiYa — Estado actual y plan futuro

> **Documento canónico** del estado del sistema y del plan de evolución (incluida la
> migración a microservicios). Centraliza lo que antes estaba disperso en
> `documents/project-structure/*`, la memoria del asistente y notas sueltas.
>
> - **Iniciado:** 2026-10-03 (sesión de diagnóstico, sin tocar código).
> - **Estado del documento:** EN CONSTRUCCIÓN. Backend diagnosticado (9/9 módulos).
>   Pendiente: frontend, auditoría de documentación/memoria, y el plan de migración.
> - **Convención:** severidades `[CRÍTICO] [ALTO] [MEDIO] [BAJO] [INFO]`. Las referencias
>   `archivo:línea` son del commit en curso; verificar antes de actuar.
> - **Relación con otros archivos:** `CLAUDE.md` = guía estable de arranque (se reescribirá
>   apuntando aquí). `NOTAS.txt` = log histórico (se mantiene). Este doc = foto viva + plan.

---

## 0. Cómo leer este documento

1. **§1 Resumen ejecutivo** — lo esencial en una página.
2. **§2 Estado actual** — qué hay y cómo está construido (por módulo).
3. **§3 Hallazgos transversales** — los ejes que cruzan todo el sistema.
4. **§4 Bugs** — catálogo accionable con severidad.
5. **§5 Funcionalidades faltantes**.
6. **§6 Mejoras y replanteamientos propuestos** — backlog de valor (se consolida al final).
7. **§7 Plan de migración a microservicios** — (pendiente del diagnóstico completo).
8. **§8 Decisiones abiertas** y **§9 Registro de sesión**.

---

## 1. Resumen ejecutivo

- **El backend está sustancialmente implementado y es de buena calidad** (dominio rico, hexagonal
  consistente, máquina de estados sólida, métricas event-driven), en contra de lo que afirmaba la
  versión previa de `CLAUDE.md` ("placeholders que lanzan `UnsupportedOperationException`"). Cifras:
  **569 clases Java** de producción + **~34 de test**; 9 módulos de negocio. El **frontend** también
  está construido (~30 páginas, React 19), no es un starter.
- **La arquitectura es un monolito modular** con fronteras hexagonales limpias pero **acoplamiento
  fuerte entre módulos**: llamadas síncronas, a veces a **puertos de persistencia ajenos**, y
  **transacciones que cruzan módulos**. Clúster duro `profiles↔services↔{metrics,feedback}`. Ese
  acoplamiento es el eje (y el principal obstáculo) del plan de microservicios.
- **Riesgo #1 — autorización**: el modelo es solo URL-matchers con `anyRequest().permitAll()` por
  defecto y **cero `@PreAuthorize`**. Hay **IDOR explotables, varios sin autenticar**, en escrituras de
  `services`, `addresses` y `categories` (§3.1/§4). Es el hallazgo más grave.
- **Riesgo #2 — concurrencia**: **sin `@Version` en ninguna entidad**; métricas con lost-update;
  propuestas de reprogramación duplicables por carrera; `@Scheduled` sin lock distribuido (bloquea el
  escalado horizontal).
- **Riesgo #3 — pruebas**: cobertura de **integración casi nula** (1 `@SpringBootTest` de contexto, 1
  controller probado de ~15); **`feedback` y `metrics` con 0 tests**. Nada detectaría las regresiones de
  los dos riesgos anteriores (§3.7).
- **Deuda de documentación**: doc-rot sistémico (docs de diseño y javadocs que describen placeholders
  ya implementados); consolidada y corregida en esta sesión (CLAUDE.md reescrito; este documento es la
  fuente de verdad).
- **Dirección de la nueva fase (requisito de entrega)**: migrar a **microservicios con un API gateway
  que se comunica con todos los servicios**. El plan (§7) es **Strangler Fig** sobre un monolito
  **endurecido primero** (Fase 0: autorización, concurrencia, outbox, sagas, storage externo, pruebas),
  decomposición de **grano grueso** (el clúster `catalog`=`profiles`+`services` va como una unidad) y
  extracción de hoja a núcleo (`notifications`→`metrics`→…). La Fase 0 no es opcional ni "en vez de": es
  lo que evita terminar en un **monolito distribuido**.
- **Prioridades inmediatas recomendadas** (todas dentro de la Fase 0, valen migre o no): cerrar la clase
  de IDOR (M1), bloqueo optimista + contadores atómicos (M2), estrategia de pruebas como red de
  seguridad (M24), y los route guards del frontend (M16/F1).

---

## 2. Estado actual del sistema

### 2.1 Visión general

- **Monorepo** con tres piezas (`docker-compose.yml`): `backend/serviya` (Spring Boot 3.5.5,
  Java 21), `frontend/serviya` (React 19 + Vite, sin TS), `mysql/` (bootstrap SQL).
- **Puertos**: backend `8080`, frontend dev `5173` / prod (Nginx) `8081`, MySQL `3307→3306`.
- **Base de datos**: MySQL 8.4. Esquema por `mysql/create_database.sql` (solo en volumen limpio)
  **+** `ddl-auto: update` (Hibernate reconcilia en arranque). **Dos mecanismos de esquema**
  conviviendo.

### 2.2 Backend — configuración y capas transversales

**Stack confirmado** (`pom.xml`): Spring Boot 3.5.5, Spring Security, Data JPA, Validation, Web;
MySQL connector; JWT (jjwt 0.12.6); MapStruct 1.5.5 + Lombok (processor order configurado);
springdoc OpenAPI 2.8.9; java-dotenv; **Bucket4j** (rate limiting); devtools.

- **[INFO] CLAUDE.md desactualizado: Flyway NO está en el `pom.xml`.** CLAUDE.md afirma "Flyway
  está en el classpath"; es falso. No hay migraciones.
- **`application.yaml`**: `ddl-auto: update`; `show-sql`/logging web en `DEBUG`;
  `include-stacktrace: always` e `include-message: always`; paginación tope `max-page-size: 20`.
  Incluye config de word-filter (RNF-006), email (Brevo), password-reset (TTL + cron de limpieza),
  rate-limit, y `app.upload.dir`.
- **`shared/`** es ya un kernel transversal: `events/` (publisher + 8 eventos de dominio +
  `TagRef`), `security/` (`CurrentUser`, `PiiAttributeConverter`), `exceptions/` + 2 handlers
  globales (`GlobalExceptionHandler`, `RestAccessErrorHandler`), `textfilter/`, `StatisticsController`.
- **`SecurityConfig`**: JWT stateless, CORS, filtros (JWT + rate limiting en `/auth/**`).
  Autorización **solo por URL-matchers**, terminando en **`.anyRequest().permitAll()`**.
- **Seguridad transversal**:
  - **JWT HS256** (subject=userId, claim `roles`), secreto derivado por SHA-256.
  - **`JwtAuthenticationFilter` consulta la BD en CADA request autenticado** (`userReadPort.findById`
    para revalidar `isActive()`) → corta bans al instante pero cuesta una query por petición y acopla
    la auth de todo el sistema a la tabla `users`.
  - **PII AES-256-GCM** (`PiiAttributeConverter`) con IV aleatorio.

### 2.3 Backend — estado por módulo

> Orden del grafo de dependencias (de núcleo a orquestador). Para cada módulo: estado, lo bueno,
> y lo que va al catálogo de bugs/mejoras (§3–§6).

#### Módulo 1 — `users` (89 archivos)
- **Estado: implementado y maduro.** Auth (login/registro), recuperación de contraseña RF-003
  (token SHA-256, rate limiting Bucket4j, scheduler de limpieza), roles, consent, borrado orquestado.
- **Lo bueno**: dominio rico (`User`, `Role`, `PasswordResetToken`, `Consent`), orquestación segura
  (mensajes genéricos anti-enumeración, envío de correo diferido).
- **Hallazgos**: el `0L` placeholder de `currentUserId()` **ya no existe** (CLAUDE.md/memoria
  desactualizados); javadoc "placeholder" obsoleto en `UserService`/`RoleService` (tienen lógica real);
  acoplamiento directo a `profiles`, `services`, `requests`, `notifications` (ver §3.3).

#### Módulo 2 — `profiles` (76 archivos)
- **Estado: implementado**, con 2 placeholders reales y deuda de limpieza.
- **Hallazgos**: 🔴 **2 IDOR** (direcciones y slots de disponibilidad, §4); placeholders
  `AddressService.verifyAddress`/`getCoordinates` (geocodificación, el endpoint `/addresses/verify`
  devuelve 500); fotos en **disco local** (no apto microservicios); casi-ciclo `profiles↔services`
  resuelto partiendo en 2 beans; duplicidad de creación de perfil de oferente (llamada directa +
  evento); logging de depuración en `setSchedule`; `@Valid` desactivado en slots; excepciones
  inconsistentes (`IllegalArgumentException`→500).

#### Módulo 3 — `services` (57 archivos)
- **Estado: implementado** (CRUD, búsqueda por `Specification`, categorías, disponibilidad, fotos,
  detalle compuesto). Es el "template" hexagonal.
- **Hallazgos**: 🔴🔴 **IDOR masivo y sin autenticar** en todas las escrituras
  (`PATCH/DELETE /services/{id}`, activate/deactivate/soft-delete, `POST /categories`) — ver §3.1/§4;
  🔴 **el dominio importa dominio de otros módulos** (`ServiceDetail`→metrics/profiles,
  `FeedbackUser`→feedback/profiles); `MarketplaceService` es un hub (feedback+metrics+profiles);
  N+1 y doble lectura de métricas en `getDetailById`; fotos en disco local.

#### Módulo 4 — `requests` (56 archivos)
- **Estado: el más maduro.** Máquina de estados (8 estados, transiciones idempotentes, patrón
  Prototype para reprogramar), split CQRS (command/query/maintenance/proposals), eventos para
  métricas, **control de propiedad correcto** (`requireOwnership`/`requireParticipant`).
- **Hallazgos**: 🔴 propuestas PENDING duplicadas por carrera (sin índice único/lock); 🔴 sin
  `@Version` (lost-update en transiciones); 🔴 `@Scheduled` (4 tareas) **sin lock distribuido**
  (doble ejecución al escalar); acoplamiento profundo: lee **puertos de persistencia** de `services`
  y `profiles` (lee sus tablas conceptualmente); javadoc "reads placeholder" obsoleto.

#### Módulo 5 — `feedback` (62 archivos)
- **Estado: implementado y simétrico** (ServiceFeedback / ClientFeedback, rating+reseña+tags
  unificados, catálogo de tags por sentimiento).
- **Hallazgos**: 🗑️ **código muerto confirmado**: `FeedbackFlow` + `FeedbackFlowPort` (nadie los
  usa); doc-rot en los `*TagCatalogService`; N+1 + escaneo completo de catálogo por ítem en lecturas
  de lista; eventos **autocontenidos** hacia métricas (buen diseño).

#### Módulo 6 — `metrics` (67 archivos)
- **Estado: implementado, muy simétrico.** Lado de escritura = **puro consumidor de eventos**
  (8 `@TransactionalEventListener` AFTER_COMMIT); nadie lo llama para escribir.
- **Hallazgos**: 🔴 **lost-update confirmado** (read-modify-write sin `@Version` ni SQL atómico;
  `REQUIRES_NEW` agrava la carrera; `registerRating` recalcula media → especialmente frágil);
  🔴 fiabilidad: eventos AFTER_COMMIT **sin outbox/reintento/reconciliación** ni idempotencia
  (al migrar a broker con entrega ≥1, duplicaría contadores); el lado de **lectura NO es
  event-driven** (importa `requests` y `feedback`). Mitigación elegante: fila de métricas creada
  BEFORE_COMMIT al asignar rol.

#### Módulo 7 — `notifications` (40 archivos)
- **Estado: el mejor en fiabilidad.** INTERNAL en línea; EMAIL diferido (AFTER_COMMIT +
  REQUIRES_NEW); **reintentos con tope** y salto inteligente de tipos inservibles sin
  `protectedData`. Integra con **Brevo** (única llamada externa), adaptador que **nunca lanza**,
  `@ConditionalOnProperty`.
- **Hallazgos**: 🟠 IDOR leve en `markAsRead` (sin dueño + permitAll); `@Scheduled` de reintento sin
  lock distribuido; **`notify()` es el servicio más dependido** (lo llaman users/requests/feedback);
  depende de `users` para el correo del destinatario.

#### Módulo 8 — `reports` (68 archivos)
- **Estado: implementado y verificado.** Reporte polimórfico (base + 3 subtipos), `ReportActionType`
  como enum con comportamiento, trazabilidad de acciones, detalle resiliente a objetivos revertidos.
- **Hallazgos**: 🟠 `getReports` **carga tabla completa** y pagina en memoria (no escala); N+1 de
  nombres; **read-hub**: depende de los puertos de entrada de 5 módulos (profiles/requests/feedback/
  notifications/users). Separación correcta: transiciona estado + traza; los efectos de moderación
  viven en `admin`.

#### Módulo 9 — `admin` (24 archivos)
- **Estado: orquestador puro** (sin entidades ni persistencia). Gestión de usuarios/roles +
  `ModerationService`. Es el **ápice del grafo** (importa de los 8 módulos).
- **Hallazgos**: 🔴 **sin guard de auto-acción ni de "último admin"** (auto-ban/auto-delete →
  lockout si es el único admin); 🟠 `searchFeedback` carga todo con `Integer.MAX_VALUE` y pagina en
  memoria; 🔴 **violaciones de capa** (importa `infrastructure` de requests/metrics/users y puertos
  de **persistencia** de feedback); 🔴 **transacciones cross-módulo** (ban+resolve+notify;
  revoke+cascada) = bloqueante para microservicios. Conceptualmente es un **BFF/orquestador**.

### 2.4 Frontend

**Stack** (`package.json`): React 19 + Vite 8, **JSX sin TypeScript**, `react-router-dom` 7,
`react-hook-form`, `leaflet`/`react-leaflet`. **Sin axios** (fetch nativo), **sin librería de estado
global** (no Redux/Zustand/Context), **sin librería de UI** (CSS plano: `globals.css` + un `.css` por
componente). Cifras: **59 `.jsx`, 16 `.js`, 35 `.css`**; ~30 páginas.

**Estructura (espejo del backend)**: `src/modules/<capability>/{pages,components}` + `src/shared/`
(componentes, `hooks/`, `api.js`, `navConfig.js`, `index.js`) + `src/app/` (routing). Cada componente
en su carpeta con `.jsx`+`.css`; barrels `index.js` por módulo.

**Wiring**: `index.html` → `main.jsx` (monta `<RouterProvider>`, importa `globals.css`) →
`app/routes.jsx` (tabla URL→página con `createBrowserRouter`). `app/App.jsx` es un **pass-through**
(`return children`) — no hay layout/provider/contexto global a nivel de router.

**Capa de API (`shared/api.js`, 438 líneas)**: helper `request()` (headers JSON, `Authorization:
Bearer` cuando `auth:true`, lanza `Error` con el mensaje del backend en no-2xx); JWT en `localStorage`
(`serviya_token`), decodificado client-side con `atob` solo para UI (`isAuthenticated` = chequeo de
`exp` local, `rolesFromToken`, `userIdFromToken`); objetos por dominio (`authApi`, `profileApi`,
`serviceApi`, `requestApi`, `proposalApi`, `adminApi`, `reportApi`, `metricsApi`, `notificationApi`,
`feedbackApi`, `moderationApi`, …) ~1:1 con el backend. `API_BASE = VITE_API_URL ?? localhost:8080`.

**Estado real**: UI sustancialmente construida (~30 páginas, no es starter). Dashboards cliente/
oferente/admin cableados a API. Ver §3.6 para lo arquitectónico y §4/§5 para bugs/faltantes.

### 2.4.1 Consolidación de `estado-frontend-pendientes.md` (julio 2026)

> Ese archivo (no versionado, backlog personal de julio) queda **absorbido aquí** y debe borrarse.
> **Ya resuelto** en aquellas sesiones (no re-abrir): logout limpia token; iconos de categoría;
> avatares con iniciales + fallback (`Avatar`/`ServiceImage`); detalle de solicitud `/requests/:id`;
> endpoints booleanos de feedback-exists; AdminDashboard cableado a datos reales; detalle de usuario
> admin con métricas; crear usuario por admin; `OffererReschedulesPage`/`OffererRequestsPage`/
> `OffererServiceDetailPage`; recuperación de contraseña (`/recover` + `/reset-password`);
> "Mis reportes"; detalle de reporte admin; sidebar responsive (drawer); varios fixes de payloads.
> Lo que **sigue abierto** se traslada a §3.6 (arquitectura), §4 (bugs) y §5 (faltantes).

### 2.5 Base de datos / Docker / infra — _(ampliar)_
- Esquema en `mysql/create_database.sql` + seeds (`0x_*.sql`). `ddl-auto: update` convive.
- `docker-compose.yml` (prod-like) + `docker-compose.dev.yml` (hot-reload, volúmenes). Secretos por
  `.env`/`.env.example`.

---

## 3. Hallazgos transversales

### 3.1 Seguridad

- **[CRÍTICO] Autorización = solo URL-matchers + `anyRequest().permitAll()`; cero `@PreAuthorize`.**
  `@PreAuthorize`/`@Secured`/`@EnableMethodSecurity` **no se usan en todo el backend** (verificado).
  CLAUDE.md afirma lo contrario. Todo endpoint no listado explícitamente en `SecurityConfig` queda
  **abierto**. La "autorización fina en el servicio" que CLAUDE.md promete **solo se cumple en
  `requests` y `feedback`**; falta en `services`, `profiles`, `categories`, `notifications`.
- **[CRÍTICO] IDOR sin autenticar** (caen en `permitAll` y el servicio no verifica dueño):
  - `PATCH /api/v1/services/{id}`, `DELETE /api/v1/services/{id}` (hard delete),
    `POST /api/v1/services/{id}/activate|deactivate`, `PATCH /api/v1/services/{id}/soft-delete`.
  - `PATCH /api/v1/addresses/{id}`, `DELETE /api/v1/addresses/{id}`.
  - `POST /api/v1/categories` (debería ser solo-admin).
- **[ALTO] IDOR autenticado**: `DELETE|POST /api/v1/offerers/me/availability/slots/{id}`
  (delete/activate/deactivate) no verifican que el slot sea del oferente autenticado.
- **[BAJO] IDOR leve**: `POST /api/v1/notifications/{id}/read` (sin dueño + permitAll).
- **[ALTO] Secretos con defaults inseguros embebidos**: `JWT_SECRET` y `ENCRYPTION_KEY` tienen valor
  por defecto en el código; si falta la variable, la app arranca con claves públicas conocidas
  (firma JWT falsificable, PII descifrable).
- **[ALTO] Sin guard de auto-acción / último-admin** (`AdminService`/`ModerationService` reciben
  `adminId` pero no lo usan) → auto-ban/auto-delete → lockout si es el único admin.
- **[MEDIO] `include-stacktrace: always` + `include-message: always`** → fuga de stacktraces al
  cliente. Config de dev que no debe ir a prod.
- **[MEDIO] Roles del JWT no se revalidan por request** → revocar un rol no surte efecto hasta que
  expira el token (solo ban/delete cortan al instante).
- **[BAJO] `PiiAttributeConverter`**: fallback silencioso que devuelve bytes crudos ante fallo de
  descifrado (enmascara clave equivocada/corrupción); columnas cifradas **no consultables por valor**
  (IV aleatorio).
- **[BAJO] CORS hardcodeado** con IP personal y URL de ngrok concreta (no configurable por entorno).

### 3.2 Concurrencia y consistencia

- **[ALTO] Sin `@Version` (bloqueo optimista) en NINGUNA entidad.** Lost-update en métricas y en
  las transiciones de solicitudes bajo concurrencia.
- **[ALTO] Métricas con read-modify-write no atómico** (`metrics` §2.3/M6). `registerRating`
  (media acumulada) es el caso más frágil.
- **[ALTO] Propuestas de reprogramación PENDING duplicadas por carrera** (`createProposal` sin
  índice único parcial ni lock pesimista).
- **[ALTO] `@Scheduled` sin lock distribuido** (requests: 4 tareas; notifications: reintento) →
  **doble ejecución al escalar a >1 instancia**. Bloquea el escalado horizontal y la migración.
- **[MEDIO] Eventos in-process sin outbox/idempotencia** → al pasar a broker, riesgo de doble
  procesamiento (métricas infladas).

### 3.3 Acoplamiento y cohesión (grafo de dependencias)

**Clúster duro (bidireccional):** `profiles ↔ services ↔ {metrics, feedback}`.
- `services.MarketplaceService` → feedback + metrics + profiles.
- `profiles.OffererPublicProfileService` → services + metrics (casi-ciclo resuelto partiendo beans).
- `ServiceDetail`/`FeedbackUser` (dominio de `services`) importan dominio de metrics/profiles/feedback.

**Acoplamiento a puertos de SALIDA/persistencia ajenos** (más profundo que por puerto de entrada;
equivale a leer las tablas de otro módulo):
- `requests` → `services` (ServicePersistencePort, ServiceAvailabilityPersistencePort,
  CategoryPersistencePort) + `profiles` (UserProfilePersistencePort, AddressPersistencePort).
- `admin` → `feedback` (ServiceFeedbackPersistencePort, ClientFeedbackPersistencePort).

**Servicios transversales muy dependidos:** `notifications.notify()` (users/requests/feedback/...),
`users` (auth en cada request), `metrics` (lecturas síncronas de profiles/services/admin).

**Violaciones de la regla de dependencia** (la propia que CLAUDE.md declara):
- `admin` importa `infrastructure` (web mappers/DTOs) de `requests`, `metrics`, `users`.

**Transacciones que cruzan módulos** (bloqueante #1 para microservicios): `ModerationService.ban…`,
`AdminService.revokeRoleByAdmin`, `UserDeletionService.deleteUser`, `UserCreationService` —
escrituras multi-módulo en una sola `@Transactional` (válido con una BD, no con BDs separadas).

**Jerarquía para extracción** (de más fácil a más difícil de separar):
`notifications` / `metrics` (hojas/consumidores) → `feedback` → `requests` →
`profiles`+`services` (clúster, juntos) → `reports` → `admin` (último, es el orquestador).

### 3.4 Rendimiento / escalabilidad

- **[MEDIO] N+1 recurrente**: nombres de oferente (`ServiceController.search`), reseñas por autor
  (`getDetailById`), tags por feedback (`feedback`), nombres en reportes.
- **[MEDIO] Doble lectura de métricas** del oferente en `getDetailById`.
- **[MEDIO] Listados que cargan tablas completas y paginan en memoria**: `reports.getReports`,
  `admin.searchFeedback` (`Integer.MAX_VALUE`).
- **[MEDIO] Catálogo de tags escaneado completo por ítem** en lecturas de feedback.
- **[MEDIO] Query a BD por request autenticado** (revalidación `isActive()` en el filtro JWT).
- **[BAJO] Rate limiting en memoria** (Bucket4j local) → no compartido entre instancias.

### 3.5 Calidad / deuda / doc-rot / código muerto

- **[MEDIO] Doc-rot sistémico**: CLAUDE.md describe el backend como placeholders (falso);
  javadocs "placeholder/sin lógica aún" sobre clases ya implementadas (`UserService`, `RoleService`,
  `AddressService`, `*TagCatalogService`, `RescheduleProposalService` "reads placeholder");
  CLAUDE.md afirma `@PreAuthorize` y `0L` que no existen, y Flyway que no está.
- **[BAJO] Código muerto**: `FeedbackFlow`, `FeedbackFlowPort` (y revisar `FeedbackParts`).
- **[BAJO] Almacenamiento en disco local** de fotos (profiles + services) → estado no compartible.
- **[BAJO] Limpieza**: imports duplicados/muertos (`MarketplaceService`, `ServiceController`,
  `AddressService`), logging de depuración (`OffererAvailabilityService`), `@Valid` comentado,
  excepciones `IllegalArgumentException` donde corresponde 422/404.
- **[INFO] `UnauthorizedException` → 401** (el handler global mapea ownership a 401, no 403;
  contra lo que dice CLAUDE.md — documentar la decisión real).

### 3.6 Frontend — arquitectura y transversales

- **[ALTO] Sin route guards.** `routes.jsx` monta cada ruta como `page(<Componente/>)` sin protección.
  Verificado: **no existe `ProtectedRoute`, ni `useContext`, ni `loader` en todo `src`**. Cualquier URL
  (`/admin/*`, `/offerer/*`, `/dashboard`) se renderiza sin sesión ni rol; el backend rechaza las
  llamadas (401) pero la UI ya se pintó → errores de carga. Las piezas existen (`isAuthenticated`,
  `rolesFromToken`, `homePathForRoles`) pero el router no las usa. **Bug de frontend #1.**
  (Nota: con los IDOR de §3.1, para las escrituras de `services`/`addresses` el backend tampoco frena.)
- **[MEDIO] Sin interceptor de 401 en `request()`.** Un token expirado/ inválido produce un `Error`
  genérico por llamada; no hay `clearToken()` + redirección a `/login` centralizados → el usuario queda
  atascado con errores sueltos.
- **[MEDIO] Sin estado/sesión global.** No hay Context/Store: **9 páginas hacen `getMyProfile()` por su
  cuenta** y recalculan el rol → fetch repetido, avatar/nav pasados por props en cada página.
  `DashboardLayout` **recibe `nav`/`avatar` por prop** (no los deriva del rol) y hace polling de
  notificaciones cada 30 s con `setInterval` por instancia de layout.
- **[BAJO] Sin error boundaries / `errorElement`.** Un error de render deja la app en blanco (sin UI de
  error por ruta). `App.jsx` es un pass-through, no envuelve nada.
- **[BAJO] N+1 de nombres en el cliente**: `userApi.getDisplayName` se llama por fila (resuelve
  `/offerers/{id}/summary` uno a uno) en listados (reportes, servicios) → espejo del N+1 del backend.
- **[INFO] Autenticación client-side por `exp`**: `isAuthenticated` solo compara `exp` local; no detecta
  revocación (consistente con la limitación del backend, §3.1/B15).
- **[INFO] Duplicación de utilidades**: `timeAgo`/`TYPE_META` reimplementados en varias páginas
  (pendiente mover a `shared/`); paginación de notificaciones por botón-por-página (no escala).

### 3.7 Testing / cobertura de pruebas

- **[ALTO · DEUDA] Cobertura insuficiente y casi nula de integración.** ~34 clases de test frente a
  **569 de producción**, **predominantemente unitarias con mocks** (24 `@ExtendWith(MockitoExtension)`,
  117 `@Mock`). La integración está **prácticamente ausente**: **1 solo `@SpringBootTest`** (solo carga
  de contexto, `ServiyaApplicationTests`) y **1 solo `@WebMvcTest`** (`AccountControllerIntegrationTest`);
  un único archivo usa `MockMvc` → **solo 1 de ~15 controllers probado**.
- **[ALTO] Módulos con CERO tests: `feedback` (0) y `metrics` (0).** `metrics` es, además, donde vive
  el lost-update (§3.2) → **el bug de concurrencia no tiene ningún test que lo detecte**.
- **Sin tests de persistencia reales** (0 `@DataJpaTest`; los "adapter tests" mockean el repositorio)
  → nada valida mappers/queries contra una BD real.
- **Sin tests de autorización/endpoint** → los IDOR (B1–B4) y el `permitAll` por defecto no están
  cubiertos: ninguna prueba fallaría ante esas regresiones de seguridad.
- **Sin tests de concurrencia** → B7 (lost-update) y B8 (propuestas duplicadas) no se reproducen.
- **Sin tooling de cobertura** (no jacoco) ni fase de integración separada (no failsafe) en `pom.xml`.
- **Reparto por módulo**: users 8, services 9, requests 6, reports 3, profiles 2, notifications 2,
  shared 2, admin 1, **feedback 0, metrics 0**. (requests y services son los mejor cubiertos; el resto,
  parcial o nulo.)

---

## 4. Catálogo de bugs (accionable)

| # | Sev | Módulo | Bug | Dónde |
|---|-----|--------|-----|-------|
| B1 | CRÍTICO | services | Escrituras IDOR sin autenticar (update/delete/activate/deactivate/soft-delete) | `ServiceController` + `SecurityConfig` + `MarketplaceService` (sin check de dueño) |
| B2 | CRÍTICO | profiles | IDOR sin autenticar en `PATCH/DELETE /addresses/{id}` | `AddressController` + `SecurityConfig` |
| B3 | CRÍTICO | services | `POST /categories` sin autenticar (debe ser admin) | `CategoryController` + `SecurityConfig` |
| B4 | ALTO | profiles | IDOR autenticado en slots de disponibilidad (delete/activate/deactivate) | `OffererAvailabilityService` (sin check de dueño) |
| B5 | ALTO | admin | Sin guard auto-acción/último-admin → lockout | `AdminService`, `ModerationService` |
| B6 | ALTO | config | Secretos con defaults inseguros embebidos (JWT/PII) | `JwtService`, `PiiAttributeConverter` |
| B7 | ALTO | metrics | Lost-update por read-modify-write sin `@Version`/SQL atómico | `OffererMetricsService` et al. |
| B8 | ALTO | requests | Propuestas PENDING duplicadas por carrera | `RescheduleProposalService.createProposal` |
| B9 | ALTO | requests/notifications | `@Scheduled` sin lock distribuido (doble ejecución al escalar) | `RequestMaintenanceScheduler`, `NotificationRetryScheduler` |
| B10 | MEDIO | global | Sin `@Version` en ninguna entidad (consistencia) | todas las entidades |
| B11 | MEDIO | config | Stacktraces/mensajes expuestos al cliente | `application.yaml` |
| B12 | MEDIO | profiles | `/addresses/verify` devuelve 500 (placeholder) | `AddressService.verifyAddress/getCoordinates` |
| B13 | MEDIO | reports/admin | Listados cargan tabla completa y paginan en memoria | `ReportService.getReports`, `AdminService.searchFeedback` |
| B14 | BAJO | notifications | IDOR leve en `markAsRead` | `NotificationController` |
| B15 | BAJO | security | Roles del JWT no se revalidan por request | `JwtAuthenticationFilter` |

**Frontend:**

| # | Sev | Área | Bug | Dónde |
|---|-----|------|-----|-------|
| F1 | ALTO | routing | Sin route guards: toda ruta protegida se renderiza sin sesión/rol | `app/routes.jsx` |
| F2 | MEDIO | api | Sin interceptor de 401 (no hay logout+redirect centralizado) | `shared/api.js` `request()` |
| F3 | MEDIO | admin | `AdminServicesPage`: filtro de categoría por input numérico (no dropdown); IDs crudos en vez de nombres (`AdminFeedbackPage`/`AdminServicesPage`) | páginas admin |
| F4 | BAJO | routing | `RequestServicePage` = maqueta 100% (0 llamadas API) en ruta muerta `/request-service` | `RequestServicePage`, `routes.jsx:37` |
| F5 | BAJO | ux | Sin error boundaries / `errorElement` → error de render = app en blanco | `app/routes.jsx`, `App.jsx` |

> Nota: F1 agrava los IDOR del backend (§3.1) solo en lectura de UI; las escrituras IDOR son del
> backend (B1–B4). Los bugs de concurrencia/seguridad que la doc de julio listaba como "frontend"
> (self-admin, carrera de propuestas, revocación de rol) son en realidad del backend → B5, B8, B15.

---

## 5. Funcionalidades faltantes (backend)

**Backend:**
- **Geocodificación/validación de direcciones** (`verifyAddress`/`getCoordinates`) — placeholder.

**Frontend (endpoints del backend aún no expuestos/consumidos en `api.js` o sin UI):**
- **Cambio de email** (`PATCH /users/me/email`): no está en `api.js` (solo `changePassword`).
- **Catálogos de tags de feedback** (`GET /service-feedback-tags`, `/client-feedback-tags`): sin
  exponer → el modal de reseña no puede ofrecer tags para calificar.
- **Lectura de reseñas por servicio** (`GET /services/{id}/feedback`): `feedbackApi` solo lee por
  `requestId`, no la lista por servicio.
- **Gestión fina de disponibilidad del oferente** (borrar/activar/desactivar slots): `availabilityApi`
  solo tiene `get`/`save` (PUT del horario completo).
- **Reportar reseña de cliente**: `reportApi.createClientFeedbackReport` existe pero **sin UI** que lo
  invoque (un oferente no puede reportar la reseña de un cliente).
- **Paginación real** en las 3 páginas de búsqueda admin; **campo de nota/justificación** al moderar.
- **Transiciones de servicio** (`POST /services/{id}/activate|deactivate`, `PATCH .../soft-delete`):
  el backend las tiene pero `serviceApi` no las expone (solo `update`/`delete`).
- **Otros gaps de bajo impacto en `api.js`** (de la doc de julio, verificar vigencia): propuestas por
  solicitud (`GET /service-requests/{id}/proposals`); catálogo de roles admin (`GET /admin/roles`,
  hoy hardcodeado); `POST /categories` + `GET /categories/{id}`; detalle admin de solicitud
  (`GET /admin/service-requests/{id}`). Las métricas por tag y principales **ya están expuestas**
  (`metricsApi`), gap residual menor.
- _(Ampliar cruzando con `estructura-endpoints.md`: endpoints del diseño no implementados en backend.)_

---

## 6. Mejoras, replanteamientos y nuevas capacidades propuestas

> **Backlog preliminar** sembrado desde el diagnóstico del backend. Se consolidará y priorizará al
> cerrar el diagnóstico (frontend + docs). Marcadas con su valor y si son **prerrequisito de
> microservicios** (⚙️).

### 6.1 Fundacionales (habilitan todo lo demás)

- **M1 — Capa de autorización centralizada.** Activar `@EnableMethodSecurity` + `@PreAuthorize` para
  el gate de rol y un patrón uniforme de verificación de propiedad en el servicio (un
  `OwnershipChecker`/política). Elimina de raíz la clase de bugs IDOR (B1–B4, B14) en vez de parchear
  endpoint por endpoint. Cambiar el default a `authenticated()` y declarar lo público explícitamente.
- **M2 — Bloqueo optimista (`@Version`) + contadores atómicos.** `@Version` en todas las entidades;
  para métricas, `UPDATE … SET x = x + ?` atómico (o reproyección) en vez de read-modify-write.
  Resuelve B7, B8, B10.
- **M3 — ⚙️ Transactional Outbox + consumidores idempotentes.** Persistir los eventos de dominio en
  una tabla outbox dentro de la transacción de negocio y publicarlos tras commit con reintento;
  consumidores con clave de idempotencia. Da fiabilidad a métricas HOY y es **la base del
  event-driven entre microservicios** (reemplaza los `@TransactionalEventListener` in-process).
- **M4 — ⚙️ Lock distribuido para `@Scheduled`** (ShedLock o equivalente). Prerrequisito para
  escalar a >1 instancia. Resuelve B9.
- **M5 — ⚙️ Almacenamiento de objetos externo** (S3/MinIO) detrás de un puerto `FileStoragePort`,
  en vez de disco local. Prerrequisito para réplicas/contenedores efímeros.
- **M6 — Gestión de secretos sin defaults**: fail-fast si falta `JWT_SECRET`/`ENCRYPTION_KEY` en
  prod; perfiles Spring (`dev`/`prod`) que separen config de desarrollo (stacktraces, DEBUG, CORS).
- **M24 — ⚙️ Estrategia de pruebas (red de seguridad de la migración).** Prerrequisito real del
  estrangulamiento: sin tests que fijen el comportamiento, refactorizar fronteras y partir servicios es
  a ciegas. Incluye: **(a)** tests de **integración con Testcontainers** (MySQL real) por módulo/flujo;
  **(b)** tests de **controlador + seguridad** (`@WebMvcTest` + `spring-security-test`/`@WithMockUser`)
  que cubran autorización e IDOR (B1–B4); **(c)** cubrir **`feedback` y `metrics`** (hoy en 0), con
  **tests de concurrencia** para el lost-update (B7) y las propuestas duplicadas (B8); **(d) jacoco**
  + umbral de cobertura y **failsafe** (fase de integración separada); **(e)** al partir, **contract
  tests** (Spring Cloud Contract / Pact) entre el API gateway y cada servicio, y sobre los esquemas de
  evento del bus. Es la base que vuelve seguro todo lo demás de la Fase 0.

### 6.2 Replanteamientos de diseño

- **M7 — Read-models / proyecciones para lecturas compuestas pesadas** (detalle de servicio,
  listados de reportes/feedback). Hoy se componen en caliente con N+1 y lecturas cruzadas; una
  proyección denormalizada (materializada o CQRS) mejora rendimiento **y** marca la costura por donde
  separar los servicios de lectura. Resuelve B13 y los N+1.
- **M8 — Reemplazar llamadas síncronas cross-módulo por contratos explícitos.** Prohibir el acceso a
  **puertos de persistencia ajenos** (requests→services/profiles; admin→feedback) y a
  `infrastructure` ajena (admin). Introducir APIs/puertos de consulta estables entre módulos — paso
  previo obligatorio a convertirlos en llamadas remotas o réplicas de datos.
- **M9 — Sagas/orquestación para flujos cross-módulo** (borrado de cuenta, moderación, revocación de
  rol). Hoy son transacciones ACID multi-módulo; rediseñarlas como saga con compensación es el
  trabajo central de la migración.
- **M10 — Normalizar manejo de errores**: excepciones de dominio consistentes (nada de
  `IllegalArgumentException`→500); revisar `UnauthorizedException`→401 vs 403 y documentar la
  decisión.

### 6.3 Nuevas capacidades / valor (a evaluar)

- **M11 — Observabilidad**: logging estructurado, métricas (Micrometer/Prometheus), trazas
  distribuidas (OpenTelemetry). Imprescindible antes y durante la migración.
- **M12 — API gateway + versionado**: `admin` ya tiene forma de BFF; formalizar un gateway/BFF y el
  versionado `/api/v1` como contrato.
- **M13 — Rate limiting distribuido** (Redis) cuando haya varias instancias.
- **M14 — Búsqueda dedicada** (índice tipo OpenSearch) si el volumen de servicios crece; hoy
  `Specification` sobre MySQL es suficiente.
- **M15 — Reconciliación de métricas** (job que recalcula desde la fuente de verdad) como red de
  seguridad del modelo event-driven.

### 6.4 Frontend (del diagnóstico del frontend)

- **M16 — `<ProtectedRoute>` + redirección por rol** (usa `isAuthenticated`/`rolesFromToken`/
  `homePathForRoles`, ya existentes). Resuelve F1.
- **M17 — Contexto de sesión global (`AuthContext`/`SessionContext`)**: usuario, roles, avatar, token
  en un solo lugar. Elimina el fetch repetido de perfil (9 páginas) y el hardcode de avatar/nav.
- **M18 — `DashboardLayout` que derive nav+avatar del rol/usuario** (hoy por props) — se apoya en M17.
- **M19 — Interceptor de 401 en `request()`** (clearToken + redirect a `/login`). Resuelve F2.
- **M20 — Error boundaries / `errorElement`** por ruta. Resuelve F5.
- **M21 — Limpieza**: deduplicar `timeAgo`/`TYPE_META` a `shared/`; paginación escalable de
  notificaciones; decidir destino de `RequestServicePage` (cablear o borrar la ruta muerta); dropdown
  de categoría en `AdminServicesPage`; resolver nombres en lote (no N+1 en el cliente).
- **M22 — Endpoint de stats admin dedicado** (`GET /api/v1/admin/stats`) en vez de 4 consultas de
  listado + N+1 de nombres en el dashboard admin (hoy aprovecha `totalElements`).
- **M23 — Enriquecer el detalle de servicio** (`ServiceDetailPage` pública y/o la del oferente) con
  feedback de ambas partes, más métricas del servicio/oferente y tags de reseña (hoy muestra un set
  limitado; requiere exponer/usar más lecturas de feedback — ver §5).

---

## 7. Plan de migración a microservicios

> Enfoque acordado: **gradual, priorizando viabilidad, factibilidad y facilidad**. Se apoya en el
> grafo de acoplamiento (§3.3) y en las mejoras fundacionales (§6.1). Patrón rector:
> **Strangler Fig** (estrangulamiento) sobre un **monolito modular endurecido** — nada de reescritura.

### 7.0 Marco de decisión (requisito de entrega)

**El modelo de microservicios con un API gateway que se comunica con todos los microservicios es un
REQUISITO de la nueva fase del proyecto, no una opción.** Por tanto la separación **se hará completa**:
todos los servicios detrás del gateway. El driver aquí es el **requisito de entrega**, y eso resuelve
la pregunta del "si"; la ingeniería se concentra en el **"cómo"**, para que el resultado no degrade en
un **monolito distribuido** (el peor de los mundos: la complejidad de red sin la independencia real):

- **Fase 0 (§7.2) primero o en paralelo, nunca después.** Endurecer las fronteras del monolito —quitar
  el acceso a persistencia ajena (§3.3), convertir las transacciones cross-módulo en sagas, meter el
  outbox, arreglar autorización y concurrencia— **antes** de cortar. Partir sobre el acoplamiento actual
  reproduce esos problemas convertidos en fallos de red, mucho más caros.
- **Grano grueso (§7.3).** El clúster `catalog` (`profiles`+`services`) viaja como **una sola unidad**;
  sub-dividirlo sería el monolito distribuido de manual.
- **Strangler Fig por hojas (§7.4).** Empezar por `notifications`/`metrics` **de-riska** el gateway, el
  bus de eventos y la operación distribuida antes de tocar el núcleo — pero el **destino es el split
  completo**, no un alto a mitad de camino.
- **Pruebas como red de seguridad (M24).** Sin tests de integración/contrato, el estrangulamiento es a
  ciegas (hoy la cobertura de integración es casi nula, §3.7).

> Clave: **la Fase 0 no es "en vez de" microservicios; es lo que hace que el split sea viable.** Su
> trabajo se reutiliza al 100 % en la separación y además deja el sistema sano desde el día 1.

### 7.1 Principios rectores

1. **Monolito modular endurecido primero.** Convertir las fronteras de módulo actuales en fronteras
   duras (sin acceso a persistencia ajena, sin transacciones cross-módulo) **dentro** del monolito.
   Si eso se logra, el 80 % del trabajo de microservicios ya está hecho y el split se vuelve mecánico.
2. **Grano grueso.** Pocos servicios grandes, no nano-servicios. El clúster `profiles`+`services`
   **viaja junto** (§3.3): partirlo sería un monolito distribuido.
3. **Datos por servicio.** Cada servicio dueño de sus tablas; lo que hoy es un `JOIN`/FK cruzando
   módulos se vuelve llamada a API o **réplica de datos vía eventos**.
4. **Event-driven donde ya lo es.** Métricas y notificaciones ya consumen eventos → son la cabeza de
   playa natural, una vez que los eventos salgan del proceso (outbox + broker).
5. **Contratos explícitos.** Nada de compartir entidades de dominio entre servicios; DTOs/esquemas
   versionados (anti-corruption layer).

### 7.2 Fase 0 — Endurecer el monolito modular (prerrequisito, aporta valor por sí sola)

> Es la fase **más importante y la de mayor ROI**. Todo esto es valioso aunque nunca se migre.

| Paso | Mejora (§6) | Resuelve |
|---|---|---|
| Autorización centralizada (method security + propiedad) | M1 | B1–B4, B14 (clase IDOR) |
| Bloqueo optimista `@Version` + contadores atómicos | M2 | B7, B8, B10 |
| **Transactional Outbox + consumidores idempotentes** | M3 | fiabilidad de eventos; **base del bus** |
| Lock distribuido para `@Scheduled` | M4 | B9 (habilita >1 instancia) |
| Almacenamiento de objetos externo (puerto `FileStoragePort`) | M5 | estado compartible |
| Secretos sin defaults + perfiles dev/prod | M6 | B6, B11 |
| **Prohibir acceso a persistencia/infra ajena; introducir APIs de consulta entre módulos** | M8 | desacopla el grafo §3.3 |
| **Sagas/compensación para flujos cross-módulo** (borrado de cuenta, moderación, revocación de rol) | M9 | transacciones distribuidas |
| Read-models para lecturas compuestas pesadas | M7 | N+1, costura de lectura |
| Observabilidad (logs estructurados, métricas, trazas) | M11 | operar un sistema distribuido |

**Hito de salida de Fase 0:** el monolito corre en **≥2 instancias** sin duplicar trabajo
(schedulers con lock, estado fuera del disco, eventos fiables), con autorización correcta y sin que
ningún módulo toque la persistencia de otro. **Si no se llega aquí, no se empieza a partir.**

### 7.3 Decomposición objetivo (grano grueso)

Candidatos de servicio derivados del grafo (§3.3), agrupando lo que está fuertemente acoplado:

| Servicio | Módulos actuales | Datos (tablas) | Nota |
|---|---|---|---|
| **identity** | `users` (+ auth, roles, consent, reset) | users, roles, user_roles, consents, password_reset_tokens | Fundacional; todos dependen de él. |
| **catalog** | `profiles` **+** `services` (clúster) | user_profiles, offerer_profiles, addresses, availabilities, services, categories | Van **juntos** (acoplamiento bidireccional). |
| **requests** | `requests` | service_requests, reschedule_proposals | Máquina de estados; ya aislada en lógica. |
| **feedback** | `feedback` | *_feedback, *_feedback_tags(+catalog) | Consumidor/productor de eventos. |
| **metrics** | `metrics` | *_metrics (read-models) | Puro consumidor de eventos. |
| **notifications** | `notifications` | notifications, deliveries, channels | Hoja + integración externa (Brevo). |
| **moderation** | `reports` + `admin.ModerationService` | reports(+subtipos), report_actions | Orquesta identity/feedback/requests. |
| **gateway/BFF** | `admin` (orquestación) + API gateway | — | No es servicio de datos; orquesta y enruta. |

### 7.4 Orden de extracción (Strangler Fig, de hoja a núcleo)

1. **notifications** — la más fácil: ya es hoja + I/O externo. Requiere: recibir `notify` como
   **evento** (no llamada síncrona) y conocer el email (API a identity o réplica). Primer servicio real.
2. **metrics** — puro consumidor de eventos vía el bus (M3). Su lado de lectura (hoy síncrono desde
   catalog/moderation) se sirve por API o read-model replicado.
3. **feedback** — productor/consumidor de eventos; depende de `requests` (validación) → API de consulta.
4. **catalog** (`profiles`+`services`) — el clúster, extraído como **una unidad**. Aquí se paga el
   grueso del desacople (read-models M7, contratos M8).
5. **requests** — depende hoy de la persistencia de catalog/profiles (§3.3) → se convierte en llamadas
   a catalog + datos denormalizados que ya trae en eventos.
6. **moderation** (reports+admin moderación) — penúltimo: orquesta a casi todos vía **saga** (M9).
7. **gateway/BFF** — se formaliza al final (o desde temprano como fachada del estrangulamiento):
   enruta, agrega (sustituye la composición que hoy hace `admin`/detalle de servicio) y valida el JWT.

**identity** se extrae temprano o se mantiene como núcleo compartido según convenga (todos dependen
de él; una opción pragmática es que sea el primer "núcleo" detrás del gateway).

### 7.5 Infraestructura transversal necesaria (cuando se parta)

- **API Gateway** (enrutado, TLS, agregación BFF) + **service discovery** + **config centralizada**.
- **Bus de eventos** (Kafka/RabbitMQ) alimentado por el **outbox** (M3); **schema registry** para
  contratos de evento versionados; consumidores **idempotentes** (clave de evento).
- **Autenticación distribuida**: validar el JWT en el gateway o en cada servicio; sustituir la
  **consulta a BD por request** (§3.1) por introspección/caché o TTL corto + refresh (evita que
  `identity` sea un cuello de botella síncrono).
- **Datos por servicio**: partir el esquema MySQL único; las FK que cruzan servicios pasan a API o a
  **réplicas por evento** (p. ej. `requests` denormaliza `client_id/offerer_id`, ya lo hace).
- **Rate limiting distribuido** (Redis, M13); **storage de objetos** (M5); **observabilidad**
  distribuida con trazas (M11).

### 7.6 Riesgos y anti-patrones a evitar

- **Monolito distribuido**: partir el clúster `catalog` o dejar llamadas síncronas en cadena → peor
  que el monolito. Mitigación: grano grueso (§7.3) + read-models (M7).
- **N+1 de red**: los N+1 de §3.4 se vuelven llamadas remotas. Resolver ANTES (read-models) de partir.
- **Consistencia eventual mal entendida**: las métricas ya son eventualmente consistentes; con broker
  hace falta idempotencia + reconciliación (M15) o los contadores derivan/duplican.
- **Sagas sin compensación**: los flujos de §3.3 (ban+resolve, borrado de cuenta) necesitan pasos de
  compensación explícitos, no "transacción distribuida".
- **Sobre-fragmentación**: resistir el impulso de un servicio por módulo; empezar por los 2–3 que
  tengan un driver real.

### 7.7 Resumen ejecutable

El destino es el **split completo con API gateway** (requisito). Secuencia recomendada para llegar
con el mínimo riesgo:

1. **Fase 0 (§7.2) — ya / en paralelo.** Obligatoria; de-riska todo y es reutilizable al 100 %.
   Incluye la estrategia de pruebas (M24), que es la red de seguridad del resto.
2. **Levantar el API gateway temprano** como fachada del estrangulamiento: al principio enruta todo al
   monolito; luego, a medida que se extraen servicios, enruta a cada uno (el gateway habla con todos).
3. **Extraer de hoja a núcleo (§7.4):** `notifications` → `metrics` → `feedback` → **`catalog`
   (como unidad)** → `requests` → `moderation`; `identity` como núcleo detrás del gateway.
4. **Destino:** todos los servicios tras el **API gateway**, comunicados por **API síncrona vía
   gateway** + **eventos** (bus con outbox e idempotencia). Datos por servicio (§7.5).

**Evitar (§7.6):** sub-dividir `catalog`; partir antes de la Fase 0; dejar cadenas de llamadas
síncronas (los N+1 de §3.4 se vuelven N+1 de red); sagas sin compensación; sobre-fragmentar.

---

## 8. Decisiones abiertas

- Ubicación/nombre definitivo de este documento (hoy en la raíz del repo).
- ¿`CLAUDE.md` se reescribe conciso apuntando aquí (recomendado) o se elimina? — pendiente de tu
  decisión del inicio.
- Destino de cada doc en `documents/project-structure/` (dejar/actualizar/eliminar) — frente de docs.
- ¿Se versiona este documento en git? **Estado real del tracking** (verificado en `.gitignore`):
  `CLAUDE.md` y `NOTAS.txt` **NO** están versionados (ambos en `.gitignore`); `documents/` **SÍ**
  (37 archivos); este documento (`ESTADO-ACTUAL-Y-PLAN-FUTURO.md`) **no está en `.gitignore`** → se
  versionaría si se hace `git add`. Decidir si versionarlo (recomendado: sí, es la fuente de verdad).
- **Frontend — decisiones de diseño pendientes** (de la doc de julio, siguen vigentes):
  - Destino de `AdminFeedbackPage` (`/admin/feedback`): se quitó del menú pero la ruta/página existen
    → reintegrar, fusionar o eliminar.
  - `RequestServicePage`: cablear el wizard o borrar la ruta muerta `/request-service`.
  - Comportamiento de las pestañas de rol del login (multi-rol) y destino del logo con sesión activa.

---

## 9. Auditoría de documentación y memoria

> `documents/` está **versionado en git** (37 archivos) → cualquier borrado es recuperable.
> Decisiones: **DEJAR** / **ACTUALIZAR/ANOTAR** / **ELIMINAR** / **CONSOLIDAR**.

### 9.1 `documents/` — decisiones por archivo

| Documento | Rol | Estado | Decisión |
|---|---|---|---|
| `GUIA-EJECUCION-SERVIYA.md` | Guía de ejecución (ops) | Vigente, canónica | **DEJAR** |
| `project-structure/GUIA_DTOS.txt` | Convención DTO/mappers | El código la sigue | **DEJAR** (referencia) |
| `project-structure/estructura-endpoints.md` | Spec REST completa | **Drift**: dice `UnauthorizedException`→403 (real: **401**); `max size=100` (real: **20**) | **ACTUALIZAR/ANOTAR** "diseño; el código manda" |
| `project-structure/estructura-servicios.docx` | Firmas de cada servicio | Probable stale (feedback unificado, `FeedbackFlow` muerto, servicios renombrados) | **ANOTAR** stale; fuente real = código |
| `project-structure/documentacion-BD.docx` | Tablas de la BD | Posible stale tras unificación de feedback | **ANOTAR**; fuente real = `create_database.sql` + entidades |
| `project-structure/estructura-frontend.md` | Referencia frontend (estudio) | Solapa con §2.4 | **DEJAR** (estudio); §2.4 manda para estado |
| `project-structure/planificacion-recuperacion-contrasena.md` | Planificación de RF-003 | Feature **ya implementada y verificada** | **ELIMINAR** (histórico; contenido en código + memoria) |
| `convenciones-nombres-carpetas-archivos.md` | Convenciones de nombres | Vigente | **DEJAR** (o fusionar lo esencial en CLAUDE.md) |
| `SERVIYA_Exposicion_Tecnica.pptx` | Presentación técnica (entregable) | Entregable académico | **DEJAR** |
| `requirements/backlog-requirements.docx` (EN) + `backlog-requisitos.docx` (ES) | Backlog RF/RNF (referenciado por el código) | **Par EN/ES duplicado** | **DEJAR** (preguntar: ¿un idioma?) |
| `user-stories/user-stories.docx` (EN) + `historias-usuario.docx` (ES) | Historias de usuario | **Par EN/ES duplicado** | **DEJAR** (preguntar: ¿un idioma?) |
| `diagrams/*` (ER, clases, BPMN, CRC, arquitectura) | Diagramas de diseño | Vigentes | **DEJAR** |
| `Workshop-2/docs/*` | **Casi-duplicado** de `diagrams/*` | Snapshot de entrega del Workshop 2 | **CONSOLIDAR** (preguntar: es entregable calificado) |

**Acciones que requieren tu visto bueno** (entregables académicos / calificables): eliminar la
duplicación `Workshop-2/docs` ↔ `diagrams/`, y consolidar los pares EN/ES. No las ejecuto sin OK.

### 9.2 Memoria del asistente — correcciones pendientes

> La memoria (`~/.claude/.../memory/`) se consolida en el cierre (junto a CLAUDE.md/NOTAS). Hallazgos:

- **ACTUALIZAR `security-gating-status`**: afirma "3 controllers con 0L pendientes" → **ya no hay
  `0L`** (todos usan `CurrentUser.id()`). El hallazgo real vigente es distinto: `permitAll` por
  defecto + IDOR en escrituras (§3.1).
- **PRUNE (histórico resuelto)**: `main-broken-feedback-merge`, `docker-secrets-not-injected` — eran
  incidencias puntuales ya resueltas; su valor es nulo para el futuro.
- **MANTENER (vigentes y confirmadas en este diagnóstico)**: `unauthorized-maps-to-401`,
  `backend-concurrency-perf-audit`, `metrics-events-implementation`, `role-security-split-convention`
  (matizar: el "gate fino en servicio" **no** se cumple en services/profiles), `created-at-convention`,
  `repo-moved-out-of-onedrive`, `e2e-*`.
- **AÑADIR** (de este diagnóstico): puntero a este documento como fuente de verdad del estado + plan.

### 9.3 CLAUDE.md — reescritura pendiente (cierre)
Correcciones mínimas imprescindibles (hoy induce a error): backend **no** es placeholder; **no** hay
Flyway; **no** hay `@PreAuthorize` (autorización = URL-matchers + `permitAll`); `UnauthorizedException`
→ 401 (no 403); apuntar a este documento para el estado/plan.

---

## 10. Registro de la sesión de diagnóstico

- **2026-10-03** — Sesión de diagnóstico (sin tocar código). Backend barrido módulo por módulo
  (estructura + config + 9 módulos). Creado este documento con los hallazgos del backend y un backlog
  preliminar de mejoras.
- **2026-10-03** — Frontend diagnosticado (wiring, routing, capa de API, layout/estado) y
  **`estado-frontend-pendientes.md` consolidado aquí** (§2.4.1, §3.6, §4, §5, §6.4, §8) y **borrado**.
- **2026-10-03** — Auditoría de documentación (§9.1) y memoria (§9.2). **Eliminado**
  `planificacion-recuperacion-contrasena.md` (feature ya implementada). **Unificado y eliminado
  `documents/Workshop-2/`** (era duplicado byte-a-byte de `diagrams/`; su índice + el enlace de
  mockups preservados en `diagrams/README.md`). Pares ES/EN **conservados** por decisión del equipo.
- **2026-10-03** — Redactado el **plan de migración a microservicios** (§7).
- **2026-10-03** — Dos insumos del equipo incorporados: **(1)** el split a microservicios con **API
  gateway** es **requisito de entrega** (no opcional) → §7.0 y §7.7 reencuadrados a "split completo, el
  cómo es lo que importa"; **(2)** diagnóstico de **testing** (§3.7): cobertura casi nula de integración,
  `feedback`/`metrics` con 0 tests → nueva mejora **M24** (estrategia de pruebas como red de seguridad
  de la migración). Siguiente: reescritura de CLAUDE.md (§9.3), correcciones de memoria (§9.2), NOTAS.txt
  y §1 resumen ejecutivo.
</content>
</invoke>
