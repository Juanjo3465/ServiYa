# APIs entre modulos del monolito

> Estado: **Contratos, migración de consumidores, BFF y grafo acíclico completados**
> (2026-11-08). Define los contratos publicos y el mecanismo para que los modulos dejen de depender
> de `domain`, `infrastructure` y puertos de persistencia ajenos. `metrics`, `notifications`,
> `profiles` y `services` son hojas; el BFF compone las lecturas cruzadas. Las reglas ArchUnit son
> absolutas y el grafo de modulos es aciclico.
> Ver `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §7 y `ModuleBoundaryTest`.

## Control de cambios

| Fecha | Cambio | Estado |
|---|---|---|
| 2026-11-08 | Reglas ArchUnit de fronteras entre modulos con baseline congelado (`ModuleBoundaryTest`). | Completado |
| 2026-11-08 | Incremento 1 de contratos: `shared.contract`, `notifications.api`, `users.api`; contratos de eventos con `String`/`boolean`. | Completado |
| 2026-11-08 | Incremento 2A: contratos del cluster catalog (`profiles.api` + `services.api`), lecturas por lote y fachadas. | Completado |
| 2026-11-08 | Incremento 2B: `requests` migrado a `profiles.api`/`services.api`; baseline reducido. | Completado |
| 2026-11-08 | Incremento 3: contratos `requests.api`, `feedback.api` y `metrics.api` con fachadas y tests. | Completado |
| 2026-11-08 | Migración `feedback` → `requests.api`; tests nuevos de feedback. | Completado |
| 2026-11-08 | Migración `users` → `profiles.api` en `AccountController`. | Completado |
| 2026-11-08 | Nuevo módulo `statistics`; `shared` queda como kernel puro (regla absoluta). | Completado |
| 2026-11-08 | Incremento 4: contrato `reports.api` + fachada y tests. | Completado |
| 2026-11-08 | `RequestLookupApi` para romper el ciclo de beans; gate completo de tests habilitado (H2 + ENCRYPTION_KEY). | Completado |
| 2026-11-08 | Migración `reports` → `profiles.api`, `requests.api`, `services.api`, `feedback.api`, `users.api` y `notifications.api`; baseline **272 → 263**. | Completado |
| 2026-11-08 | Migración `services` → `metrics.api`, `profiles.api` y `feedback.api`; `RequestCountApi` rompe el ciclo de beans; baseline **263 → 183**. | Completado |
| 2026-11-08 | Migración `metrics`/`notifications` → `feedback.api` y `UserContactApi`; baseline **183 → 176**. | Completado |
| 2026-11-08 | Migración `users` → `notifications.api`, `requests.api` y `profiles.api`; baseline **176 → 160**. | Completado |
| 2026-11-08 | Migración `profiles` → `metrics.api` y `services.api`; baseline **160 → 136**. | Completado |
| 2026-11-08 | Filtros de búsqueda de `services` resueltos vía `profiles.api` (nombre y geolocalización); baseline **136 → 130**. | Completado |
| 2026-11-08 | Contratos ampliados para `admin`: `getAllRoles`, `patchProfile`, búsqueda de feedback, reportes de subtipo y constantes compartidas. | Completado |
| 2026-11-08 | Migración `admin` a las APIs públicas; baseline **130 → 0** y reglas ArchUnit absolutas (sin freeze). | Completado |
| 2026-11-08 | Nuevo módulo `bff`: vitrina pública y resumen de oferente movidos desde `profiles`. | Completado |
| 2026-11-08 | `bff`: búsqueda/detalle/listado de servicios movidos desde `services`; `services` queda hoja. | Completado |
| 2026-11-08 | `bff`: tag-metrics, métricas de cliente y métricas propias; `metrics` queda hoja. | Completado |
| 2026-11-08 | Adaptador de correo movido a `config.integration`; `notifications` queda hoja. | Completado |
| 2026-11-08 | Test determinista de ciclos + regla de módulos hoja; grafo acíclico. | Completado |

## 1. Convencion

Cada modulo publica un unico paquete:

```
com.parosurvivors.serviya.<modulo>.api
    <Modulo>QueryApi.java
    <Modulo>CommandApi.java
    dto/*.java
```

- La implementacion vive en `<modulo>.application.api` (fachada `@Component`) y delega en los
  casos de uso existentes. No se mueven los puertos internos todavia.
- Los DTOs son `record` con primitivos, IDs, `java.time`, `java.math` y tipos de
  `shared.contract`. Nunca exponen entidades JPA, `domain` ni `infrastructure`.
- Los enums que cruzan fronteras viajan como `String` (estado, rol, tipo de perfil, tipo de
  reporte). El sentimiento de un tag viaja como `boolean positive`.
- Las busquedas paginadas usan `shared.contract.PageResult<T>`; no se expone `Page`/`Pageable`
  de Spring.
- Las consultas puntuales devuelven `Optional`; los comandos lanzan excepciones JDK o de
  `shared.contract`. La API no importa excepciones internas de otro modulo.

## 2. Contratos implementados (Incremento 1)

### `shared.contract`

| Tipo | Uso |
|---|---|
| `PageResult<T>` | Paginacion neutra para las APIs. |
| `Roles` | Constantes `CLIENT`, `OFFERER`, `ADMIN` para roles que cruzan modulos. |

Cambios de contrato de eventos en `shared.events.domain`:

- `RequestStatusChangedEvent`: `previousStatus`/`newStatus` ahora son `String`.
- `TagRef`: ahora es `(Long tagId, boolean positive)`.

Esto elimina las dependencias de `shared` hacia `requests.domain` y `feedback.domain`. El baseline
congelado de ArchUnit bajo de 24 a 15 violaciones para el kernel.

### `notifications.api`

| Tipo | Contenido |
|---|---|
| `Channel` | `INTERNAL`, `EMAIL`. |
| `NotificationCommand` | Destinatario, tipo, titulo, mensaje, entidad, canales y datos protegidos. |
| `NotificationCommandApi` | `void notify(NotificationCommand command)`. |

La fachada `NotificationCommandApiFacade` traduce `Channel` al enum interno `ChannelName` y delega
en `NotificationServicePort` (outbox transaccional intacto).

### `users.api`

| Interfaz | Operaciones |
|---|---|
| `UserQueryApi` | `findById`, `search`, `getRoles`, `getRoleAssignments`, `findUserIdsByRole`, `hasRole`. |
| `UserCommandApi` | `createUserAccount`, `changeEmail`, `changePassword`, `banUser`, `unbanUser`, `assignRole`, `revokeRole`, `removeRole`, `deleteUser`. |
| `UserContactApi` | `emailOf` (acceso acotado al correo/PII). |

DTOs: `UserSnapshot`, `UserSummary`, `RoleSnapshot`, `UserRoleAssignmentSnapshot`,
`UserSearchCriteria`, `CreateUserAccountCommand`.

`UserApiFacade` implementa las tres interfaces y traduce `String` de rol a `RoleName` en la
frontera. `UserSummary` solo contiene datos propios de identity; el nombre/foto del perfil los
compone el consumidor/BFF con `profiles.api`.

### `profiles.api` (Incremento 2A)

| Interfaz | Operaciones |
|---|---|
| `ProfileQueryApi` | `findProfile`, `findProfiles` (lote), `findOffererProfile`, `getOffererSummary`, `findAddress`, `getAddresses`, `getAvailabilities`. |
| `ProfileCommandApi` | `createProfile`, `createOffererProfile`, `createAddress`, `updateMainAddress`. |

DTOs: `ProfileSnapshot`, `OffererProfileSnapshot`, `OffererProfileSummarySnapshot`,
`AddressSnapshot`, `OffererAvailabilitySnapshot`, `CreateProfileCommand`, `CreateAddressCommand`.

Decisiones:

- Las lecturas usan los puertos de persistencia de profiles directamente; no se usa
  `OffererPublicProfileService` (que compone metrics/services).
- `OffererProfileSummarySnapshot` **no incluye `averageRating`**; ese dato lo compone el consumidor
  con `metrics.api`.
- `OffererAvailabilitySnapshot.weekDay` conserva la convención de profiles: **0 = lunes … 6 = domingo**.
- `ProfileSnapshot` incluye documento y teléfono (PII propia de profiles); la exposición pública
  debe pasar por un BFF con política de privacidad.
- Se añadió `UserProfilePersistencePort.findByUserIdIn(...)` para implementar `findProfiles` sin N+1.

### `services.api` (Incremento 2A)

| Interfaz | Operaciones |
|---|---|
| `ServiceQueryApi` | `findById`, `findByIds` (lote), `findByOffererId`, `findCategory`, `getCategories`, `getAvailabilities`. |
| `ServiceCommandApi` | `deactivateAllByOfferer`, `deleteServiceAsAdmin`. |

DTOs: `ServiceSnapshot`, `CategorySnapshot`, `ServiceAvailabilitySnapshot`.

Decisiones:

- No se expone `ServiceDetail` ni otra vista compuesta; el detalle se arma fuera del módulo.
- `ServiceAvailabilitySnapshot.weekDay` conserva la convención de services: **1 = lunes … 7 = domingo**.
- `findByOffererId` devuelve todos los servicios del oferente, incluidos inactivos/soft-deleted; el
  consumidor filtra por `active`/`deletedAt`.
- Las lecturas usan los puertos de persistencia de services; las escrituras delegan en
  `MarketplaceServicePort`.
- Se añadió `ServicePersistencePort.findByIds(...)` para implementar la consulta por lote sin N+1.

### `requests.api` (Incremento 3)

| Interfaz | Operaciones |
|---|---|
| `RequestQueryApi` | `findById`, `findDetail`, `countActiveClientRequests`, `searchForClient`, `searchForOfferer`. |
| `RequestCommandApi` | `cancelActiveRequestsForUser`, `cancelActiveRequestsForRole`, `markAsNotProvided`. |

DTOs: `RequestSnapshot`, `RequestDetailSnapshot`, `RequestSummary`, `RequestSearchCriteria`.

Decisiones:

- La API expone solo datos propios de la solicitud (ids, estado, fechas, precio).
- `findDetail` **no** compone nombres, titulo del servicio, categoria ni direccion; el consumidor/BFF
  lo hace con `profiles.api` y `services.api`.
- Los estados viajan como `String`; `RequestSnapshot` ofrece helpers `isCompleted`,
  `isPresumablyCompleted`, `isTerminal`.

### `feedback.api` (Incremento 3)

| Interfaz | Operaciones |
|---|---|
| `FeedbackQueryApi` | `findServiceFeedback`, `findClientFeedback`, `getRecentServiceFeedback`, `existsServiceFeedbackForRequest`, `existsClientFeedbackForRequest`, `getServiceTags`, `getClientTags`. |
| `FeedbackCommandApi` | `revertServiceFeedback`, `revertClientFeedback`. |

DTOs: `ServiceFeedbackSnapshot`, `ClientFeedbackSnapshot`, `TagCatalogEntry`.

Decisiones:

- Los feedback snapshots llevan `tagIds`; los nombres/sentimiento viven en `TagCatalogEntry`.
- `metrics.api` no depende de `feedback.api`: la enrichment de nombres de tags queda en el BFF.

### `metrics.api` (Incremento 3)

| Interfaz | Operaciones |
|---|---|
| `MetricsQueryApi` | `getServiceMetrics` (simple y por lote), `getOffererMetrics`, `getOffererMainMetrics`, `getClientMetrics`, `getClientMainMetrics`, `getServiceTagMetrics`, `getOffererTagMetrics`, `getClientTagMetrics`. |

DTOs: `ServiceMetricsSnapshot`, `OffererMetricsSnapshot`, `ClientMetricsSnapshot`, `TagMetricSnapshot`.

Decisiones:

- `TagMetricSnapshot` solo lleva `tagId` + `count`; el nombre/sentimiento se enriquece con
  `feedback.api`.
- Los snapshots reflejan los contadores actuales del read-model de metrics.

### `reports.api` (Incremento 4)

| Interfaz | Operaciones |
|---|---|
| `ReportQueryApi` | `findById`, `findSummary`, `search`, `getActions`, `countByReportedUser`, `countByReporter`. |
| `ReportCommandApi` | `createBaseReport`, `resolveReport`, `closeReport`, `createAction`. |

DTOs: `ReportSnapshot`, `ReportSummarySnapshot`, `ReportActionSnapshot`.

Decisiones:

- `ReportSnapshot`/`ReportSummarySnapshot` exponen datos propios del reporte y el id del objetivo;
  `ReportDetailResult` (con nombres y payloads) se compone en el consumidor/BFF.
- Los tipos de accion viajan como `String` y se validan contra `ReportActionType` en la frontera.

## 3. Reglas ArchUnit

`backend/serviya/src/test/java/com/parosurvivors/serviya/architecture/ModuleBoundaryTest.java`:

- Ningun modulo importa `infrastructure`, `application.ports.output` ni `domain` de otro modulo.
- `shared` no importa modulos de negocio.
- Los paquetes `.api` solo dependen de JDK/Spring, del propio paquete `.api` y de
  `shared.contract`; no pueden tocar `domain` ni `infrastructure` ni siquiera del mismo modulo.
- `domain` no depende de `application`/`infrastructure`; `application` no depende de
  `infrastructure`.
- `metrics`, `notifications`, `profiles` y `services` son **módulos hoja**: no pueden depender de
  ningún otro módulo de negocio (solo de `shared`).
- El **grafo de dependencias entre módulos es acíclico**. Se verifica con una matriz determinista
  (Tarjan sobre dependencias directas de ArchUnit), sin `FreezingArchRule`.

Las reglas son **absolutas**: ya no usan `FreezingArchRule` ni hay baseline congelado. Cualquier
dependencia cruzada nueva o ciclo nuevo rompe el build.

| Regla | Violaciones |
|---|---:|
| `domain` ajeno | 0 |
| Puertos de persistencia ajenos | 0 |
| `infrastructure` ajena | 0 |
| `shared` hacia modulos | 0 |
| **Total** | **0** |

Antes del Incremento 1 eran 451 (24 shared + 326 domain + 64 persistence + 37 infrastructure).
El Incremento 2A no cambio el baseline (**414**). El Incremento 2B lo redujo a **316**:
`requests` ya no importa `profiles`/`services` `domain` ni `application.ports.output`.
El Incremento 3 no cambio el baseline. Las migraciones posteriores lo redujeron a **272**:

- `feedback` → `requests.api`: **316 → 294**.
- `users` → `profiles.api` (`AccountController`): **294 → 287**.
- `statistics` extraido de `shared`: **287 → 272**.

La migracion de consumidores lo redujo de **272 a 0**:

- `reports` → APIs publicas (`profiles`, `requests`, `services`, `feedback`, `users`,
  `notifications`): **272 → 263**.
- `services` → `metrics.api`/`profiles.api`/`feedback.api` + `RequestCountApi`: **263 → 183**.
- `metrics`/`notifications` → `feedback.api`/`UserContactApi`: **183 → 176**.
- `users` → `notifications.api`/`requests.api`/`profiles.api`: **176 → 160**.
- `profiles` → `metrics.api`/`services.api`: **160 → 136**.
- Filtros de búsqueda de `services` resueltos vía `profiles.api` (nombre y geolocalización):
  **136 → 130**.
- `admin` → APIs publicas de users/profiles/metrics/reports/requests/services/feedback/notifications:
  **130 → 0**.

La regla `shared` tambien es absoluta (0 violaciones). Ya no existe
`src/test/resources/archunit_store`: si una regla falla, es una violacion real que debe corregirse.

## 4. Siguientes incrementos

| Incremento | Contratos |
|---|---|
| 2A | `profiles.api` + `services.api` (catalog) — **completado**. |
| 2B | Migrar `requests` a las APIs de catalog — **completado**. |
| 3 | `requests.api` + `feedback.api` + `metrics.api` — **completado**. |
| 4 | `reports.api` — **completado**. |
| — | Migrar consumidores: `reports`, `services`, `metrics`, `notifications`, `users`, `profiles` y `admin` — **completado**. |
| — | BFF para lecturas compuestas y módulos hoja — **completado**. |
| — | Test determinista de ciclos entre módulos — **completado**. |

`admin` y `bff` no publican API: son consumidores/orquestadores de lectura. `config` es la raiz de
composicion y puede cablear implementaciones de cualquier modulo.

## 5. Verificación

Comandos ejecutados el 2026-11-08:

```bash
cd backend/serviya

# Contratos + reglas de arquitectura
./mvnw -Dtest='UserApiFacadeTest,NotificationCommandApiFacadeTest,ProfileApiFacadeTest,ServiceApiFacadeTest,RequestApiFacadeTest,FeedbackApiFacadeTest,MetricsApiFacadeTest,ReportApiFacadeTest,ModuleBoundaryTest' test

# Gate completo (sin exclusiones)
./mvnw test
```

Resultados:

- Fachadas de contrato: `UserApiFacadeTest` 8/8, `ProfileApiFacadeTest` 13/13,
  `ServiceApiFacadeTest` 8/8, `RequestApiFacadeTest` 9/9, `FeedbackApiFacadeTest` 10/10,
  `MetricsApiFacadeTest` 5/5, `ReportApiFacadeTest` 9/9 y
  `NotificationCommandApiFacadeTest` 1/1.
- Admin migrado: `AdminServiceTest` 6/6, `ModerationServiceTest` 13/13 y
  `AdminActionGuardTest` 7/7.
- BFF: `PublicOffererQueryServiceTest` 4/4, `ServiceCatalogQueryServiceTest` 6/6 y
  `MetricsReadQueryServiceTest` 3/3.
- `ModuleBoundaryTest`: 9/9 (7 fronteras + hojas + ciclos).
- **Suite completa**: **470 tests, 0 failures, 0 errors, 14 skipped** (`BUILD SUCCESS`).
  Ya no se excluyen `ServiyaApplicationTests` ni `PiiAttributeConverterTest`: se añadió H2 y se
  configura `ENCRYPTION_KEY` en el test de PII.
- Los 14 skipped son `RedisDistributedLockTest` y `RedisRateLimiterTest`
  (`@Testcontainers(disabledWithoutDocker = true)`).
- La regla de pureza de APIs no es vacua: encontro 9 clases en `users.api`, 3 en
  `notifications.api`, 4 en `shared.contract`, 9 en `profiles.api`, 5 en `services.api`,
  6 en `requests.api`, 5 en `feedback.api`, 5 en `metrics.api` y 5 en `reports.api`.

## 6. Alcance y límites

- **Migrados:** `requests` → catalog, `feedback` → `requests.api`, `users` →
  `profiles.api`/`notifications.api`/`requests.api`, `statistics` como módulo de lectura,
  `reports` → APIs públicas, `admin` → todas las APIs públicas.
- **BFF:** las lecturas compuestas viven en `bff`: vitrina/resumen de oferente, búsqueda/detalle/
  listado de servicios, plantilla de disponibilidad, tag-metrics, métricas de cliente y métricas
  propias.
- **Módulos hoja:** `metrics`, `notifications`, `profiles` y `services` solo dependen de sí mismos
  y de `shared`.
- **Pendientes:** ninguno de acoplamiento ni de ciclos entre módulos. Queda la eventual extracción
  de microservicios.
- **`RequestLookupApi`** es una lectura mínima de requests (solo `ServiceRequestReadPort`) para
  consumidores como feedback. Evita el ciclo de beans
  `feedback → RequestApiFacade → requests query → services.api → marketplace → feedback`.
- **`RequestCountApi`** es una lectura mínima de conteo para metrics; evita el ciclo
  `metrics → RequestApiFacade → requests query → services.api → marketplace`.
- **`UserContactApi`/`UserContactFacade`** se separaron de `UserApiFacade` para que notifications
  no arrastre `UserService → NotificationService`; evita el ciclo
  `notifications → user API → user service → notifications`.
- **`UserSummary` no incluye nombre ni foto.** Son datos de `profiles`; admin compone el listado y
  el detalle con `profiles.api` (`findProfiles`, `findProfile`).
- **`shared` es un kernel puro:** `PlatformStatsRepository` se movió al módulo `statistics`; la regla
  de pureza de `shared` ya no necesita baseline y es absoluta.
- **Los eventos cambiaron de tipo, no de transporte.** `RequestStatusChangedEvent` y `TagRef` siguen
  siendo eventos in-process (`ApplicationEventPublisher`); no hay broker para ellos todavia. Cuando
  se publiquen por RabbitMQ/Kafka, el contrato ya es serializable a JSON, pero debera versionarse.
- **`metrics` ya no importa `requests.domain`.** Los estados viajan como `String` y el switch interno
  ignora estados desconocidos. Esto prepara la extraccion de metrics sin arrastrar el enum de requests.
- **La arquitectura de fronteras esta limpia.** Las reglas ArchUnit son absolutas, el baseline
  congelado se elimino al llegar a **0** violaciones y el grafo de modulos es **aciclico**.

## 7. Archivos principales

```text
backend/serviya/src/main/java/com/parosurvivors/serviya/
  shared/contract/PageResult.java
  shared/contract/Roles.java
  shared/contract/ReportTypes.java
  shared/contract/ReportActions.java
  shared/events/domain/RequestStatusChangedEvent.java
  shared/events/domain/TagRef.java
  notifications/api/Channel.java
  notifications/api/NotificationCommand.java
  notifications/api/NotificationCommandApi.java
  notifications/application/api/NotificationCommandApiFacade.java
  users/api/UserQueryApi.java
  users/api/UserCommandApi.java
  users/api/UserContactApi.java
  users/api/dto/*.java
  users/application/api/UserApiFacade.java
  users/application/api/UserContactFacade.java
  profiles/api/ProfileQueryApi.java
  profiles/api/ProfileCommandApi.java
  profiles/api/dto/*.java
  profiles/application/api/ProfileApiFacade.java
  services/api/ServiceQueryApi.java
  services/api/ServiceCommandApi.java
  services/api/dto/*.java
  services/application/api/ServiceApiFacade.java
  requests/application/services/ServiceRequestCommandService.java
  requests/application/services/ServiceRequestQueryService.java
  requests/application/services/RescheduleProposalService.java
  requests/api/RequestQueryApi.java
  requests/api/RequestCommandApi.java
  requests/api/RequestCountApi.java
  requests/api/dto/*.java
  requests/application/api/RequestApiFacade.java
  requests/application/api/RequestCountFacade.java
  feedback/api/FeedbackQueryApi.java
  feedback/api/FeedbackCommandApi.java
  feedback/api/dto/*.java
  feedback/application/api/FeedbackApiFacade.java
  metrics/api/MetricsQueryApi.java
  metrics/api/dto/*.java
  metrics/application/api/MetricsApiFacade.java
  feedback/application/services/ServiceFeedbackService.java
  feedback/application/services/ClientFeedbackService.java
  users/infrastructure/adapters/input/AccountController.java
  statistics/application/services/PlatformStatsService.java
  statistics/application/dto/response/PlatformStatsResponse.java
  statistics/infrastructure/adapters/input/StatisticsController.java
  reports/api/ReportQueryApi.java
  reports/api/ReportCommandApi.java
  reports/api/dto/*.java
  reports/application/api/ReportApiFacade.java
  requests/api/RequestLookupApi.java
  requests/application/api/RequestLookupFacade.java
  requests/application/api/RequestSnapshots.java
  admin/application/dto/result/AdminUserSummaryResult.java
  admin/application/dto/result/AdminRequestDetailResult.java
  admin/infrastructure/dto/response/AdminRequestDetailResponse.java
  bff/application/services/PublicOffererQueryService.java
  bff/application/services/ServiceCatalogQueryService.java
  bff/application/services/MetricsReadQueryService.java
  bff/infrastructure/adapters/input/PublicOffererController.java
  bff/infrastructure/adapters/input/ServiceCatalogController.java
  bff/infrastructure/adapters/input/MetricsReadController.java
  bff/infrastructure/adapters/input/ServiceAvailabilityTemplateController.java
  config/integration/UserRecipientEmailAdapter.java

backend/serviya/src/test/java/com/parosurvivors/serviya/
  architecture/ModuleBoundaryTest.java
  notifications/application/api/NotificationCommandApiFacadeTest.java
  users/application/api/UserApiFacadeTest.java
  users/application/api/UserContactFacadeTest.java
  profiles/application/api/ProfileApiFacadeTest.java
  services/application/api/ServiceApiFacadeTest.java
  requests/application/api/RequestApiFacadeTest.java
  feedback/application/api/FeedbackApiFacadeTest.java
  metrics/application/api/MetricsApiFacadeTest.java
  feedback/application/services/ServiceFeedbackServiceTest.java
  feedback/application/services/ClientFeedbackServiceTest.java
  statistics/application/services/PlatformStatsServiceTest.java
  reports/application/api/ReportApiFacadeTest.java

backend/serviya/src/test/resources/
  archunit.properties
```
