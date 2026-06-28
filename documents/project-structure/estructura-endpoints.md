# Marketplace de Servicios — Especificación de la API REST

Documento de referencia con la estructura completa de endpoints. Organizado por módulo según la capa de servicios del backend (arquitectura hexagonal, Spring Boot). Cada endpoint referencia el método del servicio que lo implementa y, cuando aplica, el requisito funcional (RF) o no funcional (RNF) del backlog.

---

## Convenciones generales

> **Fuentes**: las convenciones de esta sección se basan en (a) las RFCs oficiales de la IETF para HTTP — RFC 9110 (HTTP Semantics, 2022), RFC 5789 (PATCH, 2010), RFC 9457 (Problem Details, 2023) — y (b) las guías de estilo de API más referenciadas de la industria — Microsoft REST API Guidelines, Google API Design Guide y Zalando RESTful API Guidelines. Detalles y URLs en la sección **Referencias y estándares** al final del documento.

### Estilo de URLs

- **Sustantivos en plural y kebab-case**: `/users`, `/services`, `/service-requests`, `/reschedule-proposals`. Nunca verbos en la URL. *(Microsoft Guidelines §7.3; Zalando §136, §129.)*
- **Prefijo con versión**: todas las rutas viven bajo `/api/v1`. Cuando un cambio rompa el contrato, se crea `/api/v2` y los clientes migran a su ritmo. *(Microsoft Guidelines §12.1; Zalando §115. Existe debate sobre si versionar en URL o en header — esta guía elige URL por pragmatismo.)*
- **Identificador de recurso en el path**: `/users/{id}`, nunca `/users?id=5`. Los identificadores de entidad son numéricos: `BIGINT` en MySQL / `Long` en Java. *(REST: cada recurso tiene URI única — Fielding, 2000.)*
- **Recursos anidados cuando hay pertenencia**: `/users/{userId}/addresses` para las direcciones de un usuario. *(Google API Design Guide — Resource Naming.)*
- **Sub-recursos de acción para operaciones no-CRUD**: `POST /service-requests/{id}/accept` en lugar de un PATCH genérico. Adecuado para transiciones de estado bien definidas (acepta, cancela, completa). *(Google API Design Guide — Custom Methods.)*
- **`/me` para el usuario autenticado**: el backend extrae el id del JWT. Evita repetir el id propio en el path y refuerza la seguridad. *(Convención de facto: GitHub API, Slack API, Spotify Web API, Twitter/X API la usan.)*

### Métodos HTTP

La semántica de cada método HTTP está definida por la IETF en RFC 9110 (HTTP Semantics, junio 2022). PATCH específicamente se define en RFC 5789 (marzo 2010).

| Método | Uso | Cuerpo | Idempotente | Fuente |
|---|---|---|---|---|
| `GET` | Leer recursos | No | Sí | RFC 9110 §9.3.1 |
| `POST` | Crear recurso o ejecutar acción | Sí | No | RFC 9110 §9.3.3 |
| `PUT` | Reemplazar un recurso completo | Sí | Sí | RFC 9110 §9.3.4 |
| `PATCH` | Actualización parcial (solo los campos enviados) | Sí | Generalmente sí | RFC 5789 |
| `DELETE` | Eliminar | Opcional | Sí | RFC 9110 §9.3.5 |

La convención del proyecto (sección B.2 del documento de métodos) es usar **PATCH para los formularios de edición**: el cliente envía solo los campos modificados (dirty fields) y el backend actualiza únicamente los no-nulos. Esto se alinea con el propósito explícito de PATCH definido en RFC 5789 (aplicar un conjunto parcial de cambios al recurso).

### Códigos de respuesta esperados

Los códigos de estado HTTP y su semántica están definidos en RFC 9110 §15.

- `200 OK` — lectura o actualización exitosa con cuerpo de respuesta. *(RFC 9110 §15.3.1)*
- `201 Created` — creación exitosa. Incluye header `Location` apuntando al recurso creado. *(RFC 9110 §15.3.2)*
- `204 No Content` — operación exitosa sin cuerpo (eliminación, acción que no devuelve datos). *(RFC 9110 §15.3.5)*
- `400 Bad Request` — validación de formato fallida (Bean Validation, JSON malformado). *(RFC 9110 §15.5.1)*
- `401 Unauthorized` — no autenticado (JWT ausente, inválido o expirado). *(RFC 9110 §15.5.2)*
- `403 Forbidden` — autenticado pero sin permiso (rol insuficiente, o no es dueño del recurso). Aquí caen los `UnauthorizedException` de la verificación de propiedad (sección C.1). *(RFC 9110 §15.5.4)*
- `404 Not Found` — el recurso no existe. *(RFC 9110 §15.5.5)*
- `409 Conflict` — transición de estado inválida (`InvalidStateException`), violación de unicidad (email duplicado, doble calificación). *(RFC 9110 §15.5.10)*
- `422 Unprocessable Content` — validación de negocio fallida (alternativa a 400 cuando el formato es correcto pero la lógica de negocio rechaza el dato). *(RFC 9110 §15.5.21)*
- `500 Internal Server Error` — error no controlado. La red de seguridad del `@RestControllerAdvice` lo emite cuando una excepción inesperada escapa. *(RFC 9110 §15.6.1)*

### Filtrado, búsqueda y paginación

- **Filtros y búsqueda**: query params (`?status=PENDING&category=plumbing`). *(Microsoft Guidelines §9.)*
- **Paginación**: `?page=0&size=20`. Default razonable: `size=20`, máximo `size=100`. *(Convención de Spring Data Web mediante `Pageable`; ver `org.springframework.data.web.PageableHandlerMethodArgumentResolver`.)*
- **Ordenamiento**: `?sort=fieldName,direction` (ej. `?sort=createdAt,desc`). Va dentro del `Pageable` de Spring Data; no es un parámetro aparte.
- **Listas de valores**: se pasan repetidos (`?status=PENDING&status=ACCEPTED`) o separados por coma (`?status=PENDING,ACCEPTED`). El backend acepta ambas formas; usamos coma por simplicidad.

### Autenticación y autorización

- Todos los endpoints excepto `/api/v1/auth/**` requieren JWT en el header `Authorization: Bearer <token>`. *(El esquema Bearer está definido en RFC 6750; el formato JWT en RFC 7519.)*
- Los roles del sistema son `CLIENT`, `OFFERER`, `ADMIN`. Un mismo usuario puede tener varios roles (RF-010, RF-011).
- La autorización por rol la maneja Spring Security con `@PreAuthorize` en los controllers.
- La verificación de propiedad (recurso pertenece al solicitante) vive en los servicios, usando el `requesterId` extraído del JWT (sección C.1). **Nunca** se confía en un id que venga en el body.
- Un usuario con `isBanned = true` no puede acceder a ninguna función (RNF-007); el filtro de Spring Security lo rechaza antes de llegar a los controllers.

### Formato de respuesta de error

Todas las respuestas de error siguen el mismo formato JSON:

```json
{
  "timestamp": "2026-06-01T14:32:11Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validación fallida",
  "fieldErrors": {
    "email": "no es un correo válido",
    "password": "debe tener al menos 8 caracteres"
  },
  "path": "/api/v1/auth/register"
}
```

El campo `fieldErrors` solo aparece en errores de validación.

> **Recomendación a futuro — alinearse con RFC 9457 (Problem Details)**: existe un estándar IETF formal para respuestas de error en APIs HTTP, **RFC 9457 — Problem Details for HTTP APIs** (julio 2023, reemplaza a RFC 7807). Define el media type `application/problem+json` con campos `type`, `title`, `status`, `detail`, `instance`. Spring Boot 3 incluye soporte nativo mediante la clase `ProblemDetail` y el `ProblemDetailsExceptionHandler`. El formato actual de este documento es compatible en espíritu pero no idéntico al estándar.
>
> La decisión actual del equipo es mantener el formato genérico de arriba. Adoptar RFC 9457 más adelante daría interoperabilidad con clientes y librerías que lo entienden out-of-the-box; queda como mejora futura a discutir con el equipo. Si se adopta, el formato sería:
>
> ```json
> {
>   "type": "https://api.marketplace.example/problems/validation-error",
>   "title": "Validación fallida",
>   "status": 400,
>   "detail": "Uno o más campos no pasaron la validación",
>   "instance": "/api/v1/auth/register",
>   "errors": {
>     "email": "no es un correo válido",
>     "password": "debe tener al menos 8 caracteres"
>   }
> }
> ```

---

## 1. Autenticación

`UserAuthenticationService` · `PasswordResetTokenService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `register(RegisterDTO, roleName)` | Público | RF-002, RF-004 |
| `POST` | `/api/v1/auth/login` | `login(email, rawPassword)` | Público | RF-001 |
| `POST` | `/api/v1/auth/password-reset` | `requestPasswordReset(email)` | Público | RF-003 |
| `POST` | `/api/v1/auth/password-reset/confirm` | `confirmPasswordReset(rawToken, newPassword)` | Público (con token) | RF-003 |

**Notas**:
- `register` recibe el rol deseado (`CLIENT` u `OFFERER` — nunca `ADMIN`) en el body. Valida la entrada y delega la creación común (credenciales + perfil + roles + consentimiento) en `UserCreationService.createUserAccount`. Devuelve el JWT.
- `password-reset/confirm` recibe el token en el body, no en query, porque es sensible (no debe quedar en logs de URL).

---

## 2. Gestión de cuenta

`UserService` · `UserDeletionService` · `UserRoleService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `PATCH` | `/api/v1/users/me/password` | `changePassword(userId, currentRaw, newRaw)` | Logged | RF-007 |
| `PATCH` | `/api/v1/users/me/email` | `changeEmail(userId, newEmail)` | Logged | RF-007 |
| `DELETE` | `/api/v1/users/me` | `UserDeletionService.deleteUser(userId)` | Logged | RF-008 |
| `POST` | `/api/v1/users/me/roles/offerer` | `acquireRole(userId, OFFERER)` | Logged (CLIENT) | RF-010 |
| `POST` | `/api/v1/users/me/roles/client` | `acquireRole(userId, CLIENT)` | Logged (OFFERER) | RF-011 |
| `GET` | `/api/v1/users/me/roles` | `getUserRoles(userId)` | Logged | — |

**Notas**:
- La auto-asignación de rol usa `acquireRole`, que valida que el rol sea `CLIENT` u `OFFERER` (nunca `ADMIN`) y que el usuario se lo asigne a sí mismo; internamente delega en el mecanismo de bajo nivel `assignRole`.

---

## 3. Perfil personal

`UserProfileService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/users/me/profile` | `getProfileInfo(userId)` | Logged | RF-005 |
| `PATCH` | `/api/v1/users/me/profile` | `patchProfile(userId, PatchProfileDTO)` | Logged | RF-006 |
| `PATCH` | `/api/v1/users/me/main-address` | `updateMainAddress(userId, addressId)` | Logged | — |

**Notas**:
- `patchProfile` actualiza solo los campos no-nulos enviados (PATCH semántico). Campos editables: `fullName`, `phone`, `photoUrl`, `description`. `documentType` y `documentNumber` NO son editables.
- `phone` se cifra con AES-256-GCM antes de persistir (RNF-005).

---

## 4. Direcciones

`AddressService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/users/me/addresses` | `getUserAddresses(userId)` | Logged | RF-009 |
| `POST` | `/api/v1/users/me/addresses` | `createAddress(userId, CreateAddressDTO)` | Logged | RF-009 |
| `PATCH` | `/api/v1/addresses/{id}` | `updateAddress(addressId, PatchAddressDTO)` | Dueño | RF-009 |
| `DELETE` | `/api/v1/addresses/{id}` | `deleteAddress(userId, addressId)` | Dueño | RF-009 |
| `POST` | `/api/v1/addresses/verify` | `verifyAddress + getCoordinates` | Logged | RNF-019 |

**Notas**:
- `POST /addresses/verify` recibe `{ addressLine, city }` y devuelve `{ valid, latitude, longitude }`. Usado por el frontend para validar direcciones en formularios antes de crearlas. (RF-009 + RNF-019)
- La línea de dirección se cifra con AES-256-GCM antes de persistir (RNF-005).
- No se permite eliminar la dirección principal activa.

---

## 5. Perfil de oferente

`OffererProfileService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/offerers/{id}` | `getPublicProfile(userId)` | Público | RF-015, RF-027 |
| `GET` | `/api/v1/offerers/{id}/summary` | `getProfileSummary(userId)` | Público | — |
| `PATCH` | `/api/v1/offerers/me` | `patchOffererProfile(userId, PatchOffererProfileDTO)` | OFFERER | RF-015, RF-012 |

**Notas**:
- Campos editables: `whatsappNumber`, `publicDescription`, `specialty`. El frontend arma el enlace de WhatsApp a partir del número (RF-026).

---

## 6. Categorías

`CategoryService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/categories` | `getCategories()` | Público | — |

---

## 7. Servicios — gestión y consulta

`ServiceManagementService` · `ServiceQueryService` · `ServiceSearchService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/services` | `searchServices(filters, pageable)` | Público | RF-024, RF-025 |
| `GET` | `/api/v1/services/{id}` | `getServiceDetail(serviceId)` | Público | RF-028 |
| `GET` | `/api/v1/services/{id}/summary` | `getServiceSummary(serviceId)` | Público | — |
| `POST` | `/api/v1/services` | `createService(offererId, CreateServiceDTO)` | OFFERER | RF-013 |
| `PATCH` | `/api/v1/services/{id}` | `patchService(serviceId, PatchServiceDTO)` | Dueño (OFFERER) | RF-013 |
| `DELETE` | `/api/v1/services/{id}` | `deleteService(serviceId)` | Dueño (OFFERER) | RF-013 |
| `POST` | `/api/v1/services/{id}/activate` | `activateService(serviceId)` | Dueño (OFFERER) | RF-076 |
| `POST` | `/api/v1/services/{id}/deactivate` | `deactivateService(serviceId)` | Dueño (OFFERER) | RF-076 |
| `GET` | `/api/v1/offerers/{id}/services` | `getServicesByOfferer(offererId)` | Público | — |

**Filtros del buscador** (`GET /api/v1/services`, query params):
`name`, `categoryId`, `offererId`, `offererType` (NATURAL/COMPANY), `minPrice`, `maxPrice`, `maxDuration`, `lat`, `lon`, `maxDistanceKm`, `minRating`, `dayOfWeek`, `timeFrom`, `timeTo`, `active`. Paginado y ordenable.

---

## 8. Horario del servicio

`ServiceAvailabilityService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/services/{id}/schedule` | `getServiceSchedule(serviceId)` | Público | — |
| `PUT` | `/api/v1/services/{id}/schedule` | `setServiceSchedule(serviceId, slots)` | Dueño | RF-021 |
| `DELETE` | `/api/v1/services/{id}/schedule/slots/{slotId}` | `deleteSlot(slotId)` | Dueño | RF-021 |
| `POST` | `/api/v1/services/{id}/schedule/slots/{slotId}/activate` | `activateSlot(slotId)` | Dueño | RF-021 |
| `POST` | `/api/v1/services/{id}/schedule/slots/{slotId}/deactivate` | `deactivateSlot(slotId)` | Dueño | RF-021 |
| `POST` | `/api/v1/services/{id}/schedule/apply-offerer-template` | `applyOffererTemplate(serviceId, offererId)` | Dueño (OFFERER) | RF-021 |

**Nota**: `PUT` reemplaza el horario completo (borra + inserta atómico); la creación y edición de franjas se hace por ese reemplazo masivo. Sobre slots individuales solo quedan eliminar y activar/desactivar (se retiraron `createSlot`/`updateSlot`). `apply-offerer-template` inicializa el horario del servicio a partir de la plantilla general del oferente.

---

## 9. Disponibilidad general del oferente

`OffererAvailabilityService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/offerers/me/availability` | `getSchedule(offererId)` | OFFERER | — |
| `PUT` | `/api/v1/offerers/me/availability` | `setSchedule(offererId, slots)` | OFFERER | RF-072 |
| `DELETE` | `/api/v1/offerers/me/availability/slots/{id}` | `deleteSlot(slotId)` | Dueño | RF-072 |
| `POST` | `/api/v1/offerers/me/availability/slots/{id}/activate` | `activateSlot(slotId)` | Dueño | RF-072 |
| `POST` | `/api/v1/offerers/me/availability/slots/{id}/deactivate` | `deactivateSlot(slotId)` | Dueño | RF-072 |

**Nota**: la creación y edición de franjas se hace con `PUT` (reemplazo masivo); se retiraron `createSlot`/`updateSlot`. Sobre slots individuales solo quedan eliminar y activar/desactivar.

---

## 10. Solicitudes de servicio

`ServiceRequestQueryService` · `ServiceRequestCommandService`

### Consultas

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/users/me/client-requests` | `getClientRequests(clientId, statuses, pageable)` | CLIENT | RF-030, RF-032, RF-038 |
| `GET` | `/api/v1/users/me/offerer-requests` | `getOffererRequests(offererId, statuses, pageable)` | OFFERER | RF-016, RF-020, RF-039 |
| `GET` | `/api/v1/service-requests/{id}` | `getRequestDetailForParty(requestId, requesterId)` | Parte involucrada | RF-030 |
| `GET` | `/api/v1/service-requests/{id}/history` | `getRequestHistory(requestId)` | Parte involucrada | — |

**Filtros de listado** (query params): `status` (lista de estados separados por coma, ej. `?status=PENDING,ACCEPTED`). Para "activas" se pasa `PENDING,ACCEPTED`; para "historial" se pasa `COMPLETED,NOT_PROVIDED,REJECTED,CANCELLED`.

### Comandos (transiciones de estado)

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/service-requests` | `createRequest(clientId, serviceId, addressId, scheduledDate)` | CLIENT | RF-029 |
| `POST` | `/api/v1/service-requests/{id}/accept` | `acceptRequest(requestId, offererId)` | Dueño (OFFERER) | RF-017 |
| `POST` | `/api/v1/service-requests/{id}/reject` | `rejectRequest(requestId, offererId)` | Dueño (OFFERER) | RF-018 |
| `POST` | `/api/v1/service-requests/{id}/cancel` | `cancelRequest(requestId, userId)` | Parte involucrada | RF-022, RF-031 |
| `POST` | `/api/v1/service-requests/{id}/mark-completed` | `markAsPresumablyCompleted(requestId, offererId)` | Dueño (OFFERER) | RF-019 |
| `POST` | `/api/v1/service-requests/{id}/confirm-completion` | `confirmCompletion(requestId, clientId)` | Dueño (CLIENT) | RF-037 |
| `POST` | `/api/v1/service-requests/{id}/mark-not-provided` | `markAsNotProvided(requestId, userId)` | Parte involucrada | RF-073 |
| `POST` | `/api/v1/service-requests/{id}/reschedule` | `rescheduleRequest(requestId, newDate)` | Dueño (CLIENT) | RF-033 |

**Notas**:
- Reprogramar crea una NUEVA solicitud enlazada vía `previousRequestId`. La anterior queda en estado de cierre.
- Cada transición valida el estado actual (máquina de estados en el dominio). Si la transición no es válida desde el estado actual, lanza excepción → `409 Conflict`.

---

## 11. Propuestas de reprogramación

`RescheduleProposalService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/reschedule-proposals` | `createProposal(requestId, offererId, reason, proposedDate)` | OFFERER (dueño req.) | RF-023 |
| `GET` | `/api/v1/users/me/proposals/received` | `getProposalsForClient(clientId, statuses)` | CLIENT | RF-034 |
| `GET` | `/api/v1/users/me/proposals/sent` | `getProposalsByOfferer(offererId, statuses)` | OFFERER | — |
| `GET` | `/api/v1/service-requests/{id}/proposals` | `getProposalsByRequest(requestId)` | Parte involucrada | — |
| `POST` | `/api/v1/reschedule-proposals/{id}/accept` | `acceptProposal(proposalId, clientId, confirmedDate)` | Dueño (CLIENT) | RF-035 |
| `POST` | `/api/v1/reschedule-proposals/{id}/reject` | `rejectProposal(proposalId, clientId)` | Dueño (CLIENT) | RF-036 |
| `POST` | `/api/v1/reschedule-proposals/{id}/cancel` | `cancelProposal(proposalId, offererId)` | Dueño (OFFERER) | — |

---

## 12. Calificaciones y reseñas del servicio

`ServiceFeedbackService` · `ServiceFeedbackTagCatalogService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/service-requests/{id}/feedback` | `submitServiceFeedback(clientId, requestId, rating, review)` | CLIENT | RF-041, RF-045 |
| `GET` | `/api/v1/service-requests/{id}/feedback` | `getServiceFeedback(requestId)` | Parte involucrada | — |
| `GET` | `/api/v1/services/{id}/feedback` | `getServiceFeedbackList(serviceId, pageable)` | Público | RF-040, RF-046 |
| `GET` | `/api/v1/clients/{id}/service-feedback` | `getServiceFeedbackByClient(clientId, pageable)` | Dueño (propio) / ADMIN | — |
| `GET` | `/api/v1/service-feedback-tags` | `getCatalog()` | Público | — |

**Notas**:
- Un solo POST agrupa rating + review (cualquiera puede venir null; si ambos vienen null, no hace nada).
- Solo se permite calificar desde el estado "presuntamente cumplido" en adelante.
- `GET /clients/{id}/service-feedback` lista todo el feedback que un cliente ha **dejado** sobre servicios (su historial como reseñador), distinto de `/services/{id}/feedback` que es el feedback **recibido** por un servicio.

---

## 13. Calificaciones y reseñas al cliente

`ClientFeedbackService` · `ClientFeedbackTagCatalogService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/service-requests/{id}/client-feedback` | `submitClientFeedback(offererId, requestId, clientId, rating, review)` | OFFERER | RF-043, RF-044 |
| `GET` | `/api/v1/service-requests/{id}/client-feedback` | `getClientFeedback(requestId)` | Parte involucrada | — |
| `GET` | `/api/v1/users/{id}/client-feedback` | `getClientFeedbackList(clientId, pageable)` | OFFERER (con solicitud común) / ADMIN | RF-047 |
| `GET` | `/api/v1/offerers/{id}/client-feedback` | `getClientFeedbackByOfferer(offererId, pageable)` | Dueño (propio) / ADMIN | — |
| `GET` | `/api/v1/client-feedback-tags` | `getCatalog()` | Público | — |

**Nota**: `GET /offerers/{id}/client-feedback` lista todo el feedback que un oferente ha **dejado** sobre clientes (su historial como reseñador), distinto de `/users/{id}/client-feedback` que es el feedback **recibido** por un cliente.

---

## 14. Métricas

`ServiceMetricsService` · `OffererMetricsService` · `ClientMetricsService` · `*TagMetricsService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/services/{id}/metrics` | `getMetrics(serviceId)` | Público | RF-040 |
| `GET` | `/api/v1/services/{id}/tag-metrics` | `ServiceTagMetricsService.getTagMetrics(serviceId)` | Público | — |
| `GET` | `/api/v1/offerers/{id}/metrics` | `OffererMetricsService.getAllMetrics(offererId)` | Logged | RF-042, RF-053 |
| `GET` | `/api/v1/offerers/{id}/metrics/main` | `getMainMetrics(offererId)` | Público | RF-042, RF-053 |
| `GET` | `/api/v1/offerers/{id}/tag-metrics` | `OffererTagMetricsService.getTagMetrics(offererId)` | Público | — |
| `GET` | `/api/v1/clients/{id}/metrics` | `ClientMetricsService.getAllMetrics(clientId)` | OFFERER (con solicitud común) / ADMIN | RF-054 |
| `GET` | `/api/v1/clients/{id}/metrics/main` | `getMainMetrics(clientId)` | OFFERER (con solicitud común) / ADMIN | RF-054 |
| `GET` | `/api/v1/clients/{id}/tag-metrics` | `ClientTagMetricsService.getTagMetrics(clientId)` | OFFERER (con solicitud común) / ADMIN | — |
| `GET` | `/api/v1/users/me/metrics` | (delega según roles del usuario) | Logged | RF-051, RF-052 |

**Nota**: `/users/me/metrics` devuelve un objeto con `offererMetrics` y/o `clientMetrics` según los roles del usuario autenticado.

---

## 15. Reportes

`ReportService` · `RequestReportService` · `ServiceFeedbackReportService` · `ClientFeedbackReportService` · `ReportActionService`

### Creación

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/reports/requests` | `RequestReportService.createReport(...)` | Parte involucrada | RF-055, RF-057, RF-073 |
| `POST` | `/api/v1/reports/service-feedback` | `ServiceFeedbackReportService.createReport(...)` | Logged | RF-056 |
| `POST` | `/api/v1/reports/client-feedback` | `ClientFeedbackReportService.createReport(...)` | Logged | RF-056 |

### Consulta

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/reports` | `ReportService.getReports(type, category, status, pageable)` | ADMIN | RF-058 |
| `GET` | `/api/v1/reports/{id}` | (dispatch por tipo, devuelve detalle del subtipo) | ADMIN | RF-058 |
| `GET` | `/api/v1/reports/{id}/actions` | `ReportActionService.getActionsByReport(reportId)` | ADMIN | RF-071 |
| `GET` | `/api/v1/users/{id}/reports/received` | `ReportService.getReportsByReportedUser(userId)` | ADMIN | RF-081 |
| `GET` | `/api/v1/users/{id}/reports/sent` | `ReportService.getReportsByReporter(userId)` | ADMIN | RF-081 |

---

## 16. Moderación (acciones de admin sobre reportes)

`ModerationService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `POST` | `/api/v1/reports/{id}/actions/warn` | `warnUser(reportId, adminId)` | ADMIN | RF-060, RF-071 |
| `POST` | `/api/v1/reports/{id}/actions/ban` | `banUserFromReport(reportId, adminId)` | ADMIN | RF-069, RF-063 |
| `POST` | `/api/v1/reports/{id}/actions/revert-feedback` | `revertFeedbackFromReport(reportId, adminId)` | ADMIN | RF-049 |
| `POST` | `/api/v1/reports/{id}/actions/close` | `closeReport(reportId, adminId)` | ADMIN | RF-059 |
| `POST` | `/api/v1/reports/{id}/actions/mark-not-provided` | `markRequestAsNotProvided(reportId, adminId)` | ADMIN | RF-074 |

**Notas**:
- Todas las acciones reciben el `reportId` y, tras ejecutar, cierran el reporte (paso común `finalizeReport`).
- `revert-feedback` (`revertFeedbackFromReport`) delega en la fachada de feedback (`FeedbackFlow.remove`), que borra el rating/reseña y publica los eventos de borrado. `mark-not-provided` delega el cambio de estado en `ServiceRequestCommandService.markAsNotProvided`.

---

## 17. Administración

`AdminService` · `UserRoleService` · `RoleService`

### Usuarios

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/users` | `searchUsers(filters, pageable)` | ADMIN | RF-068 |
| `GET` | `/api/v1/admin/users/{id}` | `getUserAdminDetail(userId)` | ADMIN | RF-081 |
| `POST` | `/api/v1/admin/users` | `createUserByAdmin(adminId, CreateUserDTO, roleName)` | ADMIN | — |
| `POST` | `/api/v1/admin/users/{id}/ban` | `banUser(adminId, userId)` | ADMIN | RF-069, RF-063 |
| `POST` | `/api/v1/admin/users/{id}/unban` | `unbanUser(adminId, userId)` | ADMIN | RF-070, RF-075 |
| `DELETE` | `/api/v1/admin/users/{id}` | `deleteUser(adminId, userId)` | ADMIN | RF-068 |

### Roles

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/roles` | `RoleService.getRoles()` | ADMIN | — |
| `GET` | `/api/v1/admin/users/{id}/roles` | `getUserRoles(userId)` | ADMIN | RF-067 |
| `POST` | `/api/v1/admin/users/{id}/roles` | `assignRole(userId, roleId)` | ADMIN | RF-065 |
| `POST` | `/api/v1/admin/users/{id}/roles/admin` | `grantAdminRole(adminId, userId)` | ADMIN | — |
| `DELETE` | `/api/v1/admin/users/{id}/roles/{roleId}` | `removeRole(userId, roleId)` | ADMIN | RF-066 |

**Nota**: la asignación genérica de roles del admin cubre `CLIENT`/`OFFERER`. La **promoción** de un usuario existente a `ADMIN` va por el método dedicado `grantAdminRole` (`POST .../roles/admin`), que hace comprobaciones y delega en `assignRole`; junto con `createUserByAdmin` (cuenta nueva) son los dos caminos hacia ADMIN. La auto-asignación de los usuarios (sección 2) está limitada a `CLIENT`/`OFFERER`.

### Servicios y reseñas (acciones directas)

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `DELETE` | `/api/v1/admin/services/{id}` | `ServiceManagementService.deleteService(serviceId)` | ADMIN | RF-064 |
| `GET` | `/api/v1/admin/feedback` | (búsqueda combinada; reutiliza los listados de feedback de las secciones 12–13) | ADMIN | RF-048 |
| `POST` | `/api/v1/admin/feedback/remove` | `removeFeedbackDirectly(adminId, CreateReportDTO)` | ADMIN | RF-049 |

**Nota**: la eliminación de una reseña inapropiada (RF-049) tiene dos vías, ambas pasan por el flujo de feedback (`FeedbackFlow.remove`) y dejan rastro de auditoría (reporte + `ReportAction`): (a) si la reseña fue reportada, la acción de moderación `POST /reports/{id}/actions/revert-feedback` (sección 16); (b) si no fue reportada, `POST /admin/feedback/remove` → `removeFeedbackDirectly`, que crea internamente un reporte (admin como reportante) y lo resuelve con `revertFeedbackFromReport`. Se retiraron los `DELETE /admin/feedback/...` directos.

---

## 18. Notificaciones

`NotificationDeliveryService` · `NotificationChannelService`

| Método | Ruta | Servicio | Acceso | Requisito |
|---|---|---|---|---|
| `GET` | `/api/v1/notifications` | `getDeliveries(userId, read, channelId, status, pageable)` | Logged | RF-061, RF-062, RF-082–RF-089 |
| `POST` | `/api/v1/notifications/{id}/read` | `markAsRead(deliveryId)` | Dueño | — |
| `GET` | `/api/v1/notification-channels` | `getChannels()` | Logged | — |

**Notas**:
- `NotificationService.notify` y `NotificationDeliveryService.deliver` son **internos**; no se exponen como endpoints. Los disparan los servicios cuando ocurre un evento de negocio.
- Filtros del listado: `read` (true/false), `channelId`, `status` (PENDING/SENT/FAILED).
- El backlog actualizado desglosó las notificaciones por evento. Al **oferente**: `RF-061` (nueva solicitud), `RF-082` (cliente reprograma), `RF-083` (cliente cancela) y `RF-084` (reporte en su contra). Al **cliente**: `RF-062` (solicitud aceptada), `RF-085` (rechazada), `RF-086` (el oferente cancela), `RF-087` (propuesta de reprogramación), `RF-088` (reporte en su contra) y `RF-089` (servicio completado). Como la entrega es interna, `GET /notifications` es el único endpoint que las expone y por eso cubre todo ese conjunto (`RF-061, RF-062, RF-082–RF-089`).

---

## 19. Endpoint de salud (Actuator)

| Método | Ruta | Acceso | Notas |
|---|---|---|---|
| `GET` | `/actuator/health` | Público | Health check para balanceadores de carga y Kubernetes. Devuelve `{ "status": "UP" }` cuando todo está sano. |
| `GET` | `/actuator/info` | Público | Información básica (versión, build). |

Resto de endpoints de Actuator (metrics, env, loggers) deshabilitados por defecto. Se habilitan selectivamente según necesidad de observabilidad.

---

## Resumen — métodos HTTP por tipo de operación

Esta tabla ayuda a decidir qué método usar cuando agregues nuevos endpoints:

| Operación | Método | Ejemplo |
|---|---|---|
| Listar/buscar con filtros | `GET` | `GET /services?category=...` |
| Ver un recurso | `GET` | `GET /services/{id}` |
| Crear un recurso | `POST` | `POST /services` |
| Actualización parcial (formulario) | `PATCH` | `PATCH /services/{id}` |
| Reemplazo total (caso raro, ej. horario completo) | `PUT` | `PUT /services/{id}/schedule` |
| Eliminar | `DELETE` | `DELETE /services/{id}` |
| Transición de estado | `POST` sobre sub-recurso de acción | `POST /service-requests/{id}/accept` |
| Activar/desactivar (toggle) | `POST` sobre sub-recurso | `POST /services/{id}/activate` |
| Acción puntual no-CRUD | `POST` sobre sub-recurso | `POST /addresses/verify` |

---

## Referencias y estándares

Las convenciones aplicadas en este documento provienen de tres niveles de fuentes: el documento fundacional de REST, los estándares oficiales de HTTP publicados por la IETF, y las guías de estilo de organizaciones reconocidas de la industria. A continuación, ordenadas por categoría.

### Fundamento arquitectónico

REST no es un protocolo ni un estándar técnico, sino un **estilo arquitectónico** propuesto por Roy Fielding (coautor de las primeras especificaciones de HTTP) en su tesis doctoral del año 2000:

- **Fielding, R. T. (2000).** *Architectural Styles and the Design of Network-based Software Architectures*. PhD Dissertation, University of California, Irvine. Capítulo 5 ("Representational State Transfer"). [https://ics.uci.edu/~fielding/pubs/dissertation/top.htm](https://ics.uci.edu/~fielding/pubs/dissertation/top.htm)

### Estándares oficiales — IETF (Internet Engineering Task Force)

Los siguientes documentos son **RFCs** (Request for Comments), las normas técnicas oficiales que rigen los protocolos de Internet. Son los únicos elementos de esta lista con autoridad normativa real:

- **RFC 9110 — HTTP Semantics** (junio 2022). Define la semántica de HTTP independiente de la versión del protocolo: métodos, códigos de estado, headers, idempotencia, safety. **Reemplaza la serie RFC 7230–7235 (2014)**. [https://www.rfc-editor.org/rfc/rfc9110.html](https://www.rfc-editor.org/rfc/rfc9110.html)
- **RFC 9111 — HTTP Caching** (junio 2022). Caché de respuestas HTTP. [https://www.rfc-editor.org/rfc/rfc9111.html](https://www.rfc-editor.org/rfc/rfc9111.html)
- **RFC 5789 — PATCH Method for HTTP** (marzo 2010). Define el método PATCH y su uso para actualizaciones parciales. [https://www.rfc-editor.org/rfc/rfc5789.html](https://www.rfc-editor.org/rfc/rfc5789.html)
- **RFC 9457 — Problem Details for HTTP APIs** (julio 2023). Formato estándar para respuestas de error con el media type `application/problem+json`. **Reemplaza a RFC 7807 (2016)**. [https://www.rfc-editor.org/rfc/rfc9457.html](https://www.rfc-editor.org/rfc/rfc9457.html)
- **RFC 6750 — OAuth 2.0 Bearer Token Usage** (octubre 2012). Define el esquema `Authorization: Bearer <token>`. [https://www.rfc-editor.org/rfc/rfc6750.html](https://www.rfc-editor.org/rfc/rfc6750.html)
- **RFC 7519 — JSON Web Token (JWT)** (mayo 2015). Formato del token usado para autenticación. [https://www.rfc-editor.org/rfc/rfc7519.html](https://www.rfc-editor.org/rfc/rfc7519.html)
- **RFC 3986 — Uniform Resource Identifier (URI): Generic Syntax** (enero 2005). Sintaxis general de las URIs/URLs. [https://www.rfc-editor.org/rfc/rfc3986.html](https://www.rfc-editor.org/rfc/rfc3986.html)

### Guías de estilo de la industria

No existe un estándar IETF que rija el diseño de endpoints (nombres de recursos, anidamiento, versionado). Las siguientes guías son las referencias más citadas y de mayor influencia en la industria. Cuando dos o más coinciden, lo consideramos práctica de facto:

- **Microsoft REST API Guidelines**. Las guías internas de Microsoft, publicadas abiertamente como referencia para la comunidad. Cubren naming, métodos, versionado, paginación, filtrado, manejo de errores. [https://github.com/microsoft/api-guidelines](https://github.com/microsoft/api-guidelines)
- **Google Cloud API Design Guide**. Guía de Google para el diseño de APIs (REST y gRPC). Es especialmente fuerte en modelado de recursos y métodos personalizados (custom methods). [https://cloud.google.com/apis/design](https://cloud.google.com/apis/design)
- **Zalando RESTful API Guidelines**. Una de las guías más completas y respetadas; publicada por Zalando (la mayor tienda online de moda de Europa). Estructurada en reglas con MUST/SHOULD/COULD según RFC 2119. [https://opensource.zalando.com/restful-api-guidelines/](https://opensource.zalando.com/restful-api-guidelines/)
- **PayPal API Style Guide**. Guía pública de PayPal, con énfasis en consistencia y soporte de versiones. [https://github.com/paypal/api-standards](https://github.com/paypal/api-standards)
- **JSON:API Specification**. Especificación de comunidad para estructura de payloads JSON, incluye convenciones de paginación, relaciones, filtros. Más opinada que las anteriores; no la seguimos al pie de la letra en este proyecto. [https://jsonapi.org/](https://jsonapi.org/)

### Especificación de documentación de APIs

- **OpenAPI Specification (OAS) 3.1**. Estándar de la OpenAPI Initiative (bajo la Linux Foundation) para describir APIs HTTP. Es el formato que usaremos para documentar la API; Spring Boot lo integra mediante `springdoc-openapi`. [https://spec.openapis.org/oas/latest.html](https://spec.openapis.org/oas/latest.html)

### Libros de referencia

Aunque no son estándares, son lecturas clásicas que sintetizan las prácticas anteriores:

- **Masse, M. (2011).** *REST API Design Rulebook*. O'Reilly. Reglas concretas para nombrado, métodos y versionado.
- **Richardson, L., Amundsen, M., & Ruby, S. (2013).** *RESTful Web APIs*. O'Reilly. Una de las referencias más completas sobre diseño REST e hipermedia.

### Convenciones adicionales mencionadas en el documento

- El uso del path `/me` para el usuario autenticado es una convención de facto adoptada por las APIs públicas más usadas: GitHub (`/user`), Spotify (`/me`), Slack (`auth.test`), Twitter/X (`/users/me`). No está en una RFC ni en una guía oficial, pero es ampliamente reconocida y simplifica tanto la URL como la verificación de autoría.
- El patrón de **sub-recursos de acción** (`POST /resource/{id}/action`) sigue la sección "Custom Methods" de la Google API Design Guide. Es la forma idiomática de modelar operaciones que no encajan en CRUD puro y que el proyecto usa para todas las transiciones de estado de las solicitudes de servicio.

---

## Próximos pasos sugeridos

1. **Definir los DTOs de entrada y salida** asociados a cada endpoint (records con sus campos y validaciones Bean Validation).
2. **Definir el formato del JWT** y los claims que llevará (userId, roles, expiración).
3. **Configurar Spring Security** con el filtro JWT y las reglas de autorización por endpoint.
4. **Crear el `@RestControllerAdvice` global** con los handlers para las excepciones del dominio (`UnauthorizedException`, `InvalidStateException`, `NotFoundException`, `ConflictException`, `MethodArgumentNotValidException`).
5. **Documentar la API con OpenAPI/Swagger** (springdoc-openapi) para que el frontend tenga referencia interactiva.
