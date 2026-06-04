# Diagrama de Clases de Servicio — Marketplace de Servicios

Diagrama de la **capa de servicios** (capa de aplicación), organizado por los 8 módulos del sistema.
Cada bloque es un diagrama Mermaid independiente y renderizable por separado.

> Cómo verlo renderizado: pega cada bloque en [mermaid.live](https://mermaid.live), o abre este archivo en GitHub / VS Code (con extensión Mermaid).

**Convenciones:**
- `<<orquestador>>` — clase que coordina varios servicios en una transacción.
- `<<scheduled>>` — tareas automáticas (`@Scheduled`).
- `..>` (línea punteada) — relación de dependencia ("usa / depende de").
- Los listeners de eventos se indican con una nota; no son llamadas directas sino reacción a eventos publicados.

---

## Módulo 1 — Auth & Usuarios

```mermaid
classDiagram
    class UserAuthenticationService {
        <<orquestador>>
        +login(email, rawPassword) AuthResponseDTO
        +register(dto, roleName) AuthResponseDTO
        +requestPasswordReset(email) void
        +confirmPasswordReset(rawToken, newPassword) void
    }
    class UserService {
        +createUser(email, rawPassword) User
        +changePassword(userId, currentRaw, newRaw) void
        +changeEmail(userId, newEmail) void
        +banUser(userId) void
        +unbanUser(userId) void
    }
    class UserDeletionService {
        <<orquestador>>
        +deleteUser(userId) void
    }
    class AdminService {
        +createAdmin(requestingAdminId, dto) User
        +searchUsers(filters, pageable) Page~UserSummaryDTO~
        +getUserAdminDetail(userId) UserAdminDetailDTO
        +banUser(adminId, userId) void
        +unbanUser(adminId, userId) void
        +deleteUser(adminId, userId) void
    }
    class RoleService {
        +getRoles() List~Role~
    }
    class UserRoleService {
        +getUserRoles(userId) List~Role~
        +hasRole(userId, roleName) boolean
        +assignRole(userId, roleId) void
        +removeRole(userId, roleId) void
    }
    class ConsentService {
        +createConsent(userId, accepted) Consent
        +hasConsented(userId) boolean
    }
    class PasswordResetTokenService {
        +createToken(userId) PasswordResetToken
        +validateToken(userId, rawToken) TokenValidationResult
        +markTokenAsUsed(tokenId) void
    }

    UserAuthenticationService ..> UserService : usa
    UserAuthenticationService ..> ConsentService : usa
    UserAuthenticationService ..> UserRoleService : usa
    UserAuthenticationService ..> PasswordResetTokenService : usa
    UserAuthenticationService ..> NotificationService : usa
    UserDeletionService ..> UserService : usa
    UserDeletionService ..> ServiceManagementService : usa
    UserDeletionService ..> ServiceRequestCommandService : usa
    AdminService ..> UserService : usa
    AdminService ..> UserDeletionService : usa
    AdminService ..> NotificationService : usa
```

---

## Módulo 2 — Perfiles

```mermaid
classDiagram
    class UserProfileService {
        +getProfileInfo(userId) UserProfileDTO
        +patchProfile(userId, dto) void
        +updateMainAddress(userId, addressId) void
    }
    class AddressService {
        +getUserAddresses(userId) List~Address~
        +createAddress(userId, dto) Address
        +updateAddress(addressId, dto) Address
        +deleteAddress(addressId) void
    }
    class OffererProfileService {
        +getPublicProfile(userId) OffererProfilePublicDTO
        +getProfileSummary(userId) OffererProfileSummaryDTO
        +patchOffererProfile(userId, dto) void
    }
    class OffererAvailabilityService {
        +getSchedule(offererId) List~OffererAvailability~
        +setSchedule(offererId, slots) void
        +createSlot(offererId, dayOfWeek, start, end) OffererAvailability
        +deleteSlot(slotId) void
        +updateSlot(slotId, dayOfWeek, start, end) OffererAvailability
        +activateSlot(slotId) void
        +deactivateSlot(slotId) void
    }

    UserProfileService ..> AddressService : usa (dirección principal)
    AddressService ..> GeocodingPort : usa (coordenadas)
    note for AddressService "patchProfile y createAddress cifran PII (AES-256-GCM)"
```

---

## Módulo 3 — Servicios

```mermaid
classDiagram
    class CategoryService {
        +getCategories() List~Category~
    }
    class ServiceQueryService {
        +getServiceSummary(serviceId) ServiceSummaryDTO
        +getServiceDetail(serviceId) ServiceDetailDTO
        +getServicesByOfferer(offererId) List~ServiceSummaryDTO~
        +countServicesByOfferer(offererId) int
    }
    class ServiceManagementService {
        +createService(offererId, dto) Service
        +deleteService(serviceId) void
        +deactivateAllByOfferer(offererId) void
        +activateService(serviceId) void
        +deactivateService(serviceId) void
        +patchService(serviceId, dto) void
    }
    class ServiceSearchService {
        +searchServices(filters, pageable) Page~ServiceSummaryDTO~
    }
    class ServiceAvailabilityService {
        +getServiceSchedule(serviceId) List~ServiceAvailability~
        +setServiceSchedule(serviceId, slots) void
        +createSlot(serviceId, dayOfWeek, start, end) ServiceAvailability
        +deleteSlot(slotId) void
        +updateSlot(slotId, dayOfWeek, start, end) ServiceAvailability
        +activateSlot(slotId) void
        +deactivateSlot(slotId) void
    }

    ServiceManagementService ..> ServiceMetricsService : crea métricas (init)
    ServiceManagementService ..> ServiceAvailabilityService : crea franjas
    ServiceQueryService ..> ServiceReviewService : reseñas en detalle
    ServiceQueryService ..> OffererProfileService : resumen del oferente
    note for ServiceManagementService "createService es @Transactional: Service + ServiceMetrics + franjas"
```

---

## Módulo 4 — Solicitudes de Servicio

```mermaid
classDiagram
    class ServiceRequestQueryService {
        +getClientRequests(clientId, statuses, pageable) Page~ServiceRequestDTO~
        +getOffererRequests(offererId, statuses, pageable) Page~ServiceRequestDTO~
        +getRequestDetailForParty(requestId, requesterId) ServiceRequestDetailDTO
        +getRequestDetailForAdmin(requestId) AdminRequestDetailDTO
        +getRequestHistory(requestId) List~RequestHistoryDTO~
        +countClientRequests(clientId) int
        +countOffererRequests(offererId) int
    }
    class ServiceRequestCommandService {
        +createRequest(clientId, serviceId, addressId, scheduledDate) ServiceRequest
        +checkServiceAvailability(serviceId, scheduledDate) boolean
        +checkWithinRadius(serviceId, clientAddressId) boolean
        +acceptRequest(requestId, offererId) void
        +rejectRequest(requestId, offererId) void
        +markAsPresumablyCompleted(requestId, offererId) void
        +confirmCompletion(requestId, clientId) void
        +markAsNotProvided(requestId, userId) void
        +cancelRequest(requestId, userId) void
        +rescheduleRequest(requestId, newDate) ServiceRequest
    }
    class RescheduleProposalService {
        +createProposal(requestId, offererId, reason, proposedDate) RescheduleProposal
        +acceptProposal(proposalId, clientId, confirmedDate) ServiceRequest
        +rejectProposal(proposalId, clientId) void
        +cancelProposal(proposalId, offererId) void
        +getProposalsForClient(clientId, statuses) List~RescheduleProposalDTO~
        +getProposalsByOfferer(offererId, statuses) List~RescheduleProposalDTO~
        +getProposalsByRequest(requestId) List~RescheduleProposalDTO~
    }
    class ScheduledTasksService {
        <<scheduled>>
        +rejectExpiredPendingRequests() void
        +markStaleAcceptedAsNotProvided() void
        +rejectExpiredProposals() void
        +finalizeUnconfirmedCompletions() void
    }

    ServiceRequestCommandService ..> ServiceQueryService : valida servicio activo
    ServiceRequestCommandService ..> GeocodingPort : distancia/radio
    RescheduleProposalService ..> ServiceRequestCommandService : acceptProposal → reschedule
    ScheduledTasksService ..> ServiceRequestCommandService : transiciones automáticas
    ScheduledTasksService ..> RescheduleProposalService : rechaza propuestas vencidas
    ScheduledTasksService ..> NotificationService : notifica a las partes
    note for ServiceRequestCommandService "Reprogramar crea nueva solicitud con previousRequestId (cadena). Transiciones publican RequestStatusChangedEvent."
```

---

## Módulo 5 — Reseñas & Calificaciones

```mermaid
classDiagram
    class ServiceFeedbackService {
        <<orquestador>>
        +submitServiceFeedback(clientId, requestId, rating, review) void
        +getServiceFeedback(requestId) ServiceFeedbackDTO
        +getServiceFeedbackList(serviceId, pageable) Page~ServiceFeedbackDTO~
        +revertFeedback(requestId) boolean
    }
    class ServiceRatingService {
        +addRating(clientId, requestId, rating) ServiceRating
        +deleteRating(requestId, clientId) void
    }
    class ServiceReviewService {
        +createReview(clientId, requestId, comment, tagIds) ServiceReview
        +deleteReview(requestId, clientId) void
    }
    class ServiceReviewTagCatalogService {
        +getCatalog() List~ServiceReviewTagCatalog~
    }
    class ClientFeedbackService {
        <<orquestador>>
        +submitClientFeedback(offererId, requestId, clientId, rating, review) void
        +getClientFeedback(requestId) ClientFeedbackDTO
        +getClientFeedbackList(clientId, pageable) Page~ClientFeedbackDTO~
        +revertFeedback(requestId) boolean
    }
    class ClientRatingService {
        +addRating(offererId, requestId, clientId, rating) ClientRating
        +deleteRating(requestId, offererId) void
    }
    class ClientReviewService {
        +createReview(offererId, requestId, comment, tagIds) ClientReview
        +deleteReview(requestId, offererId) void
    }
    class ClientReviewTagCatalogService {
        +getCatalog() List~ClientReviewTagCatalog~
    }

    ServiceFeedbackService ..> ServiceRatingService : crea/consulta rating
    ServiceFeedbackService ..> ServiceReviewService : crea/consulta review
    ClientFeedbackService ..> ClientRatingService : crea/consulta rating
    ClientFeedbackService ..> ClientReviewService : crea/consulta review
    note for ServiceFeedbackService "Valida estado y unicidad. addRating publica ServiceRatedEvent; createReview publica ReviewCreatedEvent / ReviewDeletedEvent (con tags + sentiment)."
```

---

## Módulo 6 — Métricas

```mermaid
classDiagram
    class ServiceMetricsService {
        +getMetrics(serviceId) ServiceMetrics
        +updateMetrics(ServiceRatedEvent) void
        +updateReviewCount(ReviewCreatedEvent) void
        +decrementReviewCount(ReviewDeletedEvent) void
    }
    class ServiceTagMetricsService {
        +getTagMetrics(serviceId) List~ServiceTagMetrics~
        +updateTagMetrics(ReviewCreatedEvent) void
        +decrementTagMetrics(ReviewDeletedEvent) void
    }
    class OffererMetricsService {
        +getAllMetrics(offererId) OffererMetrics
        +getMainMetrics(offererId) OffererMetricsSummaryDTO
        +updateMetrics(ServiceRatedEvent) void
        +updateReviewMetrics(ReviewCreatedEvent) void
        +decrementReviewMetrics(ReviewDeletedEvent) void
        +updateRequestMetrics(RequestStatusChangedEvent) void
    }
    class ClientMetricsService {
        +getAllMetrics(clientId) ClientMetrics
        +getMainMetrics(clientId) ClientMetricsSummaryDTO
        +updateMetrics(ClientRatedEvent) void
        +updateReviewMetrics(ReviewCreatedEvent) void
        +decrementReviewMetrics(ReviewDeletedEvent) void
        +updateRequestMetrics(RequestStatusChangedEvent) void
    }
    class OffererTagMetricsService {
        +getTagMetrics(offererId) List~OffererTagMetrics~
        +updateTagMetrics(ReviewCreatedEvent) void
        +decrementTagMetrics(ReviewDeletedEvent) void
    }
    class ClientTagMetricsService {
        +getTagMetrics(clientId) List~ClientTagMetrics~
        +updateTagMetrics(ReviewCreatedEvent) void
        +decrementTagMetrics(ReviewDeletedEvent) void
    }

    note for ServiceMetricsService "Todos los update* son @EventListener: reaccionan a eventos, no se llaman directamente. Patrón UPSERT en las métricas de tags."
```

---

## Módulo 7 — Reportes

```mermaid
classDiagram
    class ReportService {
        +createBaseReport(reporterId, reportedUserId, type, category, reason) Report
        +getReportDetail(reportId) ReportDetailDTO
        +getReports(type, category, status, pageable) Page~ReportDTO~
        +getReportsByReporter(reporterId) List~ReportDTO~
        +getReportsByReportedUser(reportedUserId) List~ReportDTO~
        +countReportsByReportedUser(reportedUserId) int
        +countReportsByReporter(reporterId) int
    }
    class RequestReportService {
        +createRequestReport(reporterId, reportedUserId, category, reason, requestId) RequestReport
        +getRequestReportDetail(reportId) RequestReportDetailDTO
    }
    class ServiceReviewReportService {
        +createReport(reporterId, reportedUserId, category, reason, serviceReviewId) ServiceReviewReport
        +getReportDetail(reportId) ServiceReviewReportDetailDTO
    }
    class ClientReviewReportService {
        +createReport(reporterId, reportedUserId, category, reason, clientReviewId) ClientReviewReport
        +getReportDetail(reportId) ClientReviewReportDetailDTO
    }
    class ReportActionService {
        +createAction(reportId, adminId, actionTaken) ReportAction
        +getActionsByReport(reportId) List~ReportAction~
    }
    class ModerationService {
        <<orquestador>>
        +warnUser(reportId, adminId) void
        +banUserFromReport(reportId, adminId) void
        +revertFeedback(reportId, adminId) void
        +closeReport(reportId, adminId) void
        -finalizeReport(reportId, adminId, actionText) void
    }

    RequestReportService ..> ReportService : createBaseReport
    ServiceReviewReportService ..> ReportService : createBaseReport
    ClientReviewReportService ..> ReportService : createBaseReport
    ModerationService ..> ReportActionService : registra acción
    ModerationService ..> UserService : banUser
    ModerationService ..> ServiceFeedbackService : revertFeedback (delega)
    ModerationService ..> ClientFeedbackService : revertFeedback (delega)
    ModerationService ..> NotificationService : notifica partes
    note for ModerationService "revertFeedback delega en el FeedbackService correspondiente (servicio o cliente) según el tipo de reporte; recibe boolean de confirmación y notifica al reportado. Cada acción cierra el reporte vía finalizeReport (privado): registra ReportAction + cierra + notifica al reportante."
```

---

## Módulo 8 — Notificaciones

```mermaid
classDiagram
    class NotificationService {
        <<orquestador>>
        +notify(userId, type, title, message, entityType, entityId, channelIds, protectedData) void
        +createNotification(userId, type, title, message, entityType, entityId) Notification
    }
    class NotificationDeliveryService {
        +deliver(notificationId, channelId, protectedData) NotificationDelivery
        +getDeliveries(userId, read, channelId, status, pageable) Page~NotificationDeliveryDTO~
        +getDeliveryStatus(notificationId, channelId) String
        +isRead(notificationId, channelId) boolean
        +markAsRead(deliveryId) void
    }
    class NotificationChannelService {
        +getChannels() List~NotificationChannel~
    }

    NotificationService ..> NotificationDeliveryService : despacha por canal
    NotificationDeliveryService ..> EmailPort : canal EMAIL (puerto salida)
    NotificationDeliveryService ..> PushNotificationPort : canal PUSH (puerto salida)
    note for NotificationDeliveryService "El adaptador del canal arma el mensaje final con plantilla + protectedData. protectedData NO se persiste."
```

---

## Dependencias entre módulos (vista de alto nivel)

```mermaid
flowchart TD
    Auth["1 · Auth & Usuarios"]
    Perfiles["2 · Perfiles"]
    Servicios["3 · Servicios"]
    Solicitudes["4 · Solicitudes"]
    Resenas["5 · Reseñas"]
    Metricas["6 · Métricas"]
    Reportes["7 · Reportes"]
    Notif["8 · Notificaciones"]
    Externos["APIs externas"]

    Auth -->|eliminar usuario| Servicios
    Auth -->|eliminar usuario| Solicitudes
    Auth -->|correos, avisos| Notif
    Servicios -->|init + detalle| Metricas
    Servicios -->|resenas en detalle| Resenas
    Solicitudes -->|distancia/activo| Servicios
    Solicitudes -->|notifica| Notif
    Resenas -.->|eventos| Metricas
    Solicitudes -.->|eventos| Metricas
    Reportes -->|banear, revertir| Auth
    Reportes -->|revertir feedback| Resenas
    Reportes -->|notifica| Notif
    Notif -->|EmailPort, PushPort| Externos

    classDef mod fill:#E6F1FB,stroke:#378ADD,color:#1A3D6E
    classDef ext fill:#F1EFE8,stroke:#888780,color:#5F5E5A
    class Auth,Perfiles,Servicios,Solicitudes,Resenas,Metricas,Reportes,Notif mod
    class Externos ext
```

> Nota: las flechas punteadas (`-.->`) son comunicación por **eventos** (desacoplada); las sólidas son llamadas directas entre servicios.
