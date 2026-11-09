# Requisitos de Infraestructura e Integración — Microservicio de Chat

> **Proyecto:** Marketplace de Servicios (Ingesoft 2)
> **Documento:** Qué debe tener o implementar el resto del sistema para que el `chat-service` funcione según lo planeado, 
> **Estado:** Borrador v0.1 — documento vivo
> **Documento principal:** `guia-microservicio-chat.md`

---

## Control de cambios

| Versión | Fecha | Autor | Cambios |
|---|---|---|---|
| 0.1 | 2026-10-26 | — | Versión inicial |
| 0.2 | 2026-10-26 | — | Pruebas y documentación se trasladan a la guía del chat (§14 y §15). Redis: se usa la instancia existente del proyecto; se definen los ajustes que necesita (§9.1) |
| 0.3 | 2026-10-26 | — | Solicitudes publica `created` y `status_changed`; Notificaciones sin contenido del mensaje; soporte con rol `ADMIN`; Redis: Pub/Sub en instancia propia del chat y presencia en la de caché; frontend con filtros y advertencia de contacto |
| 0.4 | 2026-11-06 | — | **Alineación con el stack real de ServiYa.** IDs en eventos/claims son **`BIGINT`**, no UUID. JWT **HS256** con secreto compartido (DP-03). Exchanges con prefijo **`serviya.*`**. **§6 reescrito**: Notificaciones es **ejecutor puro** — el chat compone y emite el comando `notification.requested.v1` existente; Notificaciones solo entrega. Notas de estado real: los eventos de dominio de Solicitudes/Usuarios **aún no existen** (extracción planeada del monolito); la instancia Redis A es `redis-coord` (existe), la B de caché **aún no existe**. |

---

## Aviso importante: adaptar al sistema real

Este documento propone **recomendaciones**, no imposiciones. Los nombres de exchanges, routing keys, rutas, claims del JWT, herramientas y metas numéricas son sugerencias coherentes con la guía del chat. **Cada equipo debe contrastarlas con lo que el proyecto ya tiene implementado** y:

- Si el sistema ya tiene una convención (nombres de eventos, formato de errores, estructura de logs, herramienta de pruebas), **se respeta la existente** y se actualiza este documento.
- Si una recomendación no aplica, se marca como *No aplica* con una línea de justificación, en lugar de borrarla.
- Las decisiones que cambien un contrato entre servicios se registran como ADR.

---

## Índice

1. [Resumen de dependencias](#1-resumen-de-dependencias)
2. [API Gateway](#2-api-gateway)
3. [Auth / emisión de JWT](#3-auth--emisión-de-jwt)
4. [Backend principal — módulo de Solicitudes](#4-backend-principal--módulo-de-solicitudes)
5. [Backend principal — Usuarios y Perfiles](#5-backend-principal--usuarios-y-perfiles)
6. [Módulo de Notificaciones](#6-módulo-de-notificaciones)
7. [Módulos de Reportes y Métricas](#7-módulos-de-reportes-y-métricas)
8. [RabbitMQ](#8-rabbitmq)
9. [Redis e infraestructura adicional](#9-redis-e-infraestructura-adicional)
10. [Redes Docker y despliegue conjunto](#10-redes-docker-y-despliegue-conjunto)
11. [Observabilidad transversal](#11-observabilidad-transversal)
12. [Frontend](#12-frontend)
13. [Checklist por fase](#13-checklist-por-fase)

> Las pruebas y la documentación del microservicio están en la guía principal (§14 y §15).

---

## 1. Resumen de dependencias

| Componente | Qué debe aportar al chat | Fase | Criticidad |
|---|---|---|---|
| API Gateway | Ruteo REST y WebSocket a `/api/chat/**` | 0 | Bloqueante |
| Auth | Validación de JWT verificable por el chat (claves, claims) | 0 | Bloqueante |
| Solicitudes | Eventos de creación y de **todo** cambio de estado de la solicitud (**aún no se publican**; extracción planeada) | 1 | Bloqueante para MVP |
| Usuarios/Perfiles | Eventos de alta, cambios y suspensión de usuarios + carga inicial (**aún no se publican**) | 1 | Bloqueante para MVP |
| Notificaciones | **Entregar** el comando `notification.requested.v1` que compone el chat (contrato existente); no decide ni interpreta | 1 | Alta |
| RabbitMQ | Exchanges, credenciales, DLQ | 0 | Bloqueante |
| Redis de locks/rate limiting (existente) | Usuario ACL, presupuesto de memoria y acceso por red para tickets y rate limiting del chat | 0 | Bloqueante |
| Redis de caché (planeado) | Usuario ACL para presencia y "escribiendo…" | 2 | Bloqueante para fase 2 |
| Almacenamiento de objetos (nuevo) | Adjuntos | 3 | Bloqueante para fase 3 |
| Reportes / Métricas | Consumir eventos del chat | 2 | Media |
| Auth – rol `ADMIN` | Rol `ADMIN` en el token (los administradores atienden soporte y moderación) | 4 | Bloqueante para fase 4 |
| Frontend | Cliente WebSocket con reconexión y `sync` | 1 | Bloqueante para MVP |

---

## 2. API Gateway

**Obligatorio (Fase 0)**

- [ ] Ruta `/api/chat/**` → `chat-service` (puerto interno acordado, p. ej. 8000).
- [ ] Soporte de **WebSocket upgrade** en `/api/chat/v1/ws` (cabeceras `Upgrade` y `Connection` reenviadas).
- [ ] **Timeout de inactividad** de conexiones WebSocket mayor que el intervalo de heartbeat (recomendado ≥ 60 s; el chat hace ping cada ~25 s).
- [ ] Terminación TLS: exposición pública solo por **HTTPS/WSS**.
- [ ] Reenvío de cabecera de correlación (`X-Request-Id` o la que use el proyecto); generarla si no viene.
- [ ] CORS permitiendo los orígenes del frontend para `/api/chat/**`.
- [ ] **No** exponer `/health`, `/metrics` ni rutas internas del chat.

**Recomendado**

- [ ] Rate limiting general por IP/usuario en `/api/chat/**` (el chat aplica además su propio límite por usuario).
- [ ] Validación del JWT en el gateway para REST (el chat valida igualmente).
- [ ] **Sin** sesiones pegajosas: no son necesarias gracias a Redis; activarlas solo si el gateway lo exige para WS.
- [ ] Límite de tamaño de cuerpo para `/api/chat/**` (fase 3: considerar que las subidas van directo al almacenamiento con URL firmada, no por el gateway).

> **Prueba de humo de Fase 0:** abrir un WebSocket a través del gateway con un cliente simple (`websocat`, script Python) y mantenerlo abierto 5 minutos sin cortes.

---

## 3. Auth / emisión de JWT

**Obligatorio**

- [ ] Mecanismo de verificación accesible al chat:
  - **Decisión vigente (ServiYa, DP-03 — actualizada 2026-11-08):** **ES256 + JWKS**. Auth firma con una clave privada EC P-256 y publica las claves **públicas** en `GET /.well-known/jwks.json` (accesible por red interna). El chat verifica con la clave pública del JWKS; **no** tiene material de firma y no puede emitir tokens. Esto reemplaza el HS256 con secreto compartido que se planteó originalmente (el monolito y `notifications-service` también migran, ver la guía de centralización de auth `documents/microservicios/guia-implementacion-auth.md`).
  - **Config (resource server estándar):** el chat actúa como *resource server* con `jwk-set-uri` apuntando al JWKS, `jws-algorithms: ES256` (el default de las librerías suele ser RS256 → **configurarlo explícitamente**), validando `iss`, `aud` y `exp`. No escribir verificación de firma a mano.
  - El chat **no** chequea `min_iat` (revocación): esa comprobación la hace el gateway. El chat, detrás del gateway, confía en que el token ya pasó ese filtro, y solo revalida firma/claims como defensa en profundidad.
- [ ] Claims en el token:

| Claim | Uso en el chat | Estado en ServiYa |
|---|---|---|
| `sub` | ID del usuario — es un **`BIGINT`** (mismo tipo que usan los eventos y las proyecciones), no UUID | ✅ Emitido (`sub` = userId) |
| `roles` | Distinguir `CLIENT`, `OFFERER` y `ADMIN` (los administradores atienden el soporte) | ✅ Emitido como lista `["CLIENT"\|"OFFERER"\|"ADMIN"]` |
| `iss`, `aud` | Validar emisor y audiencia | ✅ **Se emiten** con el nuevo token ES256 (`aud` = `marketplace-api`; `iss` = URL lógica estable del emisor). El chat **debe** validarlos. |
| `kid`, `typ` | Selección de clave (rotación) y tipo de token | ✅ `kid` en el header para elegir la clave del JWKS; `typ` = `at+jwt` |
| `exp`, `iat` | Expiración | ✅ `exp`/`iat` emitidos |

- [ ] Política de rotación de claves documentada (con JWKS, publicar la nueva clave antes de empezar a firmar con ella).

**Fase 4**

- [ ] Rol `ADMIN` presente en el token de los administradores. Si ya existe, solo confirmar el nombre del claim y su valor.

> Si un usuario puede ser cliente y oferente a la vez, aclararlo: el rol en una conversación lo determina la solicitud, no el token.

---

## 4. Backend principal — módulo de Solicitudes

**Obligatorio (MVP)**

> **Estado real:** hoy el monolito **no publica** estos eventos al bus (los flujos cross-dominio usan eventos de aplicación Spring in-process, que no salen del proceso). Publicarlos es trabajo **planeado** de la extracción del monolito. La mecánica de outbox ya existe en ServiYa (la del `notification_outbox`) y es reutilizable. Todos los `*_id` del `data` son **`BIGINT`**.

- [ ] Publicar eventos del ciclo de vida de la solicitud en el exchange `serviya.requests` (prefijo `serviya.*`, la convención ya existente), usando el **patrón Outbox** o al menos publicación tras el commit (`@TransactionalEventListener(phase = AFTER_COMMIT)` en Spring).
- [ ] Eventos y payload mínimo:

| Routing key | `data` mínimo | Uso en el chat |
|---|---|---|
| `requests.service_request.created` | `request_id`, `client_id`, `offerer_id`, `service_id`, `status`, `created_at` | Abre la conversación (DP-01: el chat se habilita al crearse la solicitud) |
| `requests.service_request.status_changed` | `request_id`, `previous_status`, `status`, `changed_by` | Actualiza el estado copiado para los **filtros** y, si el estado es terminal, pasa el chat a solo lectura indefinida (DP-02) |

- [ ] Publicar `status_changed` en **cada** transición de estado, no solo en las terminales, porque el chat filtra por cualquier estado.
- [ ] El `occurred_at` del sobre debe reflejar el momento real del cambio: el chat lo usa para ignorar eventos que lleguen fuera de orden.
- [ ] Sobre común de eventos (`event_id`, `event_type`, `event_version`, `occurred_at`, `producer`, `correlation_id`, `data`), igual al definido en la guía §9.1.
- [ ] Endpoint **interno** o script de **carga inicial** de solicitudes vigentes para poblar `request_directory` del chat (paginado).

**Recomendado**

- [ ] Definir la lista de **estados terminales** (los que pasan el chat a solo lectura) según la máquina de estados real (DP-11 de la guía). Los valores de `status` se envían tal cual; el chat no los interpreta salvo esa lista.
- [ ] Si el módulo ya publica eventos específicos por estado (`accepted`, `cancelled`…), se pueden mantener, pero el chat solo depende de `created` y `status_changed`.
- [ ] Mostrar en la pantalla de detalle de la solicitud el acceso al chat, consultando `GET /api/chat/v1/conversations/by-request/{id}`.

---

## 5. Backend principal — Usuarios y Perfiles

**Obligatorio (MVP)**

> **Estado real:** tampoco existen hoy; mismo trabajo planeado que en §4. Todos los `*_id` son **`BIGINT`**.

- [ ] Publicar en `serviya.users`:

| Routing key | `data` mínimo |
|---|---|
| `users.user.registered` | `user_id`, `display_name`, `avatar_url`, `roles`, `status` |
| `users.user.updated` | `user_id` y campos visibles cambiados (`display_name`, `avatar_url`) |
| `users.user.suspended` | `user_id`, `reason` (opcional) |
| `users.user.reactivated` | `user_id` |
| `users.user.deleted` | `user_id` |

- [ ] Carga inicial de usuarios para `user_directory` (endpoint interno paginado o script).

**Recomendado**

- [ ] Publicar solo datos necesarios para mostrar (nombre, avatar). **No** enviar email, teléfono ni documentos al chat (minimización de datos personales).

---

## 6. Módulo de Notificaciones

> **Decisión de ServiYa (ADR-011 revisado):** Notificaciones es un **ejecutor puro** y no decide ni interpreta el mensaje. **Quien decide y compone es el chat**: cuando el destinatario está desconectado, el chat arma una notificación genérica («Tienes un mensaje nuevo de {nombre}», con el nombre que tiene en su propia proyección `user_directory`) y emite el **comando `notification.requested.v1`** por el contrato existente `serviya.notifications`, **igual que hoy lo hace el monolito**. Notificaciones solo la entrega. Esto **reduce** lo que este módulo debe construir para el chat: idealmente, nada nuevo si el comando reutiliza el mismo formato actual.

**Obligatorio (MVP)**

- [ ] Aceptar y **entregar** el comando `notification.requested.v1` que emite el chat por `serviya.notifications`, en las modalidades que ya soporta (in-app, email…), **sin lógica de decisión** propia sobre el mensaje.
- [ ] Mantener el **consumo idempotente** que ya tiene.
- [ ] Si el comando del chat necesita algún campo nuevo (p. ej. un *deep link* `/chat/{conversation_id}`), acordarlo como extensión del contrato de notificación, no como lógica que Notificaciones deba inferir.

**Recomendado**

- [ ] **Agrupar** notificaciones (si el módulo ya agrupa) — pero la decisión de agrupar/silenciar vive donde ya esté hoy, no se le añade al módulo por el chat.
- [ ] Respetar preferencias del usuario (silenciar chat), si ya existen.

> **Nota:** el evento de dominio `chat.message.sent` **ya no lo consume Notificaciones** (queda solo para Métricas, §7). Notificaciones no se entera de los mensajes: solo entrega lo que el chat le manda compuesto.

---

## 7. Módulos de Reportes y Métricas

**Recomendado (Fase 2)**

- [ ] **Reportes:** consumir `chat.message.reported` y crear el reporte con referencia al `message_id`/`conversation_id`. Para revisar el contenido, un **administrador** usará un endpoint del chat con rol `ADMIN` y auditoría (fase 4), no una copia del mensaje.
- [ ] **Métricas:** consumir `chat.conversation.created` y `chat.message.sent` para indicadores como tiempo de primera respuesta del oferente, que puede alimentar la reputación (`offerer_metrics`). Acordar si esa métrica entra en el modelo de reputación.
- [ ] **Métricas (opcional):** contar mensajes con `flags` que incluyan `CONTACT_INFO` como indicador de desintermediación (DP-06).

---

## 8. RabbitMQ

**Obligatorio (Fase 0)**

- [ ] Exchanges *topic* durables: `serviya.requests`, `serviya.users`, `serviya.chat` (prefijo `serviya.*`, ya usado por `serviya.notifications`).
- [ ] Usuario propio para el chat con permisos mínimos: *write* en `serviya.chat` y en `serviya.notifications` (para emitir el comando de notificación), *read* en sus colas, *configure* solo sobre recursos `chat.*`.
- [ ] Colas del chat durables con su **DLQ** (`chat.<nombre>.dlq`) vía `x-dead-letter-exchange`.
- [ ] *Publisher confirms* habilitados para los publicadores.
- [ ] Mensajes persistentes (`delivery_mode = 2`).

**Recomendado**

- [ ] Un vhost por entorno (`dev`, `test`, `prod`).
- [ ] Política de reintentos con retardo (cola de espera con TTL que devuelve a la principal) antes de enviar a DLQ.
- [ ] Documentar todos los eventos del sistema en un **catálogo de eventos** común (AsyncAPI por servicio + índice central).
- [ ] Panel de administración solo en red interna.

---

## 9. Redis e infraestructura adicional

Para discutir con el equipo. Se justifica cada componente y se dan alternativas.

### 9.1 Redis — ajustes necesarios

El chat usa tres instancias (detalle de claves, TTL y fallos en la guía §5.5):

| Instancia | Dueño | Qué usa el chat | Desde |
|---|---|---|---|
| **A — Locks / rate limiting** | Proyecto (existente) | Rate limiting de mensajes y tickets de WebSocket (TTL ~30 s). **No** Pub/Sub | Fase 0 |
| **B — Caché** | Proyecto (planeada) | Presencia y "escribiendo…" (DP-09) | Fase 2 |
| **C — Pub/Sub del chat** | Chat (en su propio `docker-compose`) | Difusión de mensajes entre instancias del chat (DP-08) | Fase 0 |

La instancia C la despliega y opera el equipo del chat; **no requiere nada de la infraestructura compartida**. Lo que sí debe hacer el resto del proyecto:

**Obligatorio en la instancia A (Fase 0)**

- [ ] **Usuario ACL `chat`** con acceso solo a claves `~chat:*` y a los comandos necesarios (strings y contadores con TTL, `EXPIRE`, `GETDEL`/`DEL`, `PING`). Sin `PUBLISH`/`SUBSCRIBE`, `KEYS`, `FLUSH*` ni comandos de administración.
- [ ] **Presupuesto de memoria** para el prefijo `chat:` y umbral de alerta (DP-10). Estimación MVP: del orden de cientos de KB.
- [ ] **Accesible desde la red compartida** (`marketplace-shared` o la que use el proyecto), con autenticación y sin puerto expuesto al host.
- [ ] **Monitoreo de memoria usada** (debería existir ya, por ser crítica para los locks).

**Obligatorio en la instancia B (antes de la Fase 2)**

- [ ] La instancia de caché debe estar desplegada: la presencia del chat depende de ella.
- [ ] Usuario ACL `chat` con claves `~chat:*`.
- [ ] Política con desalojo (p. ej. `allkeys-lru`) y TTL en las claves del chat, para que la presencia nunca desplace datos de caché importantes.

**Recomendado**

- [ ] Documentar en el repositorio de infraestructura qué servicios usan cada instancia y con qué prefijo, para evitar choques.

### 9.2 Almacenamiento de objetos — Fase 3

| | |
|---|---|
| **Para qué** | Guardar adjuntos fuera de la base de datos. |
| **Opción recomendada** | **MinIO** (compatible con S3) en Docker para desarrollo y entornos propios; S3 u otro proveedor si se despliega en la nube. |
| **Requisitos** | Bucket privado, URLs firmadas de subida y descarga, política de ciclo de vida para archivos huérfanos. |
| **Opcional** | Antivirus (ClamAV) que escanee antes de marcar el adjunto como disponible. |

### 9.3 Observabilidad — recomendable

| Necesidad | Opción mínima | Opción completa |
|---|---|---|
| Logs | Logs JSON a stdout + `docker compose logs` | Loki + Grafana o stack ELK |
| Métricas | Endpoint `/metrics` | Prometheus + Grafana |
| Trazas | ID de correlación en logs | OpenTelemetry + Jaeger/Tempo |

Para un proyecto académico, la opción mínima es suficiente en MVP; Prometheus + Grafana es un buen paso siguiente si el equipo quiere demostrar los RNF de rendimiento.

---

## 10. Redes Docker y despliegue conjunto

Cada servicio tiene su propio `docker-compose`, así que se necesita una forma de que se vean entre sí.

**Obligatorio**

- [ ] Una **red Docker externa compartida** (p. ej. `marketplace-shared`) creada una vez:
  ```bash
  docker network create marketplace-shared
  ```
- [ ] Gateway, RabbitMQ, Redis compartido (instancias A y B; la C es interna del chat), backend principal y `chat-service` conectados a esa red. Las bases de datos de cada servicio quedan en sus redes internas.
- [ ] Nombres de host estables (nombre del servicio o `container_name`) documentados. Nombres reales hoy en ServiYa: `rabbitmq` (`serviya-rabbitmq`), `redis-coord` (= instancia A, ya existe; red `serviya-redis-net`), la instancia B de caché **aún no existe**, `gateway` (`serviya-gateway`), `backend` (`serviya-backend`), y el nuevo `chat-service`.
- [ ] Variables de entorno del chat documentadas en `.env.example`:

| Variable | Ejemplo | Descripción |
|---|---|---|
| `CHAT_DB_URL` | `mysql+asyncmy://...@chat-db:3306/chat_db` | BD propia (**MySQL 8.4**, el motor del proyecto) |
| `REDIS_URL` | `redis://chat:***@redis-coord:6379/0` | Instancia A (locks/rate limiting) = `redis-coord` (ya existe), usuario ACL `chat` |
| `REDIS_PUBSUB_URL` | `redis://chat-pubsub:6379/0` | Instancia C, propia del chat (red interna) |
| `REDIS_CACHE_URL` | `redis://chat:***@redis-cache:6379/0` | Instancia B (caché), requerida desde fase 2 (**aún no existe**) |
| `RABBITMQ_URL` | `amqp://chat:***@rabbitmq:5672/dev` | Broker compartido |
| `JWT_JWKS_URL` | `http://<auth>/.well-known/jwks.json` | Verificación de tokens ES256 por clave pública (ver §3). Ya no se usa `JWT_SECRET` |
| `JWT_ISSUER`, `JWT_AUDIENCE` | `aud` = `marketplace-api` | Validación de claims (obligatoria con el token ES256) |
| `ALLOWED_WS_ORIGINS` | `http://localhost:8081,http://localhost:5173` | Orígenes permitidos (mismos que el CORS del gateway) |
| `LOG_LEVEL` | `INFO` | Nivel de log |

**Recomendado**

- [ ] Script o `Makefile` en un repositorio de "infra" que levante todo en orden: red → RabbitMQ y Redis → gateway → backend → chat.
- [ ] Puertos expuestos al host solo para el gateway (y paneles de administración en desarrollo).

---

## 11. Observabilidad transversal

**Obligatorio**

- [ ] **ID de correlación** propagado de punta a punta: gateway → HTTP → evento (`correlation_id`) → consumidor. Es lo que permite seguir un mensaje del chat hasta la notificación.
- [ ] Logs estructurados con campos comunes en todos los servicios: `timestamp`, `level`, `service`, `correlation_id`, `user_id` (si aplica), `event`.
- [ ] Health checks `live`/`ready` en cada servicio.

**Recomendado**

- [ ] Métricas clave del chat: conexiones WS activas, mensajes/segundo, latencia de envío, tamaño del outbox pendiente, mensajes en DLQ.
- [ ] Alerta (aunque sea manual/revisión) cuando haya mensajes en una DLQ.

---

## 12. Frontend

**Obligatorio (MVP)**

- [ ] Cliente WebSocket único por sesión con: obtención de ticket, heartbeat, reconexión con *backoff* exponencial + jitter, envío de `sync` al reconectar.
- [ ] Generación de `client_message_id` (UUID) por mensaje y reintento con el mismo id.
- [ ] Estados visuales del mensaje: enviando → enviado (ACK) → leído.
- [ ] Renderizar el texto de mensajes **como texto**, nunca como HTML.
- [ ] Respaldo por REST para enviar si el WebSocket no está disponible.
- [ ] Filtros del listado por estado de la conversación y por estado de la solicitud (combinables, varios valores).
- [ ] Mostrar la advertencia de datos de contacto cuando el ACK traiga `warnings: ["CONTACT_INFO"]` (DP-06); el mensaje ya fue enviado, así que es un aviso, no un error.
- [ ] Mostrar las conversaciones en solo lectura con el historial visible y el campo de texto deshabilitado.
- [ ] En fase 2, el botón "Iniciar chat" solo se muestra a clientes en el perfil o servicio de un oferente (DP-05).

**Recomendado**

- [ ] Guardar borradores no enviados localmente.
- [ ] Cargar historial con *scroll* infinito usando `before_seq`.

---

## 13. Checklist por fase

### Fase 0 — Cimientos
- [ ] Red Docker compartida creada
- [ ] Gateway rutea REST y WebSocket al chat
- [ ] Mecanismo de verificación de JWT acordado (DP-03)
- [ ] Usuario, exchanges y DLQ en RabbitMQ
- [ ] Usuario ACL `chat`, presupuesto de memoria y acceso por red en la instancia Redis de locks/rate limiting
- [ ] Contratos de eventos de Solicitudes y Usuarios acordados por escrito (AsyncAPI)

### Fase 1 — MVP
- [ ] Solicitudes publica `created` y `status_changed` en cada transición
- [ ] Lista de estados terminales definida (DP-11)
- [ ] Usuarios publica eventos
- [ ] Carga inicial de proyecciones disponible
- [ ] Notificaciones **entrega** el comando `notification.requested.v1` que compone el chat (sin lógica de decisión propia)
- [ ] Frontend con cliente WS completo
- [ ] ID de correlación de punta a punta

### Fase 2 — Chat directo
- [ ] Instancia Redis de caché desplegada, con usuario ACL `chat` (DP-09: presencia)
- [ ] Regla de negocio de contacto directo definida (DP-05)
- [ ] Reportes consume `chat.message.reported`
- [ ] Métricas consume eventos del chat (opcional)

### Fase 3 — Adjuntos
- [ ] MinIO/S3 desplegado con bucket privado
- [ ] Gateway/CORS configurado para subidas directas con URL firmada (o subida vía chat)

### Fase 4 — Soporte
- [ ] Rol `ADMIN` confirmado en el JWT
- [ ] Interfaz de bandeja de soporte para administradores en el frontend
