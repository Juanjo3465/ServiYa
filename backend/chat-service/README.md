# chat-service

Chat 1 a 1 entre **cliente** y **oferente** de ServiYa, asociado a una solicitud de servicio (fase 1 / MVP de la
guía). Es un microservicio independiente: BD propia, Redis Pub/Sub propio, y comunicación con el resto solo por el
API gateway (REST + WebSocket) y RabbitMQ.

- Diseño y requisitos: [`documents/microservicios/chat/guia-microservicio-chat.md`](../../documents/microservicios/chat/guia-microservicio-chat.md)
  y [`requisitos-infraestructura-chat.md`](../../documents/microservicios/chat/requisitos-infraestructura-chat.md).
- Decisiones tomadas al implementar (incluido el cambio a Java/Spring): [`docs/adr/`](docs/adr/README.md).
- Contratos: REST [`docs/openapi.json`](docs/openapi.json) · WebSocket y eventos [`docs/asyncapi.yaml`](docs/asyncapi.yaml).

**Qué hace:** abre la conversación al crearse la solicitud; mensajes de texto en tiempo real con orden total por
conversación, idempotencia por `client_message_id`, historial paginado, confirmación de lectura, recuperación tras
reconectar (`sync`); solo lectura definitiva cuando la solicitud termina; listado con filtros por estado de la
conversación y de la solicitud; advertencia (sin bloqueo) si un mensaje parece traer datos de contacto; aviso
genérico a Notificaciones cuando el destinatario está desconectado.

**Qué no hace (aún):** chat directo sin solicitud, bloqueo, archivar, «escribiendo…», presencia con TTL, borrado de
mensajes (fase 2); adjuntos (fase 3); soporte con administradores (fase 4). No emite tokens ni envía correos.

## Arquitectura en una mirada

```
src/main/java/com/parosurvivors/chat/
├── conversations/   Conversation (agregado), casos de uso de apertura/estado/listado/lectura, REST, consumidor de solicitudes
├── messaging/       Message + detector de datos de contacto, envío/historial/sync, REST
├── directory/       proyección local de usuarios (nombre, estado) + consumidor de eventos de usuarios
├── realtime/        WebSocket, tickets, Pub/Sub, presencia, rate limiting
└── shared/          puertos transversales (UnitOfWork, EventPublisher...), outbox + relay, seguridad, web, config
```

Hexagonal (Escuela B, como el monolito): `domain` y `application` no importan Spring ni JPA; los casos de uso se
instancian en `shared/infrastructure/config/UseCaseConfig`.

| Dependencia | Para qué | Si cae |
|---|---|---|
| MySQL 8.4 propio (`chat-mysql`) | Fuente de verdad | El servicio no funciona |
| `redis-coord` (compartido, usuario ACL `svc-chat-coord`, claves `chat:*` con TTL) | Tickets WS, rate limiting | Tickets → 503 (usar REST); rate limiting en memoria por instancia |
| Redis Pub/Sub propio (`chat-pubsub`) | Difusión entre instancias, «¿está en línea?» | Los mensajes se guardan; el destinatario los recupera con `sync`; se notifica por si acaso |
| RabbitMQ (`serviya.*`) | Eventos entrantes y salientes | Los salientes esperan en el outbox; los entrantes esperan en la cola |
| JWKS del emisor | Verificar JWT ES256 | Tokens ya cacheados por `kid` siguen validando |

## Requisitos previos

- Docker y Docker Compose. JDK 21 + Maven solo para desarrollar fuera de Docker.
- El ACL de `redis-coord` generado (incluye el usuario del chat):
  ```bash
  cd infra/redis/acl && set -a && . ../../../.env && set +a && sh ./render-acl.sh users.coord.acl.tpl users.coord.acl
  chmod 644 users.coord.acl   # ⚠ el script deja 640 y el usuario redis del contenedor no puede leerlo
  ```
- Un emisor de JWT con JWKS. Mientras Auth no lo publique, usar el de desarrollo (abajo).

## Levantarlo

Variables (ver [`.env.example`](../../.env.example)): `CHAT_DB_PASSWORD`, `CHAT_MYSQL_ROOT_PASSWORD`,
`CHAT_REDIS_COORD_PASSWORD` (= `REDIS_SVC_CHAT_COORD_PASSWORD` del ACL), `RABBITMQ_PASSWORD`, `JWT_JWKS_URL`,
**`JWT_ISSUER` (obligatorio: el servicio no arranca sin él)**, `JWT_AUDIENCE`, `ALLOWED_WS_ORIGINS`.
Opcionales: `CHAT_RATE_LIMIT_MAX` / `CHAT_RATE_LIMIT_WINDOW_SECONDS` (20 / 10), `LOG_STRUCTURED_FORMAT=ecs` (logs JSON).

```bash
# Desde la raíz del repo. Las migraciones (Flyway) corren solas al arrancar.
docker compose -f docker-compose.yml -f infra/redis/docker-compose.redis.yml up -d --build chat-service redis-coord gateway
```

Salud (red interna; el gateway no la expone): `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

## Probarlo a mano (sin frontend ni monolito)

Herramientas en [`dev/`](dev/), solo stdlib de Python + `openssl`:

```bash
cd backend/chat-service/dev
python3 dev_auth.py init && python3 dev_auth.py serve --port 9400 &   # JWKS de desarrollo
# El chat debe arrancar con:
#   JWT_JWKS_URL=http://host.docker.internal:9400/.well-known/jwks.json   JWT_ISSUER=http://dev-auth.serviya.local
TC=$(python3 dev_auth.py token --user 1001 --roles CLIENT)
TO=$(python3 dev_auth.py token --user 1002 --roles OFFERER)

# Lo que haría el monolito (CHAT-1): RABBITMQ_PASSWORD en el entorno; usa la consola de RabbitMQ (:15672)
python3 stub_producer.py user-registered --user 1001 --name "Ana Cliente"
python3 stub_producer.py request-created --request 77 --client 1001 --offerer 1002

API=http://localhost:8080/api/v1/chat
curl -H "Authorization: Bearer $TO" $API/conversations
TICKET=$(curl -s -X POST -H "Authorization: Bearer $TC" $API/ws-tickets | python3 -c 'import sys,json;print(json.load(sys.stdin)["ticket"])')
WS_ORIGIN=http://localhost:5173 python3 ws_client.py "ws://localhost:8080/api/v1/chat/ws?ticket=$TICKET"
# y escribir, por ejemplo:
# {"type":"message.send","id":"f-1","payload":{"conversation_id":1,"client_message_id":"c-1","body":"Hola"}}

python3 stub_producer.py status-changed --request 77 --status CANCELLED   # → solo lectura
```

`--event-id` reutiliza un id (probar idempotencia) y `--occurred-at` fija la hora (probar eventos fuera de orden).

## Pruebas

```bash
mvn test -DargLine="-Dapi.version=1.44"
```

- **⚠ `-Dapi.version=1.44`:** con Docker Engine ≥ 29, el cliente de Testcontainers que trae Spring Boot 3.5.5 habla
  la API 1.32 y Docker la rechaza. **Sin esa opción las pruebas de integración se SALTAN en silencio** (Maven dice
  `BUILD SUCCESS` con menos pruebas). Con Docker < 25 no la uses.
- Unitarias (dominio y casos de uso con fakes en memoria): milisegundos, sin Docker.
- Integración con contenedores reales (MySQL 8.4, dos Redis — el de coordinación con el **ACL real** de
  `infra/redis` —, RabbitMQ, y un JWKS ES256 de prueba): persistencia, concurrencia del `seq`, adaptadores Redis,
  criterios de aceptación del MVP de punta a punta (`ChatEndToEndIntegrationTest`, ~2,5 min) y contrato OpenAPI.
- Si cambias la API a propósito, regenera el contrato:
  `mvn test -Dtest=OpenApiContractIntegrationTest -DargLine="-Dapi.version=1.44 -Dopenapi.write=true"`.

## Runbook breve

- **Mensajes en una DLQ** (`chat-service.requests.dlq`, `chat-service.users.dlq`): inspeccionar en la consola de
  RabbitMQ. Los inválidos (JSON o campos) llegan sin reintentos; un `status_changed` de una solicitud sin
  conversación llega tras los reintentos (el `created` no llegó o llegó tarde). Corregido el origen, mover los
  mensajes a la cola principal (shovel o «Move messages» de la consola).
- **Outbox creciendo** (`SELECT COUNT(*) FROM outbox_events WHERE published_at IS NULL`): revisar conexión y
  credenciales de RabbitMQ; el log avisa al primer fallo y luego ~1 vez por minuto. Se publica solo al volver.
- **Tickets en 503 / «Rate limiting en Redis no disponible»** en el log: `redis-coord` caído, lleno o con el ACL mal
  generado. Las conexiones abiertas siguen funcionando.
- **Escalar:** varias instancias funcionan sin sesiones pegajosas (Pub/Sub + `SKIP LOCKED` en el relay). Al
  reiniciar, los clientes reciben 1001 y reconectan + `sync`.

## Pendiente

- **Bloqueante del MVP real:** el monolito no publica `requests.service_request.*` ni `users.user.*`, ni hay carga
  inicial de usuarios (ESTADO CHAT-1). Mientras tanto, `dev/stub_producer.py`.
- Emisor ES256/JWKS real y su `iss` definitivo (guía de auth).
- Limpieza de `outbox_events` publicados; métricas (`/metrics`); prueba de carga (RNF-CHAT-01/03); reglas de
  arquitectura automatizadas (ArchUnit); política de retención (DP-07) y presupuesto de memoria en `redis-coord` (DP-10).
