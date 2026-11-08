# APIs entre modulos del monolito

> Estado: **Incrementos 1, 2A y 2B completados** (2026-11-08). Define los contratos publicos y el
> mecanismo para que los modulos dejen de depender de `domain`, `infrastructure` y puertos de
> persistencia ajenos. `requests` ya consume `profiles.api`/`services.api`.
> Ver `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §7 y `ModuleBoundaryTest`.

## Control de cambios

| Fecha | Cambio | Estado |
|---|---|---|
| 2026-11-08 | Reglas ArchUnit de fronteras entre modulos con baseline congelado (`ModuleBoundaryTest`). | Completado |
| 2026-11-08 | Incremento 1 de contratos: `shared.contract`, `notifications.api`, `users.api`; contratos de eventos con `String`/`boolean`. | Completado |
| 2026-11-08 | Incremento 2A: contratos del cluster catalog (`profiles.api` + `services.api`), lecturas por lote y fachadas. | Completado |
| 2026-11-08 | Incremento 2B: `requests` migrado a `profiles.api`/`services.api`; baseline reducido. | Completado |
| Pendiente | Incremento 3: `requests.api` + `feedback.api` + `metrics.api`. | Pendiente |
| Pendiente | Incremento 4: `reports.api`. | Pendiente |

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

## 3. Reglas ArchUnit

`backend/serviya/src/test/java/com/parosurvivors/serviya/architecture/ModuleBoundaryTest.java`:

- Ningun modulo importa `infrastructure`, `application.ports.output` ni `domain` de otro modulo.
- `shared` no importa modulos de negocio.
- Los paquetes `.api` solo dependen de JDK/Spring, del propio paquete `.api` y de
  `shared.contract`; no pueden tocar `domain` ni `infrastructure` ni siquiera del mismo modulo.
- `domain` no depende de `application`/`infrastructure`; `application` no depende de
  `infrastructure`.

Las reglas con deuda existente usan `FreezingArchRule`. Baseline actual:

| Regla | Violaciones congeladas |
|---|---:|
| `domain` ajeno | 249 |
| Puertos de persistencia ajenos | 15 |
| `infrastructure` ajena | 37 |
| `shared` hacia modulos | 15 |
| **Total** | **316** |

Antes del Incremento 1 eran 451 (24 shared + 326 domain + 64 persistence + 37 infrastructure).
El Incremento 2A no cambió el baseline (**414**). El Incremento 2B lo redujo a **316**:
`requests` ya no importa `profiles`/`services` `domain` ni `application.ports.output`.

Para actualizar el baseline de forma intencional:

```bash
cd backend/serviya
./mvnw test -Darchunit.freeze.store.default.allowStoreUpdate=true
```

## 4. Siguientes incrementos

| Incremento | Contratos |
|---|---|
| 2A | `profiles.api` + `services.api` (catalog) — **completado**. |
| 2B | Migrar `requests` a las APIs de catalog — **completado**. |
| 3 | `requests.api` + `feedback.api` + `metrics.api`. |
| 4 | `reports.api` (solo lo consume admin). |

`admin` no publica API: es consumidor/orquestador. `config` es la raiz de composicion y puede
cablear implementaciones de cualquier modulo.

## 5. Verificación

Comandos ejecutados el 2026-11-08:

```bash
cd backend/serviya

# Contratos + reglas de arquitectura
./mvnw -Dtest='UserApiFacadeTest,NotificationCommandApiFacadeTest,ProfileApiFacadeTest,ServiceApiFacadeTest,ModuleBoundaryTest' test

# Suite amplia, excluyendo solo las dos clases con fallos ambientales preexistentes
./mvnw -Dtest='!ServiyaApplicationTests,!PiiAttributeConverterTest' test
```

Resultados:

- `UserApiFacadeTest`: 7/7.
- `NotificationCommandApiFacadeTest`: 1/1.
- `ProfileApiFacadeTest`: 11/11.
- `ServiceApiFacadeTest`: 8/8.
- `ModuleBoundaryTest`: 7/7.
- Incremento 2B: `ServiceRequestCommandServiceTest` 33/33, `ServiceRequestQueryServiceTest`
  14/14, `RescheduleProposalServiceTest` 21/21.
- Suite amplia: **409 tests, 0 failures, 0 errors, 14 skipped** (`BUILD SUCCESS`).
- Los 14 skipped son `RedisDistributedLockTest` y `RedisRateLimiterTest`
  (`@Testcontainers(disabledWithoutDocker = true)`).
- La regla de pureza de APIs no es vacua: encontro 9 clases en `users.api`, 3 en
  `notifications.api`, 2 en `shared.contract`, 9 en `profiles.api` y 5 en `services.api`.

Fallos ambientales preexistentes, **no causados por este cambio** (por eso se excluyen de la suite
amplia):

- `ServiyaApplicationTests.contextLoads`: configura H2 pero `pom.xml` no tiene la dependencia
  `com.h2database:h2` (`Cannot load driver class: org.h2.Driver`).
- `PiiAttributeConverterTest` (2 tests): no define `ENCRYPTION_KEY`.

Pendiente recomendado: corregir ambos para que `./mvnw test` completo sea el gate de CI.

## 6. Alcance y límites

- **La migración empezó por `requests`.** Ya no importa `profiles`/`services` `domain` ni
  `application.ports.output`; usa `ProfileQueryApi` y `ServiceQueryApi`. El resto de consumidores
  (`users`, `admin`, `reports`) sigue con sus puertos internos y se migrará en los pasos 3 y 4.
- **`UserSummary` no incluye nombre ni foto.** Son datos de `profiles`; el consumidor/BFF los
  compone con `profiles.api` cuando se migre el listado de admin.
- **`shared` sigue teniendo una violacion conocida:** `PlatformStatsRepository` importa repositorios
  de users/services/requests/feedback. Esta en el baseline (15 lineas) y se resuelve moviendo las
  estadisticas a un modulo de lectura/BFF.
- **Los eventos cambiaron de tipo, no de transporte.** `RequestStatusChangedEvent` y `TagRef` siguen
  siendo eventos in-process (`ApplicationEventPublisher`); no hay broker para ellos todavia. Cuando
  se publiquen por RabbitMQ/Kafka, el contrato ya es serializable a JSON, pero debera versionarse.
- **`metrics` ya no importa `requests.domain`.** Los estados viajan como `String` y el switch interno
  ignora estados desconocidos. Esto prepara la extraccion de metrics sin arrastrar el enum de requests.
- **La arquitectura no esta limpia.** El baseline congelado es un ratchet: impide violaciones nuevas,
  no certifica que las **316** existentes sean correctas.

## 7. Archivos principales

```text
backend/serviya/src/main/java/com/parosurvivors/serviya/
  shared/contract/PageResult.java
  shared/contract/Roles.java
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

backend/serviya/src/test/java/com/parosurvivors/serviya/
  architecture/ModuleBoundaryTest.java
  notifications/application/api/NotificationCommandApiFacadeTest.java
  users/application/api/UserApiFacadeTest.java
  profiles/application/api/ProfileApiFacadeTest.java
  services/application/api/ServiceApiFacadeTest.java

backend/serviya/src/test/resources/
  archunit.properties
  archunit_store/
```
