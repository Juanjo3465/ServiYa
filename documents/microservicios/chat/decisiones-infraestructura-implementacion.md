# Decisiones de implementación — infraestructura del chat

> Documento de trabajo derivado de `requisitos-infraestructura-chat.md` y de las decisiones
> tomadas con el equipo. Fuente de verdad para la implementación de la infraestructura compartida.
>
> **Alcance:** integración del `chat-service` con la infraestructura de ServiYa.
> **Fuera de alcance:** código del `chat-service` (otro equipo), frontend WebSocket y la instancia
> Redis de caché (`redis-cache`, fase 2).
>
> **Estado:** decisiones cerradas. Infraestructura compartida implementada; la integración end-to-end
> queda pendiente del `chat-service` y de la rama `api-gateway` (ES256/JWKS).

## 1. Decisiones cerradas

| Tema | Decisión | Motivo |
|---|---|---|
| Red compartida | **`serviya-redis-net`** | Ya existe y agrupa gateway, backend, RabbitMQ y Redis. Se evita una segunda red. |
| Vhost RabbitMQ | **`/` (default)** | Es el vhost que ya usan el monolito y `notifications-service`. `/dev` queda descartado para no romper notificaciones. |
| Puerto del chat | **8000**, solo interno | No hay conflicto y coincide con el ejemplo del documento. |
| Gateway REST | `/api/chat/**` → `http://chat-service:8000` | Contrato del documento. |
| Gateway WebSocket | `/api/chat/v1/ws` → `ws://chat-service:8000` | Requiere preservar `Upgrade`/`Connection`. |
| JWT | **ES256 + JWKS** | Decisión de auth 2026-11-08. El chat no tiene clave privada. |
| JWKS | `http://gateway:8080/.well-known/jwks.json` | URL estable a través del gateway; hoy enruta al emisor (monolito) y luego al servicio de identidad. |
| Redis A | Reutilizar **`redis-coord`** | Rate limiting y tickets de WebSocket. Usuario ACL `chat`, prefijo `chat:`. |
| Redis B (caché) | **Excluida** | Presencia/typing son fase 2; no entra en esta entrega. |
| Redis C (Pub/Sub) | Propiedad del equipo del chat | Contenedor `chat-pubsub` en el compose del chat. |
| RabbitMQ: dueño de colas | **`chat-service`** declara colas, bindings y DLQ | El servicio es dueño de su ciclo de vida; infra declara exchanges y ACL. |
| Eventos del monolito | **En alcance** | `requests` y `users` publican al bus mediante outbox. |
| Almacenamiento de objetos | **MinIO-compatible local en infraestructura compartida** | Fase 3. S3-compatible, sin coste cloud. |
| Imagen local | **`pgsty/silo`** | Las imágenes comunitarias de MinIO desaparecieron de Docker Hub/Quay en 2026; Silo es un fork compatible que incluye `mc`/`mcli`. |
| Subida de adjuntos | **URLs prefirmadas directas a MinIO** | Recomendación del documento. Bucket privado y expiración corta. |
| Bucket | `chat-attachments` | Privado; sin acceso anónimo. |
| Envelope de eventos | `event_id`, `event_type`, `event_version`, `occurred_at`, `producer`, `subject`, `correlation_id`, `causation_id`, `data` | Trazabilidad suficiente sin sobreingeniería. |

## 2. Desviaciones conscientes del documento original

El documento de requisitos se mantiene como referencia, con estas adaptaciones:

1. **Vhost `/`** en lugar del ejemplo `/dev`. El documento debe actualizarse para no confundir.
2. **Red `serviya-redis-net`** en lugar de `marketplace-shared`. Es la red real del proyecto.
3. **JWKS a través del gateway** en lugar de apuntar directamente al monolito. El gateway es el
   punto estable; el emisor puede cambiar sin tocar al chat.
4. **MinIO en infraestructura compartida**, no en el compose del chat. El almacenamiento es un
   recurso compartido y el chat lo consume por red interna.
5. **`redis-cache` no se implementa ahora**; solo se documenta como dependencia de fase 2.

## 3. Topología RabbitMQ

Vhost: `/`.

### 3.1 Exchanges

| Exchange | Tipo | Uso |
|---|---|---|
| `serviya.requests` | topic, durable | Eventos del ciclo de vida de solicitudes |
| `serviya.users` | topic, durable | Eventos del ciclo de vida de usuarios |
| `serviya.chat` | topic, durable | Eventos de chat para métricas/reportes/soporte |
| `serviya.notifications` | topic, durable (existente) | Comandos de notificación ya compuestos |
| `serviya.chat.dlx` | direct, durable | Dead-letter de las colas del chat |

### 3.2 Routing keys

```text
requests.service_request.created
requests.service_request.status_changed

users.user.registered
users.user.updated
users.user.suspended
users.user.reactivated
users.user.deleted

chat.conversation.created
chat.message.sent
chat.conversation.read_only
chat.message.reported
chat.support.ticket_opened
chat.support.ticket_resolved
```

El comando existente de notificaciones conserva su routing key:

```text
notification.requested.v1
```

### 3.3 Colas del chat

| Cola | Exchange | Routing keys | DLQ |
|---|---|---|---|
| `chat-service.requests` | `serviya.requests` | `requests.service_request.*` | `chat-service.requests.dlq` |
| `chat-service.users` | `serviya.users` | `users.user.*` | `chat-service.users.dlq` |

Las colas y sus bindings los declara `chat-service`. Infraestructura declara los exchanges,
`serviya.chat.dlx` y el usuario ACL.

### 3.4 Usuario RabbitMQ `chat`

Permisos sobre vhost `/`:

- **configure:** `^(chat-service\..*|serviya\.chat|serviya\.chat\.dlx)$`
- **write:** `^(serviya\.chat|serviya\.notifications)$`
- **read:** `^chat-service\..*$`

No puede publicar en `serviya.requests` ni `serviya.users`.

## 4. Envelope de eventos de dominio

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "requests.service_request.status_changed",
  "event_version": 1,
  "occurred_at": "2026-11-09T13:00:00Z",
  "producer": "serviya-monolith",
  "subject": "service_request/123",
  "correlation_id": "f1c2...",
  "causation_id": "b7a1...",
  "data": {
    "request_id": 123,
    "previous_status": "PENDING",
    "status": "ACCEPTED",
    "changed_by": 45
  }
}
```

Reglas:

- `event_id` es UUID; se usa como `message_id` de RabbitMQ y para idempotencia.
- `event_version` es la versión del esquema, no del agregado.
- `occurred_at` es UTC.
- `subject` identifica el agregado: `service_request/{id}`, `user/{id}`,
  `conversation/{id}`.
- `causation_id` referencia el `event_id` que causó el evento; puede ser `null`.
- Los IDs son `BIGINT`, no UUID.
- Los consumidores ignoran campos desconocidos y son idempotentes por `event_id`.
- `notification.requested.v1` **no** se migra a este envelope: es un comando con contrato propio
  y payload cifrado.

## 5. Variables de entorno

### Chat service

```env
CHAT_PORT=8000

CHAT_DB_URL=mysql+asyncmy://chat:${CHAT_DB_PASSWORD}@chat-db:3306/chat_db
CHAT_DB_USER=chat
CHAT_DB_PASSWORD=
CHAT_DB_ROOT_PASSWORD=

# Redis A — compartida
REDIS_URL=redis://chat:${REDIS_CHAT_PASSWORD}@redis-coord:6379/0

# Redis C — propia del chat
REDIS_PUBSUB_URL=redis://chat-pubsub:6379/0

# RabbitMQ
RABBITMQ_URL=amqp://chat:${CHAT_RABBITMQ_PASSWORD}@rabbitmq:5672/

# Auth
JWT_JWKS_URL=http://gateway:8080/.well-known/jwks.json
JWT_ISSUER=https://auth.serviya.local
JWT_AUDIENCE=marketplace-api
JWT_ALGORITHMS=ES256

# Frontend
ALLOWED_WS_ORIGINS=http://localhost:5173,http://localhost:8081

# Observabilidad
CORRELATION_HEADER=X-Request-Id
LOG_LEVEL=INFO
```

`REDIS_CACHE_URL` no se define en esta entrega.

### MinIO / S3

```env
MINIO_ROOT_USER=
MINIO_ROOT_PASSWORD=
MINIO_CHAT_ACCESS_KEY=
MINIO_CHAT_SECRET_KEY=
MINIO_BUCKET=chat-attachments
MINIO_REGION=us-east-1
S3_INTERNAL_ENDPOINT=http://minio:9000
S3_PUBLIC_ENDPOINT=http://localhost:9000
```

`S3_PUBLIC_ENDPOINT` es el host que usa el navegador para la URL prefirmada; no debe ser el nombre
interno de Docker.

## 6. MinIO local

- Servicio `minio` en el `docker-compose.yml` raíz, con profile `minio`. **Implementación actual:** usa `pgsty/silo`, un fork S3-compatible de MinIO (las imágenes oficiales de MinIO ya no están disponibles públicamente).
- Consola en `:9001` solo para desarrollo.
- API S3 en `:9000` para las URLs prefirmadas del navegador en local.
- Bucket `chat-attachments` privado.
- Inicialización idempotente con `minio/mc`.
- Red compartida `serviya-redis-net`; puertos de host solo en desarrollo.
- CORS configurado para los orígenes del frontend.
- Validación de MIME/tamaño y autorización de participantes en `chat-service` antes de firmar.
- Ciclo de vida para adjuntos huérfanos: fase 3, se documenta pero no bloquea el MVP.

## 7. Gateway y JWT

- La ruta del chat debe declararse antes del catch-all del monolito.
- El gateway no debe eliminar `Origin` en la ruta WebSocket: el chat valida el origen del handshake.
  Esto requiere revisar el filtro global `RemoveRequestHeader=Origin` (hoy se aplica a todo).
- El gateway propaga/genera `X-Request-Id`.
- El chat valida ES256/JWKS por su cuenta (defensa en profundidad).
- `min_iat` se valida solo en el gateway.
- La integración completa queda bloqueada hasta que `origin/api-gateway` (ES256/JWKS) esté
  disponible en el entorno de despliegue.

## 8. Dependencias y bloqueos

| Dependencia | Estado | Impacto |
|---|---|---|
| `chat-service` | No existe en este repo | No se pueden probar REST/WS reales ni adjuntos end-to-end |
| `origin/api-gateway` ES256/JWKS | Rama separada, no en `main` | Bloquea validación JWT real |
| `redis-cache` | Excluida | Solo bloquea presencia/typing de fase 2 |
| Frontend WS | Otro equipo | No bloquea la infraestructura |
| Autenticación centralizada | Fase posterior | JWKS se consume vía gateway desde ya |

## 9. Orden de implementación

1. Red compartida y variables de entorno. **Hecho:** se reutiliza `serviya-redis-net`.
2. Redis A: ACL `chat` y presupuesto de memoria. **Hecho:** usuario `chat` en
   `infra/redis/acl/users.coord.acl.tpl`, restringido a `~chat:*`.
3. RabbitMQ: exchanges, DLX, usuario `chat` y permisos. **Hecho:**
   `infra/rabbitmq/setup-topology.sh`.
4. Gateway: rutas REST/WS, CORS, correlación y tratamiento de `Origin`. **Hecho:** rutas en
   `backend/api-gateway/src/main/resources/application.yaml` y `OriginHeaderFilter`.
5. MinIO: contenedor, bucket privado, CORS y credenciales. **Hecho:** profile `minio` del
   `docker-compose.yml` con `pgsty/silo`; bucket `chat-attachments`.
6. Monolito: outbox de eventos de `requests` y `users`, envelope común y backfill. **Hecho
   (publicación):** `domain_event_outbox`, `ChatEventPublisher` y llamadas desde requests/users/
   profiles. **Pendiente:** endpoint/script de backfill inicial para proyecciones del chat.
7. Observabilidad: correlación extremo a extremo y smoke tests. **Hecho en gateway y monolito:** el
   gateway genera/propaga `X-Request-Id`, el monolito lo conserva en MDC y el envelope lo incluye. La
   propagación al `chat-service` queda a cargo de ese servicio.
8. Documentación final y actualización de los documentos de requisitos. **Hecho:** este documento,
   `infra/redis/README.md`, `infra/minio/README.md`, `infra/rabbitmq/README.md` y la guía de ejecución.

## 10. Criterios de aceptación

- [x] `docker compose config` válido.
- [x] `redis-coord` healthy; el usuario `chat` solo opera sobre `chat:*`.
- [x] RabbitMQ crea exchanges, DLX y permisos sin afectar `serviya.notifications`.
- [x] Gateway enruta REST y WebSocket del chat y no expone health/metrics.
- [x] MinIO/Silo arranca, el bucket es privado y el usuario `chat` solo accede a su bucket.
- [x] El monolito publica `users.user.registered` con el envelope acordado y sobrevive a RabbitMQ caído
  (outbox transaccional).
- [x] Los tests del monolito siguen verdes.
- [x] `redis-cache` sigue sin implementarse.
- [ ] Prueba end-to-end REST/WS con `chat-service` real.
- [ ] Backfill inicial de `user_directory` y `request_directory`.
- [ ] Validación JWT ES256 real con la rama `api-gateway` desplegada.
