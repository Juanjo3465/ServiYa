# notifications-service — extracción del módulo de notificaciones

> Primer microservicio extraído del monolito (Strangler Fig, `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §7.4 paso 1).
> Rama: `feature/notifications-microservice`. Código: `backend/notifications-service/` (servicio),
> `backend/api-gateway/` (gateway) y `backend/serviya/.../notifications/` (cliente que queda en el monolito).

---

## 1. Resumen

| Antes | Después |
|---|---|
| Módulo `notifications` dentro del monolito (40 archivos). | Servicio Spring Boot independiente con **BD propia** (`serviya_notifications`). |
| Los demás módulos llamaban a `notify(...)` **en memoria**, dentro de su transacción. | Llaman al **mismo** `notify(...)`, que ahora deja un comando en un **outbox** transaccional; un relay lo publica en **RabbitMQ** y el servicio lo consume. |
| El correo del destinatario se leía de `users` al enviar. | El monolito lo resuelve y viaja **sellado** (AES-GCM) en el mensaje. |
| `GET /api/v1/notifications`, `POST /{id}/read`, `GET /notification-channels` los servía el monolito. | Los sirve el servicio; el **API gateway** los enruta. El frontend no cambia. |

Ningún llamador del monolito cambió de código: `NotificationServicePort.notify(...)` conserva su firma.

---

## 2. Análisis de dependencias (estado previo a la extracción)

### 2.1 Lo que `notifications` usaba de otros módulos (salientes)

| Dependencia | Dónde | Cómo se resolvió |
|---|---|---|
| `users.UserQueryServicePort` + `users.domain.User` (correo del destinatario) | `EmailApiAdapter` | El correo viaja en el mensaje. El servicio ya no conoce `users`. |
| `shared.security.CurrentUser` (id del JWT) | `NotificationController` | El servicio valida el JWT por su cuenta (mismo `JWT_SECRET`, solo verifica). |
| `shared.lock.DistributedLock` (lock en `redis-coord` del reintento) | `NotificationRetryScheduler` | `SELECT … FOR UPDATE SKIP LOCKED` en la BD propia. Sin lock externo. |
| `shared.coordination.CoordinationAvailability` (modo degradado D-07/1a) | `NotificationDeliveryService` | Puerto propio + sondeo `PING` con un usuario ACL que **solo** puede hacer `PING`. Se quitó del monolito (era su único consumidor). |
| `shared.events.DomainEventPublisherPort` | `NotificationDeliveryService` | Adaptador propio (eventos internos del servicio). |
| `shared.exceptions.ResourceNotFoundException` | `NotificationDeliveryService` | Excepción propia. |

Conclusión: era una **hoja** del grafo, con una sola dependencia de negocio (`users`, para el correo).

### 2.2 Quién usaba `notifications` (entrantes)

Solo a través de `NotificationServicePort.notify(...)` y del enum `ChannelName`:

| Módulo | Clases | Llamadas |
|---|---|---|
| users | `UserAuthenticationService`, `UserService`, `UserDeletionService` | bienvenida, rol adquirido, reset y cambio de contraseña, aviso por cuenta eliminada |
| requests | `ServiceRequestCommandService`, `RescheduleProposalService`, `RequestMaintenanceService` | ciclo de vida de solicitudes y propuestas, vencimientos |
| reports | `ReportService`, `RequestReportService`, `ServiceFeedbackReportService`, `ClientFeedbackReportService` | reportes y su resolución |
| admin | `AdminService`, `ModerationService` | ban, revocación de rol, moderación |

Todas las llamadas ocurrían **dentro de la transacción de negocio** del llamador. Por eso no se usó HTTP síncrono:
una caída del servicio habría tumbado o desincronizado esas operaciones.

### 2.3 Base de datos

- `notifications.user_id` → **FK a `users`** (`ON DELETE CASCADE`). Se elimina: `users` vive en otra BD. Los usuarios
  se borran de forma lógica (soft delete), así que la cascada no se usaba en la práctica.
- `notification_deliveries` → FK a `notifications` y a `notification_channels`: ambas tablas viajan juntas y se conservan.
- Ninguna consulta de otro módulo leía estas tablas (verificado: ni JPQL ni SQL nativo fuera del módulo).

### 2.4 Frontend

`frontend/serviya/src/shared/api.js` → `notificationApi` (3 endpoints). Mismas rutas y mismo JSON detrás del gateway,
así que **no hay cambios en el frontend**. Solo cambia el `nginx.conf`, que ahora apunta al gateway.

---

## 3. Arquitectura resultante

```
 Navegador ──▶ nginx (frontend :8081) ──▶ API GATEWAY (:8080) ──┬──▶ monolito (backend:8080)
                                                                 │        │  @Transactional negocio
                                                                 │        │  + INSERT notification_outbox
                                                                 │        ▼
                                                                 │   relay (cada 1 s, SKIP LOCKED)
                                                                 │        │ publisher confirms
                                                                 │        ▼
                                                                 │   RabbitMQ  exchange serviya.notifications
                                                                 │        │ routing key notification.requested.v1
                                                                 │        ▼
                                                                 └──▶ notifications-service (:8082)
                                                                          ├─ consumidor idempotente (eventId)
                                                                          ├─ BD serviya_notifications (Flyway)
                                                                          ├─ bandeja REST /api/v1/notifications/**
                                                                          ├─ correo AFTER_COMMIT ─▶ Brevo
                                                                          └─ reintento programado (SKIP LOCKED)
```

### 3.1 Decisiones y alternativas descartadas

| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| **RabbitMQ + Transactional Outbox** (M3) | HTTP síncrono; Redis Streams | La notificación debe confirmarse o revertirse con el negocio, y una caída del servicio no debe afectarlo. Redis (`redis-coord`) es solo de coordinación. |
| **Correo en el mensaje** | API interna de users en cada envío | Elimina el acoplamiento síncrono con identity. Coste: el correo viaja por el bus (mitigado con el sellado, §5). |
| **BD propia + Flyway** con `ddl-auto: validate` (M25) | Compartir el MySQL del monolito | Datos por servicio (§7.1.3). El esquema es reproducible y versionado. |
| **`FOR UPDATE SKIP LOCKED`** para el reintento y el relay | Lock distribuido en `redis-coord` | El servicio es dueño de su BD; con ≥2 réplicas cada fila la procesa una sola, sin otra infraestructura. |
| **API gateway** (Spring Cloud Gateway) desde ya | Regla extra en nginx | Es el requisito de entrega (§7.0) y la fachada del estrangulamiento: cada extracción futura será una ruta más. |

---

## 4. Contrato `notification.requested` v1

Productor: `NotificationOutboxPersistenceAdapter` (monolito). Consumidor: `NotificationRequestedListener` (servicio).
Ejemplo canónico: `backend/notifications-service/src/test/resources/contracts/notification-requested.v1.json`.

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `schemaVersion` | int | sí | `1`. Otra versión se rechaza y va a la DLQ. |
| `eventId` | UUID | sí | Único por petición. **Clave de idempotencia** (`notifications.source_event_id` UNIQUE). |
| `occurredAt` | ISO-8601 | no | Momento de la petición. |
| `userId` | long | sí | Destinatario (id de identity). |
| `type`, `title`, `message` | string | sí | Igual que antes (`welcome`, `password_reset`, …). |
| `entityType`, `entityId` | string, long | no | Entidad referenciada (p. ej. `SERVICE_REQUEST`, 9). |
| `channels` | [string] | no | `INTERNAL`, `EMAIL`. Vacío = `INTERNAL`. Los desconocidos se ignoran. |
| `sealed` | base64 | no | AES-256-GCM de `{recipientEmail, protectedData}`. `null` si no hay nada sensible. |

**Topología**: exchange `serviya.notifications` (topic) → cola `notifications-service.notification-requested`
(durable, con dead-letter) → DLX `serviya.notifications.dlx` → `notifications-service.notification-requested.dlq`.
La cola la declaran **ambos** lados con los mismos argumentos: si el monolito publica antes del primer arranque del
servicio, el mensaje espera en la cola en vez de descartarse.

**Evolución**: añadir campos opcionales es compatible, porque el consumidor ignora los desconocidos. Cambiar o quitar
un campo exige `schemaVersion` 2 y routing key `notification.requested.v2` (convivencia de ambas durante el despliegue).

---

## 5. Garantías y modos de fallo

| Situación | Qué pasa |
|---|---|
| La operación de negocio hace rollback | La fila del outbox también: **no sale la notificación** (igual que antes). |
| RabbitMQ caído | El negocio sigue funcionando. Las filas se acumulan en `notification_outbox` (con `attempts`/`last_error`) y salen al volver. |
| notifications-service caído | Los mensajes esperan en la cola durable. La bandeja del frontend devuelve error hasta que vuelve. |
| Mensaje duplicado (entrega ≥1) | Se descarta por `eventId` (chequeo previo + UNIQUE para la carrera entre consumidores). |
| Mensaje inválido o sellado con otra clave | Directo a la **DLQ**, sin reintentos (`AmqpRejectAndDontRequeueException`). |
| Fallo transitorio al consumir (BD) | 4 intentos con backoff (1 s → 10 s) y luego a la DLQ. |
| Brevo falla | La entrega queda `FAILED` y la reintenta el scheduler (máx. 3 intentos). `password_reset` no se reintenta (sin el enlace no sirve). |
| `redis-coord` caído (con el sondeo activo) | Los correos se difieren (D-07/1a) y el reintento **no** gasta intentos hasta que Redis vuelva. |

**Consistencia**: la notificación aparece en la bandeja ~1 s después del commit (intervalo del relay). Antes era
inmediata. Es una consecuencia aceptada del desacople (consistencia eventual).

---

## 6. Seguridad

- **Sellado del payload**: el correo y `protectedData` (el enlace de reset lleva un token) quedan en reposo en el
  outbox y en la cola. Se cifran con AES-256-GCM y la clave compartida `NOTIFICATIONS_PAYLOAD_KEY`, con una AAD ligada
  al contrato v1. Así se mantiene la regla original: *el token nunca toca una tabla en claro*. Un vector de prueba común
  (`PayloadSealerTest` en ambos proyectos) fija la compatibilidad.
- **Correo en reposo** en el servicio: columna `recipient_email` cifrada (VARBINARY, clave propia `ENCRYPTION_KEY`
  del servicio), necesaria para que el reintento reenvíe sin preguntar a identity.
- **JWT**: el servicio solo **verifica** (HS256, misma derivación SHA-256 que `JwtService`). **Diferencia con el
  monolito**: no revalida contra `users` si la cuenta sigue activa, así que un usuario baneado conserva acceso a *su*
  bandeja hasta que expira el token (`JWT_EXPIRATION_MS`, 1 h). Ver pendientes (§9).
- **Autorización por defecto denegada** (`anyRequest().denyAll()`), al revés que el `permitAll` del monolito (§3.1).
  `markAsRead` sigue validando la propiedad (`findByIdAndUserId`): sin IDOR (B14).
- **Fail-fast** si faltan `JWT_SECRET`, `ENCRYPTION_KEY` o `NOTIFICATIONS_PAYLOAD_KEY`, o si son débiles.
- **Redis**: usuario ACL `svc-notifications-coord` con `resetkeys resetchannels +@connection +ping`, sin acceso a claves.

---

## 7. Operación

### 7.1 Variables de entorno nuevas (`.env`)

| Variable | Quién | Nota |
|---|---|---|
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | monolito, servicio, rabbitmq | |
| `NOTIFICATIONS_PAYLOAD_KEY` | monolito **y** servicio | **La misma** en los dos. ≥ 32 caracteres aleatorios. |
| `NOTIFICATIONS_ENCRYPTION_KEY` | servicio | Distinta de la `ENCRYPTION_KEY` del monolito. |
| `NOTIFICATIONS_DB_USER/PASSWORD`, `NOTIFICATIONS_MYSQL_ROOT_PASSWORD` | servicio, notifications-mysql | |
| `REDIS_SVC_NOTIFICATIONS_COORD_PASSWORD` | servicio, ACL de Redis | Hay que **volver a generar el ACL** (`render-acl.sh`). |
| `GATEWAY_CORS_ALLOWED_ORIGINS` | gateway | Opcional; por defecto, los mismos orígenes que tenía el monolito. |
| `EMAIL_*` | servicio | Se movieron del monolito al servicio. |

### 7.2 Arranque

```bash
docker compose up -d --build                                                          # sin Redis
docker compose -f docker-compose.yml -f infra/redis/docker-compose.redis.yml up -d --build   # con Redis (activa el sondeo D-07/1a)
```

Ejecución local sin Docker: el gateway escucha en `:8080` y espera al monolito en `MONOLITH_URI`
(por defecto `http://localhost:8079`; arrancar el monolito con `SERVER_PORT=8079`) y al servicio en `:8082`.

### 7.3 Dead-letter queue

Consola de RabbitMQ: http://localhost:15672, cola `notifications-service.notification-requested.dlq`. Un mensaje en
la DLQ es un error de contrato o de claves, no un fallo transitorio. Para reprocesarlo tras corregir la causa, usa la
opción *Move messages* de la consola hacia el exchange `serviya.notifications` con la routing key `notification.requested.v1`.

### 7.4 Migrar las notificaciones existentes (una sola vez)

Las tablas antiguas siguen en la BD del monolito (`ddl-auto: update` no borra tablas). Para conservar el histórico
(ejecutar en **Git Bash**; PowerShell 5.1 corrompe la codificación de las tuberías):

```bash
source .env
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --no-create-info --complete-insert --skip-triggers "$DB_NAME" notifications notification_deliveries \
  | docker compose exec -T notifications-mysql mysql -u"$NOTIFICATIONS_DB_USER" -p"$NOTIFICATIONS_DB_PASSWORD" serviya_notifications
```

- Los ids de canal (1 = INTERNAL, 2 = EMAIL) coinciden porque Flyway los siembra con esos ids.
- Las filas migradas no tienen `source_event_id` ni `recipient_email`: se ven en la bandeja, pero una entrega EMAIL
  antigua en `FAILED` ya no se reenvía (queda `FAILED`).
- Verificado lo anterior, se pueden borrar las tablas antiguas del monolito (`DROP TABLE notification_deliveries,
  notifications, notification_channels;`). En una BD nueva ya no se crean (`mysql/01_create_database.sql`).

---

## 8. Pruebas

| Proyecto | Pruebas |
|---|---|
| notifications-service | 30 unitarias (entrega/reintento, idempotencia, consumidor y política de DLQ, sellado con vector compartido, JWT compatible con el monolito, Brevo nunca lanza) + **4 de integración** con MySQL 8.4 y RabbitMQ 4.1 reales (Testcontainers): bus → BD → bandeja REST, duplicados, aislamiento entre usuarios, `markAsRead`, `SKIP LOCKED`, cifrado en reposo y DLQ. |
| monolito | Cliente (`NotificationServiceTest`), contrato JSON y sellado (`NotificationOutboxPersistenceAdapterTest`, `PayloadSealerTest`), relay (`NotificationOutboxPublisherTest`). Suite completa: mismos resultados que `main` (3 errores ambientales previos: `contextLoads` sin H2 y `PiiAttributeConverterTest`). |
| api-gateway | 4 de enrutado real contra dos backends HTTP de prueba (destino por ruta, CORS resuelto en el borde sin reenviar `Origin`). |

> **Docker Engine 29 + Testcontainers 1.21.3** (versión del BOM de Boot 3.5.5): la detección de Docker falla con un
> HTTP 400 y los tests de integración se **saltan** en silencio (también los de Redis del monolito). Para ejecutarlos:
> `./mvnw test "-DargLine=-Dapi.version=1.44"`.

---

## 9. Pendientes y deuda conocida

1. **Revalidación de la cuenta en el servicio** (ban inmediato): hoy espera a que expire el JWT. Opciones: TTL de
   token más corto, o un evento `user.banned` que el servicio consuma (lista de bloqueo local).
2. **Rate limiting de la bandeja**: el techo global `api-default` del monolito ya no cubre `/api/v1/notifications/**`.
   Se puede aplicar en el gateway con `RequestRateLimiter` sobre Redis.
3. **Validación de JWT en el gateway** (§7.7): hoy cada servicio valida; centralizarlo es opcional.
4. **Outbox genérico**: el outbox es específico de notificaciones. Cuando se extraiga `metrics` (paso 2), generalizarlo
   para los eventos de dominio (`RequestStatusChangedEvent`, …).
5. **Bug sistémico CONFIRMADO en `main` (fuera del alcance de esta extracción, sin corregir en el monolito)**: tras
   B10, **31 entidades** tienen `@Version` pero **ningún** dominio lleva la versión, y todos los `update` hacen
   `repository.save(mapper.toEntity(dominio))`. La entidad sale con `version = null` y Hibernate la rechaza.
   Reproducido en Docker: `PATCH /api/v1/users/me/profile` → **500** con
   `DataIntegrityViolationException: Detached entity with generated id '21' has an uninitialized version value 'null':
   UserProfileEntity.version`. Afecta, previsiblemente, a cualquier actualización (perfiles, solicitudes, propuestas,
   métricas, reportes, usuarios…). Arreglo propuesto: llevar `version` al dominio y mapearlo en ambos sentidos (como
   hace `NotificationDeliveryPersistenceMapper` en el servicio), o actualizar cargando la entidad gestionada. El
   `GlobalExceptionHandler` tampoco registra las excepciones genéricas, lo que ocultaba el problema.
