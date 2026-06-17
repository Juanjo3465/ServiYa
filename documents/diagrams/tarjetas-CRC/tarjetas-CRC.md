# Tarjetas CRC — Marketplace de Servicios

Tarjetas **CRC (Class – Responsibility – Collaborator)** de la capa de servicios, organizadas por los 8 módulos del sistema.

Cada tarjeta describe, en lenguaje conceptual (no firmas técnicas):
- **Clase** — el nombre del servicio.
- **Responsabilidades** — qué sabe hacer / de qué se encarga.
- **Colaboradores** — otras clases (servicios, puertos o entidades de dominio) de las que depende para cumplir su tarea.

> Las colaboraciones marcadas como *(evento)* no son llamadas directas: el colaborador reacciona a un evento publicado (desacoplamiento por Spring Application Events).

---

## Módulo 1 — Auth & Usuarios

### UserAuthenticationService  ·  «orquestador»
| Responsabilidades | Colaboradores |
|---|---|
| Autenticar usuarios (login) y emitir el JWT. | UserService |
| Orquestar el registro completo en una transacción (usuario, consentimiento, perfil, rol). | ConsentService |
| Validar que el rol a asignar sea CLIENT u OFFERER (nunca ADMIN). | UserRoleService |
| Coordinar la recuperación de contraseña (solicitud y confirmación). | PasswordResetTokenService, NotificationService |

### UserService
| Responsabilidades | Colaboradores |
|---|---|
| Crear el registro base de usuario (con hash de contraseña). | — (repositorio de usuarios) |
| Gestionar al usuario existente: cambiar contraseña y correo. | — |
| Banear y desbanear usuarios. | — |

### UserDeletionService  ·  «orquestador»
| Responsabilidades | Colaboradores |
|---|---|
| Orquestar la eliminación (soft delete) de un usuario en una transacción. | UserService |
| Si es oferente: desactivar sus servicios y cancelar las solicitudes recibidas. | ServiceManagementService |
| Si es cliente: cancelar las solicitudes creadas. Notificar a la contraparte. | ServiceRequestCommandService, NotificationService |

### AdminService
| Responsabilidades | Colaboradores |
|---|---|
| Crear administradores (fuera del registro público). | UserService |
| Buscar/listar usuarios y armar el detalle con conteos para el panel admin. | UserDeletionService |
| Banear, desbanear y eliminar usuarios notificando al afectado. | NotificationService |

### RoleService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el catálogo de roles del sistema. | — |

### UserRoleService
| Responsabilidades | Colaboradores |
|---|---|
| Consultar los roles de un usuario y verificar si tiene uno. | — |
| Asignar y quitar roles a un usuario. | — |

### ConsentService
| Responsabilidades | Colaboradores |
|---|---|
| Registrar el consentimiento de términos durante el registro. | — |
| Verificar si un usuario ha consentido. | — |

### PasswordResetTokenService
| Responsabilidades | Colaboradores |
|---|---|
| Generar y persistir el token de recuperación (hasheado). | — |
| Validar el token (correspondencia, expiración, uso). | — |
| Marcar el token como usado. | — |

---

## Módulo 2 — Perfiles

### UserProfileService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer la info personal del usuario (con PII descifrada). | AddressService |
| Actualizar el perfil por formulario (PATCH parcial), cifrando PII. | — |
| Cambiar la dirección principal validando propiedad. | — |

### AddressService
| Responsabilidades | Colaboradores |
|---|---|
| Gestionar las direcciones de un usuario (crear, editar, eliminar, listar). | GeocodingPort |
| Cifrar la línea de dirección (PII) y obtener coordenadas. | — |

### OffererProfileService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el perfil público y el resumen del oferente (incluye WhatsApp y métricas). | OffererMetricsService |
| Actualizar el perfil de oferente por formulario (PATCH parcial). | — |

### OffererAvailabilityService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el horario de disponibilidad del oferente. | — |
| Guardar el calendario completo (reemplazo masivo, transaccional). | — |
| Gestionar franjas individuales (crear, editar, eliminar, activar). | — |

---

## Módulo 3 — Servicios

### CategoryService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el catálogo de categorías de servicios. | — |

### ServiceQueryService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer resúmenes y la página de detalle de un servicio. | ServiceReviewService |
| Componer el detalle con calificación, reseñas y resumen del oferente. | OffererProfileService |
| Listar y contar los servicios de un oferente. | — |

### ServiceManagementService
| Responsabilidades | Colaboradores |
|---|---|
| Crear el servicio con su métrica y franjas (transaccional). | ServiceMetricsService, ServiceAvailabilityService |
| Editar atributos del servicio (PATCH) y gestionar su ciclo de vida. | — |
| Activar/desactivar servicios; desactivar todos los de un oferente. | — |

### ServiceSearchService
| Responsabilidades | Colaboradores |
|---|---|
| Buscar servicios con filtros compuestos (texto, categoría, precio, cercanía, puntuación, disponibilidad). | — |

### ServiceAvailabilityService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el horario de disponibilidad de un servicio. | — |
| Guardar el calendario completo del servicio (reemplazo masivo, transaccional). | — |
| Gestionar franjas individuales del servicio. | — |

---

## Módulo 4 — Solicitudes de Servicio

### ServiceRequestQueryService
| Responsabilidades | Colaboradores |
|---|---|
| Listar las solicitudes de un cliente/oferente filtradas por estado. | ServiceFeedbackService |
| Proveer el detalle de una solicitud (vista de parte y vista de admin). | — |
| Proveer el historial de reprogramaciones y los conteos. | — |

### ServiceRequestCommandService
| Responsabilidades | Colaboradores |
|---|---|
| Crear solicitudes validando servicio activo, disponibilidad y radio. | ServiceQueryService, GeocodingPort |
| Gestionar las transiciones de estado (máquina de estados en el dominio). | ServiceRequest (dominio) |
| Reprogramar creando una solicitud enlazada (previousRequestId). | NotificationService (evento), MetricsServices (evento) |

### RescheduleProposalService
| Responsabilidades | Colaboradores |
|---|---|
| Crear, cancelar y consultar propuestas de reprogramación (filtrables por estado). | ServiceRequestCommandService |
| Aceptar una propuesta: cambiar su estado y disparar la reprogramación. | NotificationService |
| Rechazar una propuesta notificando al oferente. | — |

### ScheduledTasksService  ·  «scheduled»
| Responsabilidades | Colaboradores |
|---|---|
| Rechazar automáticamente solicitudes pendientes y propuestas vencidas. | ServiceRequestCommandService |
| Marcar como no presentadas las aceptadas sin confirmar tras 1 día. | RescheduleProposalService |
| Finalizar como completadas las no confirmadas por el cliente tras 1 día. | NotificationService |

---

## Módulo 5 — Reseñas & Calificaciones

### ServiceFeedbackService  ·  «orquestador»
| Responsabilidades | Colaboradores |
|---|---|
| Validar estado y unicidad antes de crear feedback de servicio. | ServiceRatingService |
| Crear rating y/o reseña según lo enviado (uno, otro o ambos). | ServiceReviewService |
| Proveer el feedback de una solicitud y los listados de feedback. | — |
| Revertir el feedback de una solicitud (elimina rating y/o review si existen) y confirmar al solicitante. | — |

### ServiceRatingService
| Responsabilidades | Colaboradores |
|---|---|
| Crear la calificación de un servicio y publicar el evento de calificación. | ServiceMetricsService (evento), OffererMetricsService (evento) |
| Eliminar la calificación publicando el evento de borrado. | — |

### ServiceReviewService
| Responsabilidades | Colaboradores |
|---|---|
| Crear la reseña de servicio con sus tags (transaccional) y publicar el evento. | ServiceTagMetricsService (evento) |
| Eliminar la reseña y sus tags publicando el evento de borrado. | OffererMetricsService (evento) |

### ServiceReviewTagCatalogService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el catálogo de tags disponibles para reseñas de servicios. | — |

### ClientFeedbackService  ·  «orquestador»
| Responsabilidades | Colaboradores |
|---|---|
| Validar estado y unicidad antes de crear feedback de cliente. | ClientRatingService |
| Crear rating y/o reseña del cliente según lo enviado. | ClientReviewService |
| Proveer el feedback de una solicitud y los listados de feedback. | — |
| Revertir el feedback de una solicitud (elimina rating y/o review si existen) y confirmar al solicitante. | — |

### ClientRatingService
| Responsabilidades | Colaboradores |
|---|---|
| Crear la calificación de un cliente y publicar el evento. | ClientMetricsService (evento) |
| Eliminar la calificación publicando el evento de borrado. | — |

### ClientReviewService
| Responsabilidades | Colaboradores |
|---|---|
| Crear la reseña del cliente con sus tags (transaccional) y publicar el evento. | ClientTagMetricsService (evento) |
| Eliminar la reseña y sus tags publicando el evento de borrado. | ClientMetricsService (evento) |

### ClientReviewTagCatalogService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el catálogo de tags disponibles para reseñas de clientes. | — |

---

## Módulo 6 — Métricas

### ServiceMetricsService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer las métricas precalculadas de un servicio. | — |
| Recalcular promedio y conteos de rating/reseñas al escuchar eventos. | ServiceRatingService (evento), ServiceReviewService (evento) |

### ServiceTagMetricsService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el conteo de tags de un servicio. | — |
| Aplicar UPSERT/decremento de tags al escuchar eventos de reseña. | ServiceReviewService (evento) |

### OffererMetricsService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer las métricas completas y el resumen del oferente. | — |
| Actualizar rating, reseñas, tags positivas/negativas y contadores de estado por eventos. | ServiceRatingService (evento), ServiceReviewService (evento), ServiceRequestCommandService (evento) |

### ClientMetricsService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer las métricas completas y el resumen del cliente. | — |
| Actualizar rating, reseñas, tags positivas/negativas y contadores de estado por eventos. | ClientRatingService (evento), ClientReviewService (evento), ServiceRequestCommandService (evento) |

### OffererTagMetricsService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el conteo de tags de un oferente. | — |
| Aplicar UPSERT/decremento de tags al escuchar eventos de reseña de servicio. | ServiceReviewService (evento) |

### ClientTagMetricsService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer el conteo de tags de un cliente. | — |
| Aplicar UPSERT/decremento de tags al escuchar eventos de reseña de cliente. | ClientReviewService (evento) |

---

## Módulo 7 — Reportes

### ReportService
| Responsabilidades | Colaboradores |
|---|---|
| Crear el reporte base común (lógica compartida por composición). | — |
| Listar reportes de forma unificada (por tipo, categoría, estado) y proveer conteos. | — |

### RequestReportService
| Responsabilidades | Colaboradores |
|---|---|
| Crear un reporte sobre una solicitud (reutilizando el reporte base). | ReportService |
| Proveer el detalle del reporte con los datos de la solicitud. | — |

### ServiceReviewReportService
| Responsabilidades | Colaboradores |
|---|---|
| Crear un reporte sobre una reseña de servicio (reutilizando el reporte base). | ReportService |
| Proveer el detalle del reporte con los datos de la reseña. | — |

### ClientReviewReportService
| Responsabilidades | Colaboradores |
|---|---|
| Crear un reporte sobre una reseña de cliente (reutilizando el reporte base). | ReportService |
| Proveer el detalle del reporte con los datos de la reseña. | — |

### ReportActionService
| Responsabilidades | Colaboradores |
|---|---|
| Registrar las acciones tomadas por un admin sobre un reporte. | — |
| Proveer el historial de acciones de un reporte. | — |

### ModerationService  ·  «orquestador»
| Responsabilidades | Colaboradores |
|---|---|
| Aplicar acciones de moderación (advertir, banear, revertir feedback, cerrar). | UserService |
| Para revertir feedback: identificar el tipo según el reporte y DELEGAR en el FeedbackService correspondiente. | ServiceFeedbackService, ClientFeedbackService |
| Cerrar el reporte y registrar la acción de forma centralizada (finalizeReport). | ReportActionService |
| Notificar al reportante el resultado y al reportado lo que le afecta (incluida la reversión confirmada por el boolean). | NotificationService |

---

## Módulo 8 — Notificaciones

### NotificationService  ·  «orquestador»
| Responsabilidades | Colaboradores |
|---|---|
| Crear la notificación base persistiendo solo el mensaje genérico. | NotificationDeliveryService |
| Despachar la notificación por los canales solicitados (transaccional). | — |
| Transportar los datos protegidos sin persistirlos. | — |

### NotificationDeliveryService
| Responsabilidades | Colaboradores |
|---|---|
| Crear el delivery por canal y despacharlo por el puerto de salida. | EmailPort, PushNotificationPort |
| Gestionar el estado del envío y la lectura (readAt). | NotificationChannelService |
| Proveer las notificaciones del usuario (ordenadas, filtrables). | — |

### NotificationChannelService
| Responsabilidades | Colaboradores |
|---|---|
| Proveer los canales de notificación disponibles. | — |
