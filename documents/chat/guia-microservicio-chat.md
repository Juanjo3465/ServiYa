# Guía de Arquitectura — Microservicio de Chat

> **Proyecto:** Marketplace de Servicios (Ingesoft 2)
> **Documento:** Guía de arquitectura y decisiones del microservicio `chat-service`
> **Estado:** Borrador v0.1 — documento vivo, se construye de forma incremental
> **Documento complementario:** `requisitos-infraestructura-chat.md` (lo que el resto del sistema debe implementar para integrarse con el chat)

---

## Control de cambios

| Versión | Fecha | Autor | Cambios |
|---|---|---|---|
| 0.1 | 2026-10-26 | — | Versión inicial: contexto, requisitos, MVP, arquitectura y decisiones base |
| 0.2 | 2026-10-26 | — | Se agregan secciones de Pruebas (§14) y Documentación (§15) del microservicio. Redis: se reutiliza la instancia existente del proyecto (nueva §5.5, ADR-005 actualizado, compose sin Redis propio) |
| 0.3 | 2026-10-26 | — | Análisis de motor de BD (§7.5, ADR-004); filtros por estado de conversación y de solicitud (RF-CHAT-31, §7.4); soporte atendido por administradores; resueltas DP-01, 02, 04, 05, 06, 08 y 09; Redis Pub/Sub propio del chat; advertencia de datos de contacto (RF-CHAT-32) |
| 0.4 | 2026-11-06 | — | **Alineación con el stack real de ServiYa** (tras contrastar con el código): (1) **IDs `BIGINT`**, no UUID — ServiYa usa `BIGINT` para usuarios y solicitudes; `client_message_id` sigue siendo un token opaco del cliente. (2) **Motor de BD: MySQL 8.4**, no PostgreSQL — es el motor del proyecto (§6, §7, ADR-004 reescrito). (3) **Modelo de notificación invertido** (DP-04, ADR-011, §9.3): Notificaciones es ejecutor puro; **el chat decide y compone** la notificación y emite un **comando** por el contrato existente `serviya.notifications`. (4) Prefijo de exchanges **`serviya.*`** (convención ya existente), no `marketplace.*`. (5) Notas de estado real: los eventos de dominio de Solicitudes/Usuarios **aún no se publican** en el monolito (extracción planeada); JWT es **HS256** con secreto compartido (DP-03). |

---

## Índice

1. [Propósito y alcance](#1-propósito-y-alcance)
2. [Contexto del sistema](#2-contexto-del-sistema)
3. [Requisitos](#3-requisitos)
   - 3.1 Requisitos funcionales
   - 3.2 Requisitos no funcionales
   - 3.3 MVP
4. [Hoja de ruta por fases](#4-hoja-de-ruta-por-fases)
5. [Arquitectura del microservicio](#5-arquitectura-del-microservicio)
6. [Stack tecnológico](#6-stack-tecnológico)
7. [Modelo de dominio y datos](#7-modelo-de-dominio-y-datos)
8. [Interfaces: API REST y protocolo WebSocket](#8-interfaces-api-rest-y-protocolo-websocket)
9. [Integración asíncrona (eventos RabbitMQ)](#9-integración-asíncrona-eventos-rabbitmq)
10. [Seguridad](#10-seguridad)
11. [Escalabilidad y confiabilidad](#11-escalabilidad-y-confiabilidad)
12. [Despliegue](#12-despliegue)
13. [Registro de decisiones (ADR)](#13-registro-de-decisiones-adr)
14. [Pruebas](#14-pruebas)
15. [Documentación](#15-documentación)
16. [Consideraciones y recomendaciones](#16-consideraciones-y-recomendaciones)
17. [Decisiones pendientes y resueltas](#17-decisiones-pendientes-y-resueltas)
18. [Glosario](#18-glosario)

---

## 1. Propósito y alcance

Este documento guía el diseño e implementación de un **módulo de chat** concebido desde cero como un **microservicio independiente** del backend principal (Java + Spring Boot). Recoge el *qué* (requisitos), el *cómo* (arquitectura) y el *por qué* (decisiones y alternativas descartadas).

**Dentro del alcance**

- Chat 1 a 1 entre **cliente** y **oferente**.
- Etapa 1: conversación **asociada a una solicitud de servicio**.
- Etapa 2: conversación **no asociada** a una solicitud (contacto directo).
- Etapas posteriores: **adjuntos** y **chat de soporte** (atendido por los **administradores** de la plataforma, no por un rol de soporte aparte).

**Fuera del alcance**

- Chats grupales.
- Llamadas de voz o video.
- Envío de notificaciones push/email (lo hace el módulo de Notificaciones a partir de eventos del chat).

---

## 2. Contexto del sistema

El chat es un servicio autónomo: tiene **su propio código, base de datos, despliegue y ciclo de vida**. Se comunica con el resto del sistema solo por contratos explícitos: HTTP/WebSocket a través del API Gateway y eventos por RabbitMQ.

```mermaid
flowchart LR
    FE[Frontend web / móvil] -->|HTTPS REST + WSS| GW[API Gateway]
    GW -->|/api/chat/**| CHAT[chat-service]
    GW -->|/api/**| CORE[Backend principal<br/>Spring Boot]

    CORE -->|eventos: solicitudes, usuarios| MQ[(RabbitMQ)]
    MQ -->|consume| CHAT
    CHAT -->|eventos: mensajes, conversaciones| MQ
    MQ -->|consume| NOTIF[Módulo Notificaciones]

    CHAT --- DB[(MySQL 8.4<br/>chat_db)]
    CHAT --- REDIS[(Redis existente<br/>locks + rate limiting)]
    CHAT --- PUBSUB[(Redis Pub/Sub<br/>propio del chat)]
    CHAT -.fase 2.- CACHE[(Redis caché<br/>del proyecto)]
    AUTH[Auth / emisor JWT] -.->|JWKS / clave pública| CHAT
```

**Principios rectores**

1. **Base de datos por servicio.** El chat nunca lee tablas del backend principal y nadie lee las del chat.
2. **Autonomía en tiempo de ejecución.** Si el backend principal cae, el chat sigue funcionando para conversaciones existentes.
3. **Comunicación asíncrona por defecto.** El chat se entera de solicitudes y usuarios por eventos, no por llamadas síncronas.
4. **El chat no decide reglas de negocio ajenas.** No sabe *por qué* una solicitud se canceló; solo reacciona al evento.

---

## 3. Requisitos

Convención de identificadores: `RF-CHAT-XX` (funcionales), `RNF-CHAT-XX` (no funcionales). La columna **Fase** indica en qué etapa se entrega (ver sección 4). Prioridad según MoSCoW: **M**ust, **S**hould, **C**ould, **W**on't (por ahora).

### 3.1 Requisitos funcionales

#### Conversaciones

| ID | Requisito | Prioridad | Fase |
|---|---|---|---|
| RF-CHAT-01 | El sistema debe crear una conversación entre el cliente y el oferente **al crearse la solicitud de servicio** (DP-01). | M | 1 |
| RF-CHAT-02 | Solo puede existir **una** conversación activa por solicitud de servicio. | M | 1 |
| RF-CHAT-03 | Un usuario debe poder listar sus conversaciones ordenadas por actividad más reciente, con el último mensaje y el número de mensajes no leídos. | M | 1 |
| RF-CHAT-31 | El listado de conversaciones debe poder **filtrarse por estado de la conversación** (`ACTIVE`, `READ_ONLY`, …) y **por estado de la solicitud asociada** (los estados que maneje el módulo de Solicitudes), combinables entre sí y con selección de varios valores. En fase 2 se agrega el filtro por tipo (`REQUEST`, `DIRECT`). | M | 1 |
| RF-CHAT-04 | Un usuario solo puede ver y participar en conversaciones de las que es participante. | M | 1 |
| RF-CHAT-05 | Cuando la solicitud llega a un estado terminal (p. ej. rechazada, cancelada o finalizada), la conversación asociada pasa a **solo lectura de forma indefinida**: se conserva y se puede consultar el historial, pero nadie puede escribir ni se reabre (DP-02). | M | 1 |
| RF-CHAT-06 | Un **cliente** debe poder iniciar una conversación directa con un oferente **sin solicitud asociada**. El oferente puede responder, pero no puede iniciar este tipo de conversación (DP-05). | M | 2 |
| RF-CHAT-07 | Solo puede existir una conversación directa activa por par cliente–oferente. | M | 2 |
| RF-CHAT-08 | Un usuario debe poder archivar/ocultar una conversación de su listado sin afectar al otro participante. | S | 2 |
| RF-CHAT-09 | Un usuario debe poder bloquear a otro usuario para que no pueda escribirle. | S | 2 |

#### Mensajes

| ID | Requisito | Prioridad | Fase |
|---|---|---|---|
| RF-CHAT-10 | Un participante debe poder enviar mensajes de texto en una conversación activa. | M | 1 |
| RF-CHAT-11 | Los mensajes deben entregarse en **tiempo real** al otro participante si está conectado. | M | 1 |
| RF-CHAT-12 | Un usuario debe poder consultar el historial de una conversación con paginación (desde el más reciente hacia atrás). | M | 1 |
| RF-CHAT-13 | Los mensajes deben mostrarse en un orden consistente para ambos participantes. | M | 1 |
| RF-CHAT-14 | El sistema debe registrar hasta qué mensaje leyó cada participante (confirmación de lectura). | M | 1 |
| RF-CHAT-15 | Si el destinatario no está conectado, **el chat decide** avisarle: compone una notificación genérica (sin el contenido ni una vista previa del mensaje) con el nombre del emisor que ya tiene en `user_directory`, y emite un **comando de notificación** para que el módulo de Notificaciones solo lo **entregue** (push/email/in-app). Notificaciones no decide ni interpreta el mensaje (DP-04, ADR-011). | M | 1 |
| RF-CHAT-16 | Si el cliente reenvía un mensaje porque no recibió la confirmación del servidor (corte de red, timeout, reconexión), el sistema **no debe guardarlo ni entregarlo dos veces**: reconoce el reenvío por su `client_message_id` y responde con el mensaje ya guardado (idempotencia). | M | 1 |
| RF-CHAT-17 | El sistema debe validar la longitud máxima del mensaje (propuesta: 2 000 caracteres). | M | 1 |
| RF-CHAT-32 | Si un mensaje parece contener datos de contacto (teléfono, correo o enlace), el sistema debe **advertir** al emisor sobre los riesgos de negociar fuera de la plataforma, **sin bloquear** el mensaje, y marcarlo para métricas (DP-06). | S | 1 |
| RF-CHAT-18 | El sistema debe mostrar el indicador "escribiendo…". | C | 2 |
| RF-CHAT-19 | El sistema debe mostrar el estado en línea / última conexión del otro participante. | C | 2 |
| RF-CHAT-20 | Un usuario debe poder eliminar un mensaje propio (borrado lógico, se muestra "mensaje eliminado"). | C | 2 |

#### Adjuntos (fase 3)

| ID | Requisito | Prioridad | Fase |
|---|---|---|---|
| RF-CHAT-21 | Un participante debe poder adjuntar imágenes y documentos (tipos permitidos configurables, p. ej. JPG, PNG, PDF). | M | 3 |
| RF-CHAT-22 | El sistema debe validar tamaño máximo y tipo real (MIME por contenido, no por extensión). | M | 3 |
| RF-CHAT-23 | Los adjuntos solo deben ser descargables por los participantes de la conversación (URLs firmadas con expiración). | M | 3 |
| RF-CHAT-24 | El sistema debe generar miniaturas para imágenes. | C | 3 |

#### Soporte (fase 4)

| ID | Requisito | Prioridad | Fase |
|---|---|---|---|
| RF-CHAT-25 | Un usuario (cliente u oferente) debe poder abrir una conversación con soporte, que es atendida por un **administrador**. | M | 4 |
| RF-CHAT-26 | Los **administradores** deben ver una bandeja de conversaciones de soporte abiertas y poder asignárselas. | M | 4 |
| RF-CHAT-27 | Una conversación de soporte debe poder marcarse como resuelta y reabrirse. | S | 4 |
| RF-CHAT-28 | Un **administrador** debe poder consultar (solo lectura) una conversación cliente–oferente reportada, con registro de auditoría de cada acceso. | S | 4 |

#### Moderación y reportes (transversal)

| ID | Requisito | Prioridad | Fase |
|---|---|---|---|
| RF-CHAT-29 | Un participante debe poder reportar un mensaje o conversación; el chat publica un evento para el módulo de Reportes. | S | 2 |
| RF-CHAT-30 | Si un usuario es suspendido/bloqueado en la plataforma, no debe poder enviar mensajes. | M | 1 |

### 3.2 Requisitos no funcionales

| ID | Categoría | Requisito | Meta propuesta (ajustar) | Fase |
|---|---|---|---|---|
| RNF-CHAT-01 | Rendimiento | Latencia de entrega de un mensaje entre participantes conectados. | p95 < 500 ms | 1 |
| RNF-CHAT-02 | Rendimiento | Tiempo de respuesta de endpoints REST de lectura (listar conversaciones, historial). | p95 < 300 ms | 1 |
| RNF-CHAT-03 | Capacidad | Conexiones WebSocket concurrentes por instancia. | ≥ 1 000 en MVP | 1 |
| RNF-CHAT-04 | Escalabilidad | El servicio debe poder escalar horizontalmente (varias instancias) sin sesiones pegajosas. | Diseño listo desde MVP | 1 |
| RNF-CHAT-05 | Disponibilidad | Disponibilidad mensual del servicio. | ≥ 99 % (proyecto académico) | 1 |
| RNF-CHAT-06 | Durabilidad | Un mensaje confirmado (ACK) al emisor no se pierde. | 0 pérdidas | 1 |
| RNF-CHAT-07 | Consistencia | Orden total de mensajes dentro de cada conversación. | Secuencia por conversación | 1 |
| RNF-CHAT-08 | Confiabilidad | Los eventos publicados no se pierden si RabbitMQ no está disponible momentáneamente. | Patrón Outbox | 1 |
| RNF-CHAT-09 | Confiabilidad | El procesamiento de eventos consumidos es idempotente. | Tabla de eventos procesados | 1 |
| RNF-CHAT-10 | Seguridad | Todo tráfico externo cifrado. | HTTPS / WSS en el gateway | 1 |
| RNF-CHAT-11 | Seguridad | Autenticación con los JWT del sistema y autorización por participante en cada operación. | 100 % de endpoints | 1 |
| RNF-CHAT-12 | Seguridad | Límite de envío de mensajes por usuario (anti-spam). | p. ej. 20 msg / 10 s | 1 |
| RNF-CHAT-13 | Seguridad | El contenido de los mensajes no aparece en logs. | Obligatorio | 1 |
| RNF-CHAT-14 | Privacidad | Política de retención y eliminación de datos personales definida (Ley 1581 de 2012 en Colombia, si aplica). | Documentada | 2 |
| RNF-CHAT-15 | Resiliencia | Si el backend principal no está disponible, el chat sigue operando con conversaciones existentes. | Obligatorio | 1 |
| RNF-CHAT-16 | Resiliencia | Los clientes se reconectan automáticamente y recuperan mensajes perdidos durante la desconexión. | Obligatorio | 1 |
| RNF-CHAT-17 | Observabilidad | Logs estructurados (JSON) con ID de correlación; health checks; métricas básicas. | Obligatorio | 1 |
| RNF-CHAT-18 | Mantenibilidad | Arquitectura hexagonal; el dominio no depende de frameworks. | Revisión en PR | 1 |
| RNF-CHAT-19 | Mantenibilidad | Cobertura de pruebas en dominio y casos de uso. | ≥ 80 % en esas capas | 1 |
| RNF-CHAT-20 | Portabilidad | Despliegue reproducible con Docker y su propio `docker-compose`. | Obligatorio | 1 |
| RNF-CHAT-21 | Documentación | Contratos publicados: OpenAPI (REST) y AsyncAPI (eventos y WebSocket). | Obligatorio | 1 |
| RNF-CHAT-22 | Almacenamiento | Adjuntos fuera de la base de datos (almacenamiento de objetos). | Obligatorio | 3 |

> **Nota:** las metas numéricas son propuestas razonables para un proyecto de este tamaño. Ajustarlas con el equipo según la carga esperada y lo que se pueda medir realmente.

### 3.3 MVP

El MVP corresponde a la **Fase 1**: chat 1 a 1 asociado a una solicitud de servicio, solo texto.

**Incluye**

- RF-CHAT-01 a 05, 10 a 17, 30, 31 (filtros por estado de conversación y de solicitud) y 32 (advertencia de datos de contacto).
- RNF de fase 1 (tabla anterior).
- Una sola instancia en producción, pero **diseñada para escalar** (difusión entre instancias por la instancia Redis Pub/Sub propia del chat, ver §5.5).
- Contratos publicados: OpenAPI + AsyncAPI.

**No incluye**

- Chat sin solicitud, bloqueo entre usuarios, archivar, "escribiendo…", presencia, borrado de mensajes.
- Adjuntos, soporte, moderación avanzada.
- Edición de mensajes (se recomienda no implementarla nunca o muy tarde; complica el modelo y la moderación).

**Criterios de aceptación del MVP**

1. Al crearse una solicitud, cliente y oferente ven la conversación en su listado sin acción manual.
2. Un mensaje enviado por uno aparece en menos de 1 s en la pantalla del otro (ambos conectados).
3. Si el destinatario está desconectado, al reconectarse ve los mensajes en orden, y Notificaciones recibió el evento correspondiente.
4. Reenviar el mismo mensaje (mismo `client_message_id`) no crea duplicados.
5. Un tercero autenticado que intente leer o escribir en la conversación recibe 403 / cierre de conexión.
6. Al cancelarse la solicitud, la conversación pasa a solo lectura y su historial sigue disponible.
7. Reiniciar el contenedor del chat no pierde mensajes ya confirmados.
8. Detener RabbitMQ unos minutos no pierde eventos: se publican al volver.
9. El listado filtra correctamente por estado de la conversación, por estado de la solicitud y por ambos combinados.
10. Un mensaje con un número de teléfono se envía, pero el emisor ve la advertencia.

---

## 4. Hoja de ruta por fases

| Fase | Nombre | Contenido principal | Depende de |
|---|---|---|---|
| 0 | Cimientos | Repositorio, estructura hexagonal, Docker Compose, CI, health checks, validación de JWT, conexión a RabbitMQ y Redis | Acuerdos con Auth y Gateway |
| 1 | **MVP** | Chat por solicitud, solo texto, tiempo real, historial, lectura, eventos | Eventos de Solicitudes y Usuarios |
| 2 | Chat directo | Conversaciones sin solicitud, bloqueo, archivar, presencia, "escribiendo…", reportes | Reglas de negocio de contacto directo |
| 3 | Adjuntos | Subida/descarga con almacenamiento de objetos, validación, antivirus opcional | MinIO/S3 |
| 4 | Soporte | Bandeja de soporte para administradores, asignación, resolución, auditoría | Rol `ADMIN` en el JWT |

```mermaid
gantt
    dateFormat  YYYY-MM-DD
    title Hoja de ruta tentativa (ajustar al calendario del curso)
    section Base
    Fase 0 Cimientos      :f0, 2026-11-02, 7d
    section Entregas
    Fase 1 MVP            :f1, after f0, 21d
    Fase 2 Chat directo   :f2, after f1, 14d
    Fase 3 Adjuntos       :f3, after f2, 14d
    Fase 4 Soporte        :f4, after f3, 14d
```

---

## 5. Arquitectura del microservicio

### 5.1 Estilo: arquitectura hexagonal

Se mantiene la misma filosofía del backend principal: **Escuela B** (los puertos viven en la capa de `application`, no en `domain`), con organización por *feature* + capa. Esto permite que alguien que conoce el backend en Spring reconozca la estructura aunque el lenguaje cambie.

```mermaid
flowchart TB
    subgraph IN[Adaptadores de entrada]
        REST[HTTP REST<br/>FastAPI routers]
        WS[WebSocket gateway]
        CONS[Consumidores RabbitMQ]
    end
    subgraph APP[Application]
        UC[Casos de uso]
        PIN[Puertos de entrada]
        POUT[Puertos de salida]
    end
    subgraph DOM[Domain]
        ENT[Entidades, value objects,<br/>reglas e invariantes]
    end
    subgraph OUT[Adaptadores de salida]
        REPO[Repositorios MySQL]
        PUB[Publicador Outbox → RabbitMQ]
        BUS[Difusión en tiempo real<br/>Redis Pub/Sub]
        STORE[Almacenamiento de objetos<br/>fase 3]
    end
    REST --> PIN
    WS --> PIN
    CONS --> PIN
    PIN --> UC
    UC --> ENT
    UC --> POUT
    POUT -.implementa.- REPO
    POUT -.implementa.- PUB
    POUT -.implementa.- BUS
    POUT -.implementa.- STORE
```

**Regla de dependencias:** `infrastructure → application → domain`. El dominio no importa FastAPI, SQLAlchemy, Pydantic, aio-pika ni Redis.

### 5.2 Estructura de carpetas propuesta

```text
chat-service/
├── src/chat_service/
│   ├── conversations/
│   │   ├── domain/            # Conversation, Participant, ConversationStatus, reglas
│   │   ├── application/
│   │   │   ├── ports/         # in: casos de uso (interfaces) / out: repos, publicadores
│   │   │   ├── commands/      # dataclasses de entrada
│   │   │   └── use_cases/     # OpenRequestConversation, ListMyConversations, ...
│   │   └── infrastructure/
│   │       ├── http/          # routers FastAPI + schemas Pydantic
│   │       ├── persistence/   # modelos SQLAlchemy + adaptadores de repositorio
│   │       └── messaging/     # consumidores de eventos de solicitudes
│   ├── messaging/             # feature "mensajes" (Message, envío, historial, lectura)
│   │   ├── domain/
│   │   ├── application/
│   │   └── infrastructure/
│   │       ├── ws/            # endpoint WebSocket, gestor de conexiones locales
│   │       └── realtime/      # adaptador Redis Pub/Sub
│   ├── directory/             # proyecciones locales de usuarios y solicitudes
│   └── shared/
│       ├── domain/            # errores base, IDs, reloj
│       ├── application/       # unit of work, puerto de eventos
│       └── infrastructure/    # config, DB session, auth JWT, outbox, logging
├── migrations/                # Alembic
├── tests/
│   ├── unit/
│   ├── integration/
│   └── contract/
├── docs/
│   ├── adr/
│   ├── openapi.json           # generado
│   └── asyncapi.yaml
├── Dockerfile
├── docker-compose.yml
├── pyproject.toml
└── README.md
```

> El nombre `messaging` como feature puede confundirse con mensajería de RabbitMQ; si el equipo prefiere, llamarlo `messages`.

### 5.3 Casos de uso del MVP

| Caso de uso | Disparador | Puertos de salida que usa |
|---|---|---|
| `OpenRequestConversation` | Evento de solicitud habilitada | `ConversationRepository`, `EventPublisher` |
| `CloseRequestConversation` | Evento de solicitud cancelada/finalizada | `ConversationRepository`, `RealtimeNotifier` |
| `SendMessage` | WebSocket (o REST como respaldo) | `ConversationRepository`, `MessageRepository`, `RealtimeNotifier`, `EventPublisher`, `RateLimiter` |
| `GetConversationHistory` | REST | `MessageRepository` |
| `ListMyConversations` | REST | `ConversationReadModel` |
| `MarkAsRead` | WebSocket o REST | `ParticipantRepository`, `RealtimeNotifier` |
| `SyncUserProjection` | Eventos de usuarios | `UserDirectory` |

### 5.4 Flujo de envío de un mensaje

```mermaid
sequenceDiagram
    autonumber
    participant A as Cliente A (WS)
    participant I1 as chat-service instancia 1
    participant DB as MySQL
    participant R as Redis Pub/Sub
    participant I2 as chat-service instancia 2
    participant B as Cliente B (WS)
    participant O as Outbox relay
    participant MQ as RabbitMQ

    A->>I1: message.send {conversation_id, client_message_id, body}
    I1->>I1: Autoriza participante, valida, rate limit
    I1->>DB: TX: INSERT message (seq++) + INSERT outbox_event
    DB-->>I1: OK (o conflicto por client_message_id → devuelve el existente)
    I1-->>A: message.ack {client_message_id, message_id, seq, created_at}
    I1->>R: PUBLISH conv:{id} message.new
    R-->>I2: message.new
    I2-->>B: message.new
    O->>DB: lee outbox pendiente
    O->>MQ: chat.message.sent
    O->>DB: marca publicado
```

Puntos clave:

- **El ACK se envía después del commit**, nunca antes (RNF-CHAT-06).
- La difusión por Redis es *best effort*: si B no recibe el `message.new`, lo recupera al reconectarse pidiendo mensajes con `seq > último_seq_conocido` (RNF-CHAT-16).
- El evento a RabbitMQ sale por el **outbox**, no directamente desde el caso de uso (RNF-CHAT-08).

### 5.5 Uso de Redis

El chat usa tres instancias de Redis, cada una con una política acorde a lo que guarda:

| Instancia | Dueño | Uso | Política de memoria |
|---|---|---|---|
| **A — Locks / rate limiting** | Proyecto (existente, compartida) | Locks distribuidos y rate limiting de todo el sistema | Al llenarse **rechaza escrituras** (`noeviction`) |
| **B — Caché** | Proyecto (planeada, compartida) | Caché | Con desalojo de claves (p. ej. `allkeys-lru`) |
| **C — Pub/Sub del chat** | **Chat** (nueva, en su `docker-compose`) | Difusión de mensajes en tiempo real entre instancias del chat | Sin persistencia; límite de memoria y de buffers de suscriptores |

| Uso en el chat | Instancia | Patrón de clave / canal | TTL | Si falla o está llena |
|---|---|---|---|---|
| Rate limiting de envío de mensajes | A | `chat:rl:{user_id}:{ventana}` | Duración de la ventana | *Fail-open* con limitador en memoria por instancia + métrica y alerta |
| Tickets de conexión WebSocket | A | `chat:wsticket:{ticket}` | ~30 s | `503` en `POST /ws-tickets`; el cliente reintenta con backoff y mientras tanto usa REST |
| Difusión entre instancias (Pub/Sub) | **C** (DP-08) | Canales `chat:conv:{id}` y `chat:user:{id}` | No aplica | Los mensajes se guardan igual; el destinatario los recupera por `sync`/REST |
| Presencia y "escribiendo…" (fase 2) | **B** (DP-09) | `chat:presence:{user_id}`, `chat:typing:{conv_id}` | 30–60 s | Se muestra "desconectado"; no afecta mensajes |
| Locks distribuidos | **No se usan** | — | — | El orden de mensajes se garantiza en MySQL (§7.3) |

**Por qué este reparto**

- **Tickets y rate limiting en A:** no deben desaparecer por desalojo (un ticket desalojado rompe el login del WebSocket y un contador desalojado anula el límite), y su volumen es mínimo.
- **Pub/Sub en una instancia propia (C):** Redis atiende los comandos de cada instancia en un solo hilo principal, así que un volumen alto de difusión competiría con los locks y el rate limiting del sistema. Además, los buffers de los suscriptores cuentan como memoria usada; en la instancia A, que rechaza escrituras al llenarse, un suscriptor lento podría terminar bloqueando locks de otros módulos. Con su propia instancia, el chat escala la difusión sin afectar al resto y, si en el futuro una sola instancia no alcanza, puede migrar a Redis Cluster con Pub/Sub fragmentado (*sharded pub/sub*) sin tocar a los demás servicios.
- **Presencia en B:** son datos que se pueden perder sin daño; si se desalojan, alguien aparece "desconectado" unos segundos.
- El chat **no guarda mensajes ni conversaciones en Redis**: la fuente de verdad es MySQL.

**Reglas obligatorias en las instancias compartidas (A y B)**

1. **Toda clave lleva TTL.** Llenar la instancia A afecta locks y rate limiting de todo el sistema.
2. **Prefijo `chat:`** en todas las claves.
3. **Usuario ACL propio** (`chat`) restringido a claves `chat:*` y a los comandos necesarios.
4. **Prohibido** `KEYS`, `FLUSHDB`, `FLUSHALL` y operaciones O(N) sobre colecciones grandes.
5. **Presupuesto de memoria acordado** con infraestructura (DP-10) y medición periódica del uso por prefijo (job de diagnóstico con `SCAN` + `MEMORY USAGE`, nunca en el camino crítico).

**Configuración de la instancia C (propia)**

- Sin persistencia (`save ""`, `appendonly no`): los mensajes publicados son efímeros por diseño.
- `maxmemory` acotado y `client-output-buffer-limit pubsub` configurado para desconectar suscriptores lentos (el cliente se recupera con `sync`).
- Solo accesible desde la red interna del chat.

**Estimación de memoria en A (MVP):** con ~1 000 usuarios activos, tickets y contadores ocupan del orden de cientos de KB.

**Puertos de salida:** la aplicación solo conoce `RateLimiter` y `WsTicketStore` (adaptadores sobre A), `RealtimeNotifier` (sobre C) y `PresenceStore` (sobre B, fase 2). Cambiar de instancia es un cambio de configuración, sin tocar dominio ni casos de uso.

---

## 6. Stack tecnológico

Recomendación para Python:

| Necesidad | Elección recomendada | Alternativas consideradas |
|---|---|---|
| Lenguaje | **Python 3.12+** | Node.js (TypeScript), Go, Java |
| Framework web + WebSocket | **FastAPI** (sobre Starlette, servidor Uvicorn) | Django Channels, Litestar |
| Validación / esquemas | **Pydantic v2** (solo en adaptadores) | marshmallow |
| ORM / acceso a datos | **SQLAlchemy 2 (async)** + driver `asyncmy` (o `aiomysql`) | SQLModel, Tortoise ORM |
| Migraciones | **Alembic** (nativo de Python; equivale al Flyway que usan los microservicios Java del proyecto) | — |
| Base de datos | **MySQL 8.4** (instancia propia del chat) — es el motor del proyecto; análisis en §7.5 | PostgreSQL, MongoDB, Cassandra/ScyllaDB (descartadas, §7.5) |
| Tickets WS y rate limiting | **Redis existente** de locks/rate limiting (prefijo `chat:`, usuario ACL) — §5.5 | Redis dedicado |
| Difusión entre instancias | **Redis Pub/Sub propio del chat** — §5.5 | RabbitMQ para fan-out; Pub/Sub en la instancia de locks |
| Presencia (fase 2) | **Redis de caché** del proyecto — §5.5 | — |
| Cliente Redis | **redis-py** (modo `asyncio`) | — |
| Cliente RabbitMQ | **aio-pika** (o **FastStream** si se quiere AsyncAPI generado) | pika (síncrono) |
| JWT | **PyJWT** (hoy **HS256** con secreto compartido, igual que el resto de ServiYa — DP-03; soporte JWKS listo por si se migra a RS256) | python-jose |
| Configuración | **pydantic-settings** (variables de entorno) | dynaconf |
| Pruebas | **pytest**, pytest-asyncio, httpx, **Testcontainers** | — |
| Calidad | **Ruff** (lint + formato), **mypy**, **import-linter** (verifica capas hexagonales) | flake8, black, pylint |
| Gestión de dependencias | **uv** o Poetry | pip + requirements |
| Logs | **structlog** (JSON) | logging estándar |

**Por qué Python + FastAPI encaja aquí:** el chat es un servicio intensivo en **E/S** (muchas conexiones abiertas esperando), no en CPU. El modelo `asyncio` maneja miles de WebSockets por proceso con poco consumo, FastAPI tiene WebSocket nativo y genera OpenAPI automáticamente, y la curva de aprendizaje es baja.

**Lo que se pierde frente a Java/Spring:** no se reutilizan librerías ni convenciones del backend principal, y el equipo debe mantener dos stacks. Se mitiga manteniendo la misma arquitectura (hexagonal, Escuela B) y contratos bien documentados.

> Verificar las versiones estables vigentes de cada librería al iniciar la Fase 0 y fijarlas en `pyproject.toml`.

---

## 7. Modelo de dominio y datos

### 7.1 Conceptos de dominio

- **Conversation:** canal 1 a 1. Tiene un **tipo** (`REQUEST`, `DIRECT`, `SUPPORT`), un **contexto** opcional (`context_id` = id de la solicitud cuando es `REQUEST`) y un **estado**: `ACTIVE` o `READ_ONLY` para conversaciones de solicitud y directas; `ACTIVE` o `RESOLVED` para soporte. No hay estado de cierre definitivo: la solo lectura es indefinida (DP-02). Las conversaciones `REQUEST` guardan además una copia del **estado actual de la solicitud** (`context_status`) para poder filtrar sin consultar otro servicio (§7.4).
- **Participant:** usuario dentro de una conversación, con su **rol** en ella (`CLIENT`, `OFFERER`, `ADMIN`) y su `last_read_seq`. El rol `ADMIN` solo aparece en conversaciones de soporte.
- **Message:** contenido inmutable con número de **secuencia** dentro de la conversación.
- **Proyecciones locales:** copias mínimas de datos de otros servicios (usuarios, solicitudes) que el chat necesita para autorizar y mostrar, mantenidas por eventos.

**Invariantes del dominio**

1. Una conversación `REQUEST` tiene exactamente un `CLIENT` y un `OFFERER`.
2. Solo se escribe en conversaciones `ACTIVE`.
3. Solo los participantes pueden leer o escribir. Excepción (fase 4): un `ADMIN` puede **leer** una conversación reportada; cada acceso queda auditado.
4. La secuencia de mensajes es estrictamente creciente y sin huecos dentro de una conversación.
5. Un mensaje no cambia de contenido después de creado (solo puede marcarse como eliminado, fase 2).
6. Una conversación `DIRECT` solo la crea el cliente (DP-05).
7. En conversaciones `REQUEST`, `READ_ONLY` es definitivo: no vuelven a `ACTIVE` (DP-02).

> **Por qué `type` + `context_id` desde el MVP:** permite añadir chat directo (fase 2) y soporte (fase 4) sin migrar datos ni rediseñar la tabla.

### 7.2 Esquema relacional propuesto

```mermaid
erDiagram
    CONVERSATIONS ||--|{ CONVERSATION_PARTICIPANTS : tiene
    CONVERSATIONS ||--o{ MESSAGES : contiene
    MESSAGES ||--o{ ATTACHMENTS : "fase 3"

    CONVERSATIONS {
        bigint id PK
        varchar type "REQUEST | DIRECT | SUPPORT"
        bigint context_id "id de solicitud, nullable"
        varchar status "ACTIVE | READ_ONLY | RESOLVED"
        varchar context_status "estado de la solicitud, nullable"
        datetime context_status_at "fecha del evento que lo fijó"
        bigint last_seq
        datetime last_message_at
        datetime created_at
    }
    CONVERSATION_PARTICIPANTS {
        bigint conversation_id PK,FK
        bigint user_id PK
        varchar role "CLIENT | OFFERER | ADMIN"
        bigint last_read_seq
        datetime archived_at "fase 2"
        datetime joined_at
    }
    MESSAGES {
        bigint id PK
        bigint conversation_id FK
        bigint seq
        bigint sender_id
        char client_message_id "token opaco del cliente (UUID char(36))"
        varchar kind "TEXT | SYSTEM | ATTACHMENT"
        json flags "p. ej. [\"CONTACT_INFO\"]"
        text body
        datetime created_at
        datetime deleted_at "fase 2"
    }
    ATTACHMENTS {
        bigint id PK
        bigint message_id FK
        varchar storage_key
        varchar mime_type
        bigint size_bytes
    }
```

> **Tipos (MySQL 8.4):** todos los identificadores son `BIGINT` para alinear con el resto de ServiYa (usuarios y solicitudes son `BIGINT`). `context_id` guarda el `request_id` del monolito. `client_message_id` **no** es un id del servidor sino un token de idempotencia generado por el cliente, por lo que se guarda como `CHAR(36)` (un UUID del frontend) y nunca se interpreta. `flags` usa una columna `JSON` (MySQL no tiene arrays nativos). Las fechas son `DATETIME(6)` en UTC.

Tablas de soporte técnico:

| Tabla | Propósito |
|---|---|
| `outbox_events` | Eventos pendientes de publicar en RabbitMQ (id, tipo, payload JSON, created_at, published_at). |
| `processed_events` | IDs de eventos consumidos para garantizar idempotencia. |
| `user_directory` | Proyección: `user_id`, nombre visible, avatar, estado (activo/suspendido), `updated_at`. |
| `request_directory` | Proyección: `request_id`, `client_id`, `offerer_id`, estado, `updated_at`. |

**Restricciones e índices clave**

> **MySQL 8.4 no tiene índices parciales (`WHERE ...`) como PostgreSQL.** El equivalente es una **columna generada** que vale `NULL` salvo para el tipo que interesa, más un índice único normal: en MySQL un índice `UNIQUE` admite múltiples `NULL`, así que la unicidad solo aplica a las filas marcadas. Lo mismo para los índices "solo de soporte".

```sql
-- Una conversación REQUEST por solicitud: columna generada NULL salvo cuando type='REQUEST'
ALTER TABLE conversations
  ADD COLUMN request_ctx BIGINT
    GENERATED ALWAYS AS (IF(type = 'REQUEST', context_id, NULL)) VIRTUAL;
CREATE UNIQUE INDEX ux_conv_request ON conversations (request_ctx);

-- Orden y unicidad de secuencia
CREATE UNIQUE INDEX ux_msg_conv_seq ON messages (conversation_id, seq);

-- Idempotencia de envío
CREATE UNIQUE INDEX ux_msg_client_id
  ON messages (conversation_id, sender_id, client_message_id);

-- Listado y filtros de un usuario (§7.4)
CREATE INDEX ix_participant_user ON conversation_participants (user_id, conversation_id);
CREATE INDEX ix_conv_last_message ON conversations (last_message_at DESC, id DESC);

-- Bandeja de soporte para administradores (fase 4): clave compuesta solo para SUPPORT
ALTER TABLE conversations
  ADD COLUMN support_last_message_at DATETIME(6)
    GENERATED ALWAYS AS (IF(type = 'SUPPORT', last_message_at, NULL)) VIRTUAL;
CREATE INDEX ix_conv_support_inbox ON conversations (status, support_last_message_at DESC);
```

Para chat directo (fase 2), la unicidad por par se resuelve con una columna `pair_key` generada: `NULL` salvo cuando `type = 'DIRECT'`, con valor `CONCAT(LEAST(user_a,user_b), ':', GREATEST(user_a,user_b))`, y un índice `UNIQUE` sobre ella (mismo patrón de columna generada + único).

### 7.3 Asignación de la secuencia

Dentro de la misma transacción del `INSERT` (MySQL 8.4 no tiene `RETURNING`, así que se bloquea la fila con `SELECT ... FOR UPDATE` y luego se incrementa):

```sql
-- 1) Bloquea la fila de la conversación y verifica que siga activa.
--    Si no devuelve fila → inexistente o en solo lectura → se rechaza el envío.
SELECT last_seq
  FROM conversations
 WHERE id = :conversation_id AND status = 'ACTIVE'
   FOR UPDATE;

-- 2) El nuevo seq es (last_seq leído + 1). Con la fila ya bloqueada nadie más lo cambia.
UPDATE conversations
   SET last_seq = last_seq + 1, last_message_at = NOW(6)
 WHERE id = :conversation_id;
```

El `SELECT ... FOR UPDATE` bloquea la fila de la conversación, lo que serializa los envíos **solo dentro de esa conversación** (en un chat 1 a 1 la contención es mínima) y a la vez verifica que siga activa.

### 7.4 Listado con filtros por estado

Para cumplir RF-CHAT-31 sin llamar al módulo de Solicitudes en cada consulta, el estado de la solicitud se **desnormaliza** en la conversación (`context_status`), y se actualiza al consumir el evento de cambio de estado (§9.2).

```sql
-- Los filtros multivaluados se expresan con IN (...) y el adaptador OMITE la cláusula
-- cuando la lista viene vacía (MySQL no tiene arrays ni ANY(array) como PostgreSQL).
-- Las partes entre /* ... */ las agrega el repositorio solo si hay filtro.
SELECT c.id, c.type, c.status, c.context_id, c.context_status,
       c.last_message_at,
       c.last_seq - p.last_read_seq AS unread_count,
       m.sender_id AS last_sender_id, m.body AS last_body, m.kind AS last_kind
  FROM conversation_participants p
  JOIN conversations c ON c.id = p.conversation_id
  LEFT JOIN messages m ON m.conversation_id = c.id AND m.seq = c.last_seq
 WHERE p.user_id = :user_id
   AND p.archived_at IS NULL
   /* AND c.status         IN (:statuses)          -- solo si hay estados de conversación */
   /* AND c.context_status IN (:request_statuses)  -- solo si hay estados de solicitud   */
   AND (:cursor_ts IS NULL OR (c.last_message_at, c.id) < (:cursor_ts, :cursor_id))
 ORDER BY c.last_message_at DESC, c.id DESC
 LIMIT :limit;
```

Notas:

- **Filtros multivaluados:** MySQL no tiene arrays; el repositorio construye un `IN (...)` con tantos marcadores como valores y **omite** la cláusula cuando la lista está vacía (no se interpola texto del usuario: cada valor es un parámetro). La comparación de tupla `(last_message_at, id) < (:cursor_ts, :cursor_id)` sí funciona en MySQL 8.
- **Escala:** un usuario tiene decenas o cientos de conversaciones, así que el índice por `user_id` reduce el conjunto y los filtros se aplican sobre pocas filas. No hace falta un índice por cada combinación de filtros.
- **Eventos fuera de orden:** se guarda `context_status_at` (el `occurred_at` del evento) y se ignora cualquier evento más antiguo que el último aplicado.
- **No leídos:** `last_seq - last_read_seq`. Al enviar un mensaje, el emisor avanza su propio `last_read_seq` hasta ese mensaje para no contarlo como no leído.
- **Conversación recién creada:** `last_message_at` se inicializa con la fecha de creación para que aparezca en el listado y en el orden correcto.
- **Valores de `context_status`:** el chat guarda el estado tal como lo publica Solicitudes (texto), sin interpretarlo, salvo la lista configurable de estados terminales que pasan la conversación a solo lectura. Así, si Solicitudes agrega un estado, el filtro funciona sin cambiar el chat.

### 7.5 Elección del motor de base de datos: ¿relacional o no relacional?

**Patrones de acceso del chat**

1. Guardar un mensaje, incrementar la secuencia y registrar el evento en el outbox **en una sola transacción**.
2. Leer el historial de una conversación por rango de secuencia (acceso clave + rango).
3. Listar las conversaciones de un usuario con filtros (estado, estado de solicitud, tipo), ordenadas por actividad y con no leídos.
4. Garantizar unicidad: una conversación por solicitud, una directa por par cliente–oferente, un mensaje por `client_message_id`.
5. Actualizar proyecciones (*upsert*) a partir de eventos, de forma idempotente.

**Volumen estimado (orden de magnitud):** 10 000 solicitudes/mes × 30 mensajes ≈ 300 000 mensajes/mes ≈ 3,6 M/año, unos pocos GB con índices. Incluso multiplicado por 100, sigue dentro de lo que MySQL maneja con comodidad usando particionado.

> **Decisión del proyecto:** el motor es **MySQL 8.4**, el mismo que usan el monolito y el `notifications-service`. El análisis de abajo confirma que una base **relacional** cubre los cinco patrones; se mantiene como relacional (antes se proponía PostgreSQL) para no introducir un tercer motor que operar. Las diferencias prácticas con PostgreSQL ya están aplicadas en §7.2–§7.4 (sin índices parciales → columnas generadas; `JSON` en vez de `JSONB`; sin `RETURNING` → `SELECT ... FOR UPDATE`; `FULLTEXT` en vez de `GIN/tsvector`).

| Criterio | MySQL 8.4 (relacional, el del proyecto) | MongoDB | Cassandra / ScyllaDB |
|---|---|---|---|
| Transacción mensaje + secuencia + outbox | Nativa (InnoDB) | Multi-documento solo con *replica set* (también en desarrollo); más configuración | No hay transacciones entre tablas; el outbox tal como está diseñado no es posible |
| Historial por conversación y rango | Muy bueno con índice `(conversation_id, seq)` | Muy bueno | Excelente, su caso ideal a escala masiva |
| Listado con filtros combinados y no leídos | Directo (join + índices) | Requiere desnormalizar en documentos por participante | Una tabla por consulta; filtros combinados difíciles |
| Unicidad e idempotencia | Índices únicos; "parciales" vía columna generada (§7.2) | Índices únicos y parciales | Solo con transacciones ligeras (LWT), costosas |
| Esquema flexible (adjuntos, mensajes de sistema) | Columnas `JSON` | Nativo | Limitado |
| Escala de escritura | Vertical + particionado; suficiente para decenas a cientos de millones de mensajes | Horizontal con *sharding* | Horizontal lineal, para miles de millones |
| Consistencia | Fuerte | Configurable | Eventual / ajustable |
| Conocimiento del equipo y operación | Ya se usa en todo el proyecto | Tecnología nueva | Nueva y compleja (clúster de varios nodos recomendado) |

**Conclusión: no es necesaria una base de datos no relacional, y basta con el MySQL que el proyecto ya opera.**

- **MongoDB** no aporta nada que MySQL con columnas `JSON` no cubra, y complica la transacción del outbox.
- **Cassandra/ScyllaDB** resuelven un problema de escala que este proyecto no tiene, a cambio de perder transacciones, unicidad sencilla y filtros flexibles, que son justo lo que el chat necesita.
- **MySQL 8.4** cubre todos los patrones con garantías fuertes (InnoDB) y reutiliza el motor y el conocimiento del equipo. PostgreSQL también serviría, pero añadiría un motor más a la operación sin beneficio para este volumen.

**Recomendaciones sobre MySQL**

- **Instancia propia del chat** (no una base dentro del servidor del backend principal), para escalar y respaldar de forma independiente.
- **Particionado de `messages`** cuando supere unas decenas de millones de filas. Preferir partición por `KEY`/`HASH(conversation_id)`: en tablas particionadas de MySQL **toda clave única/primaria debe incluir la columna de partición**, así que la PK sería `(conversation_id, id)` o `(conversation_id, seq)` y `(conversation_id, seq)` sigue siendo único. Si se necesita archivar por antigüedad, evaluar particionado por rango de fecha y ajustar la unicidad.
- **Pool de conexiones** (ProxySQL, o el pool de SQLAlchemy/`asyncmy`) si se despliegan varias instancias del chat.
- **Búsqueda en mensajes** (si se pide en el futuro): índice `FULLTEXT` sobre `body` (parser `ngram` para español/CJK según haga falta) antes de pensar en un motor de búsqueda aparte.

**Cuándo reconsiderar una no relacional**

- Escritura sostenida de miles de mensajes por segundo o cientos de millones de filas donde el particionado ya no alcanza.
- En ese caso, la opción recomendada es **híbrida**: conversaciones, participantes, proyecciones y outbox siguen en MySQL, y solo el historial de `messages` se mueve a Cassandra/ScyllaDB. El puerto `MessageRepository` de la arquitectura hexagonal permite ese cambio sin tocar dominio ni casos de uso.

---

## 8. Interfaces: API REST y protocolo WebSocket

### 8.1 API REST (a través del Gateway)

Prefijo público: `/api/chat/v1`. Todas requieren `Authorization: Bearer <JWT>`.

| Método | Ruta | Descripción | Fase |
|---|---|---|---|
| `GET` | `/conversations?status=&request_status=&type=&cursor=&limit=` | Lista mis conversaciones (último mensaje, no leídos). `status` y `request_status` aceptan varios valores separados por coma y se combinan entre sí; `type` desde fase 2 | 1 |
| `GET` | `/conversations/{id}` | Detalle de una conversación | 1 |
| `GET` | `/conversations/by-request/{requestId}` | Conversación asociada a una solicitud | 1 |
| `GET` | `/conversations/{id}/messages?before_seq=&after_seq=&limit=` | Historial paginado por secuencia | 1 |
| `POST` | `/conversations/{id}/messages` | Envío por REST (respaldo si WS no está disponible) | 1 |
| `POST` | `/conversations/{id}/read` | Marcar leído hasta `seq` | 1 |
| `POST` | `/ws-tickets` | Emite ticket de un solo uso para abrir el WebSocket | 1 |
| `POST` | `/conversations` | Abrir conversación directa con un oferente | 2 |
| `POST` | `/blocks` / `DELETE /blocks/{userId}` | Bloquear / desbloquear | 2 |
| `POST` | `/conversations/{id}/attachments` | Solicitar subida de adjunto (URL firmada) | 3 |
| `GET` | `/support/conversations?status=&assigned_to=` | Bandeja de soporte (solo `ADMIN`) | 4 |
| `GET` | `/admin/conversations/{id}/messages` | Lectura auditada de una conversación reportada (solo `ADMIN`) | 4 |

Endpoints internos (no expuestos por el gateway): `GET /health/live`, `GET /health/ready`, `GET /metrics`.

**Errores:** formato uniforme alineado con el backend principal (recomendado: RFC 9457 *Problem Details*):

```json
{
  "type": "https://marketplace/errors/conversation-read-only",
  "title": "La conversación es de solo lectura",
  "status": 409,
  "code": "CHAT_CONVERSATION_READ_ONLY",
  "traceId": "c3f1..."
}
```

**Paginación:** por cursor (secuencia o `last_message_at`), no por `offset`, para que lleguen mensajes nuevos sin desordenar las páginas.

### 8.2 WebSocket

- **URL:** `wss://<gateway>/api/chat/v1/ws?ticket=<ticket>`
- **Una sola conexión por cliente** (dispositivo/pestaña), multiplexando todas sus conversaciones.
- **Formato:** JSON con sobre común.

```json
{
  "type": "message.send",
  "id": "b7c1...",          
  "payload": { }
}
```

| Dirección | `type` | Payload | Fase |
|---|---|---|---|
| C → S | `message.send` | `conversation_id`, `client_message_id`, `body` | 1 |
| S → C | `message.ack` | `client_message_id`, `message_id`, `seq`, `created_at`, `warnings` (p. ej. `CONTACT_INFO`, RF-CHAT-32) | 1 |
| S → C | `message.new` | mensaje completo | 1 |
| C → S | `conversation.read` | `conversation_id`, `seq` | 1 |
| S → C | `conversation.read_updated` | `conversation_id`, `user_id`, `seq` | 1 |
| S → C | `conversation.status_changed` | `conversation_id`, `status`, `context_status` | 1 |
| S → C | `conversation.created` | conversación | 1 |
| C → S | `sync` | lista de `{conversation_id, last_seq}` tras reconectar | 1 |
| S → C | `error` | `code`, `message`, `ref_id` | 1 |
| C ↔ S | `ping` / `pong` | — | 1 |
| C → S | `typing.start` / `typing.stop` | `conversation_id` | 2 |
| S → C | `presence.changed` | `user_id`, `online`, `last_seen` | 2 |

**Reglas del protocolo**

- El servidor cierra la conexión con código `4401` si el ticket es inválido, y `4403` si el usuario es suspendido.
- *Heartbeat* cada ~25 s para atravesar proxies y detectar conexiones muertas.
- El cliente reintenta con *backoff* exponencial con jitter y, al reconectar, envía `sync`.
- Mensajes entrantes con tamaño máximo (p. ej. 8 KB por frame) para evitar abusos.

---

## 9. Integración asíncrona (eventos RabbitMQ)

### 9.1 Convenciones

- **Exchange** tipo *topic* por dominio emisor: `serviya.requests`, `serviya.users`, `serviya.chat` (prefijo `serviya.*`, igual que el exchange `serviya.notifications` que el proyecto ya opera).
- **Routing key:** `<dominio>.<entidad>.<evento>` en pasado, p. ej. `chat.message.sent`.
- **Sobre común** de todos los eventos:

```json
{
  "event_id": "uuid",
  "event_type": "chat.message.sent",
  "event_version": 1,
  "occurred_at": "2026-10-26T12:00:00Z",
  "producer": "chat-service",
  "correlation_id": "uuid",
  "data": { }
}
```

- Entrega **al menos una vez**: los consumidores deben ser idempotentes (`processed_events`).
- Cada cola de consumo tiene su **DLQ**.

> **Sobre común aún por ratificar en ServiYa.** Este sobre es una propuesta del chat. Hoy el único evento que el proyecto publica (`notification.requested.v1`, exchange `serviya.notifications`) usa un formato distinto y con payload **cifrado**. El equipo de ServiYa debe **establecer un sobre de eventos estándar** para todos los servicios antes de la Fase 1 (es un pendiente abierto del lado del monolito, no algo que el chat pueda asumir ya hecho). Los `*_id` del `data` son **`BIGINT`** (ids de ServiYa), no UUID.

### 9.2 Eventos que el chat consume

> **Aún no existen en el monolito.** Hoy ServiYa no publica eventos de dominio de Solicitudes ni de Usuarios al bus (los flujos cross-dominio internos usan eventos de aplicación Spring in-process, que no salen del proceso). Publicar estos eventos es trabajo **planeado** de la extracción del monolito y es **bloqueante para el MVP del chat**. Ver el documento de infraestructura (§4 y §5).

| Evento | Emisor | Acción en el chat | Fase |
|---|---|---|---|
| `requests.service_request.created` | Backend – Solicitudes | Actualiza `request_directory`; abre la conversación `REQUEST` (DP-01) | 1 |
| `requests.service_request.status_changed` | Backend – Solicitudes | Actualiza `context_status` (filtros, §7.4) y lo avisa por WS; si el nuevo estado es terminal, pasa la conversación a `READ_ONLY` indefinidamente (DP-02) | 1 |
| `users.user.registered` / `updated` | Backend – Auth/Perfiles | Actualiza `user_directory` (nombre, avatar) | 1 |
| `users.user.suspended` / `reactivated` / `deleted` | Backend – Auth | Bloquea o reactiva envío; en `deleted`, anonimiza | 1 |

> **Estados terminales (DP-11, ya resuelto por el código de ServiYa):** son terminales `REJECTED`, `CANCELLED`, `COMPLETED`, `RESCHEDULED` y `NOT_PROVIDED` (`RequestStatus.isTerminal()`). `PENDING`, `ACCEPTED` y `PRESUMABLY_COMPLETED` **no** lo son. Esa es la lista que pasa la conversación a `READ_ONLY`.

### 9.3 Eventos que el chat publica

| Evento | Consumidores esperados | Datos clave | Fase |
|---|---|---|---|
| `chat.conversation.created` | Métricas | `conversation_id`, `type`, `context_id`, participantes | 1 |
| `chat.message.sent` | Métricas | `conversation_id`, `message_id`, `sender_id`, `recipient_ids`, `recipient_online`, `flags`. **Sin contenido ni vista previa** (DP-04) | 1 |
| `notification.requested.v1` (comando) | **Notificaciones** (contrato existente `serviya.notifications`) | Notificación **ya compuesta por el chat**: destinatario, canal(es) y texto genérico («Tienes un mensaje nuevo de {nombre}»). **Sin el cuerpo del mensaje** | 1 |
| `chat.conversation.read_only` | Métricas (opcional) | `conversation_id`, motivo | 1 |
| `chat.message.reported` | Reportes | `message_id`, `reporter_id`, motivo | 2 |
| `chat.support.ticket_opened` / `resolved` | Soporte, Métricas | `conversation_id`, `user_id` | 4 |

> **Decisión importante (ADR-011, revisada para ServiYa):** el módulo de Notificaciones es un **ejecutor puro** — solo entrega en las modalidades que haga falta (push/email/in-app) y **no decide ni interpreta** el mensaje. Por eso:
> - **El chat decide** si notificar (ya conoce `recipient_online`) y **compone** el texto genérico con el nombre del emisor que tiene en `user_directory`.
> - El chat **emite el comando `notification.requested.v1`** por el contrato que ServiYa ya usa (exchange `serviya.notifications`), igual que hoy lo hace el monolito. Notificaciones solo lo entrega.
> - `chat.message.sent` deja de ser consumido por Notificaciones: **queda solo como evento de dominio para Métricas** (y sigue sin contenido, por privacidad, DP-04).
> - El comando de notificación **no** lleva el cuerpo del mensaje, solo el texto genérico ya compuesto.

### 9.4 Patrón Outbox

1. El caso de uso guarda el cambio de negocio y el evento en `outbox_events` en **la misma transacción**.
2. Un proceso *relay* (tarea en segundo plano del mismo servicio) lee los pendientes, publica con *publisher confirms* y marca `published_at`.
3. Si RabbitMQ falla, los eventos esperan en la tabla; nada se pierde.

---

## 10. Seguridad

### 10.1 Autenticación

- El chat **no emite** tokens: valida los JWT emitidos por el módulo Auth.
- Validación de **firma** (**hoy ServiYa firma con HS256 y secreto compartido** — DP-03; preferible migrar a RS256/ES256 con JWKS, ver documento de infraestructura), `exp`, y `iss`/`aud` cuando el emisor los incluya (hoy el token solo trae `sub` y `roles`).
- El gateway puede validar también, pero el chat **siempre valida** (defensa en profundidad; nunca confiar solo en cabeceras inyectadas).

### 10.2 Autenticación del WebSocket con ticket

Los navegadores no permiten cabeceras personalizadas en el *handshake* WebSocket, y poner el JWT en la URL lo expone en logs del gateway. Por eso:

1. El cliente llama `POST /api/chat/v1/ws-tickets` con su JWT normal.
2. El chat genera un ticket aleatorio, lo guarda en Redis con TTL de ~30 s asociado al `user_id`.
3. El cliente abre `wss://.../ws?ticket=...`; el chat lo consume (borrado atómico) y asocia la conexión al usuario.
4. Al expirar el JWT original, el servidor puede cerrar la conexión (`4401`) para forzar renovación.

### 10.3 Autorización

- Cada operación verifica que el usuario sea participante de la conversación **en el caso de uso**, no solo en el adaptador.
- La creación de conversaciones `REQUEST` solo ocurre por eventos del sistema, no por petición del usuario.
- En fase 2, solo un cliente puede crear conversaciones `DIRECT` (DP-05); se valida que el destinatario sea oferente activo y no haya bloqueo.
- En fase 4, la bandeja de soporte y la lectura de conversaciones reportadas exigen el rol `ADMIN` del JWT, y cada lectura de moderación queda en un registro de auditoría.

### 10.4 Otras medidas

- **Rate limiting** de envíos por usuario en la instancia Redis de rate limiting (además del límite general del gateway), con *fail-open* controlado según §5.5.
- **Validación de origen** (`Origin`) en el handshake WebSocket contra una lista permitida.
- **Contenido:** se guarda texto plano; el escape contra XSS es responsabilidad del frontend al renderizar. Nunca renderizar HTML de mensajes.
- **Logs:** nunca registrar el `body` de los mensajes ni tokens.
- **Secretos:** solo por variables de entorno / *Docker secrets*, nunca en el repositorio.
- **Adjuntos (fase 3):** URL firmadas de corta duración, validación de MIME real, tamaño máximo, nombres de archivo regenerados.

---

## 11. Escalabilidad y confiabilidad

| Tema | Estrategia |
|---|---|
| Varias instancias | Cada instancia mantiene un mapa local `user_id → conexiones`. Para entregar a un usuario conectado a otra instancia se publica en la instancia Redis Pub/Sub propia del chat (`chat:conv:{id}` o `chat:user:{id}`). No se requieren sesiones pegajosas. |
| Reconexión | El cliente envía `sync` con su último `seq` por conversación; el servidor devuelve lo faltante. |
| Idempotencia | `client_message_id` único por emisor y conversación. |
| Orden | Secuencia por conversación asignada en BD. |
| Durabilidad | ACK solo tras commit; eventos por outbox. |
| Caída del backend principal | El chat usa sus proyecciones locales; no hay llamadas síncronas en el camino crítico. |
| Caída del Redis Pub/Sub (C) | Degradación: los mensajes se guardan y se recuperan por `sync`/REST; se pierde solo la entrega instantánea entre instancias. `/health/ready` reporta estado degradado. |
| Redis A lleno (rechaza escrituras) | El chat no puede emitir tickets ni actualizar contadores: responde `503` en tickets, aplica el limitador en memoria y registra alerta. Las conexiones ya abiertas siguen funcionando. Ver §5.5. |
| Caída del Redis de caché (B, fase 2) | La presencia y "escribiendo…" se muestran como desconocidos; mensajes y conversaciones no se afectan. |
| Caída de RabbitMQ | Los eventos esperan en outbox. |
| Cierre ordenado | Al recibir SIGTERM: dejar de aceptar conexiones, enviar cierre `1001` a los clientes (reconectan a otra instancia), terminar relay. |

---

## 12. Despliegue

- Imagen Docker multi-etapa, usuario no root, `HEALTHCHECK` contra `/health/live`.
- `docker-compose.yml` propio con: `chat-service`, `chat-db` (MySQL 8.4, el motor del proyecto) y `chat-pubsub` (Redis dedicado a Pub/Sub). RabbitMQ, Gateway y las instancias **Redis de locks/rate limiting y de caché** son **compartidos** y se alcanzan por una **red Docker externa** común (ver documento de infraestructura).
- Migraciones con Alembic ejecutadas como paso previo al arranque (servicio `migrate` de un solo uso o comando de entrada).
- Configuración 100 % por variables de entorno; incluir `.env.example` documentado.

Ejemplo orientativo:

```yaml
services:
  chat-service:
    build: .
    env_file: .env
    depends_on:
      chat-db: { condition: service_healthy }
      chat-pubsub: { condition: service_healthy }
    # REDIS_URL        → instancia compartida de locks/rate limiting (redis://chat:***@redis-coord:6379/0)
    # REDIS_PUBSUB_URL → instancia propia (redis://chat-pubsub:6379/0)
    # REDIS_CACHE_URL  → instancia compartida de caché, desde fase 2
    networks: [chat-internal, marketplace-shared]

  chat-db:
    image: mysql:8.4
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    environment:
      MYSQL_DATABASE: chat_db
      MYSQL_USER: ${CHAT_DB_USER}
      MYSQL_PASSWORD: ${CHAT_DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${CHAT_DB_ROOT_PASSWORD}
    volumes: [chat-db-data:/var/lib/mysql]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    networks: [chat-internal]

  chat-pubsub:
    image: redis:7
    command: >
      redis-server --save "" --appendonly no
      --maxmemory 256mb
      --client-output-buffer-limit pubsub 32mb 8mb 60
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
    networks: [chat-internal]

volumes:
  chat-db-data:

networks:
  chat-internal:
  marketplace-shared:
    external: true
```

> Solo `chat-service` está en la red compartida; la base de datos y `chat-pubsub` quedan aisladas en la red interna del chat. Los valores de memoria de `chat-pubsub` son de partida y se ajustan con pruebas de carga. Como las instancias Redis compartidas son externas al compose, el servicio no debe fallar al arrancar si tardan en responder: reintenta y reporta `ready = false` mientras tanto.

---

## 13. Registro de decisiones (ADR)

Formato resumido. Cada ADR debe existir también como archivo en `docs/adr/NNNN-titulo.md` (ver documento complementario).

### ADR-001 — Chat como microservicio independiente
- **Contexto:** el chat tiene carga y patrón de uso muy distintos al resto (conexiones persistentes, tiempo real).
- **Decisión:** servicio aparte con BD propia, comunicación por gateway y eventos.
- **Consecuencias:** + escalado y despliegue independientes, tecnología libre. − duplicación de datos (proyecciones), consistencia eventual, más operación.

### ADR-002 — Python + FastAPI
- **Decisión:** Python 3.12+, FastAPI, asyncio.
- **Alternativas:** Node.js (excelente para tiempo real, pero el equipo no lo domina igual), Java/Spring (reutiliza conocimiento, pero el objetivo es independencia y el stack reactivo de Spring tiene más curva), Go (rendimiento superior, curva alta).
- **Consecuencias:** desarrollo rápido, OpenAPI automático; un segundo stack que mantener.

### ADR-003 — WebSocket nativo con protocolo JSON propio
- **Alternativas:** STOMP (útil en Spring, poco natural en Python), Socket.IO (agrega su propio protocolo y librería obligatoria en el cliente), SSE + POST (unidireccional, más simple pero dos canales), *long polling*.
- **Decisión:** WebSocket nativo, sobre JSON versionado, REST como respaldo.
- **Consecuencias:** máximo control y cero dependencias en el cliente; hay que implementar heartbeat y reconexión.

### ADR-004 — MySQL 8.4 propio para mensajes (sin base no relacional)
- **Contexto:** se evaluó si el chat necesita una base no relacional (análisis completo en §7.5). ServiYa opera MySQL en el monolito y en `notifications-service`.
- **Alternativas:** PostgreSQL, MongoDB, Cassandra/ScyllaDB, modelo híbrido.
- **Decisión:** **MySQL 8.4** en instancia propia del chat, el mismo motor del proyecto. Cubre los cinco patrones de acceso con transacciones (InnoDB), unicidad (índices únicos; "parciales" vía columna generada) y JSON, y el volumen esperado está muy por debajo de sus límites. Se descartó PostgreSQL por no introducir un tercer motor sin beneficio para este volumen.
- **Consecuencias:** + garantías fuertes, outbox simple, filtros flexibles, un solo motor que operar en todo el proyecto. − sin índices parciales nativos (se emulan con columnas generadas), sin `RETURNING` (se usa `SELECT ... FOR UPDATE`), escala horizontal de escritura limitada.
- **Revisar si:** se sostienen miles de mensajes por segundo o el particionado no alcanza → mover solo `messages` a Cassandra/ScyllaDB (modelo híbrido).

### ADR-005 — Reparto de Redis: instancias compartidas + Pub/Sub propio
- **Contexto:** el proyecto ya opera una instancia de locks y rate limiting (rechaza escrituras al llenarse) y planea otra de caché con desalojo.
- **Alternativas:** todo en la instancia de locks; un único Redis dedicado al chat; RabbitMQ para el fan-out en tiempo real; sesiones pegajosas en el gateway.
- **Decisión:** tickets y rate limiting en la instancia existente de locks/rate limiting; Pub/Sub en una **instancia propia del chat** (DP-08); presencia en la instancia de caché (DP-09). Aislamiento lógico por prefijo `chat:` y usuario ACL en las compartidas.
- **Consecuencias:** + el tráfico de tiempo real no afecta locks ni rate limiting del sistema y escala aparte; cada dato está en la instancia cuya política le conviene. − una instancia más que operar (dentro del compose del chat) y dependencia de la instancia de caché para la fase 2.

### ADR-006 — Proyecciones locales alimentadas por eventos
- **Alternativa:** consultar al backend principal por HTTP en cada operación.
- **Decisión:** el chat mantiene copias mínimas de usuarios y solicitudes.
- **Consecuencias:** autonomía y baja latencia; consistencia eventual (segundos). Se requiere un mecanismo de carga inicial (*backfill*).

### ADR-007 — Modelo genérico de conversación (`type` + `context_id`)
- **Decisión:** una sola tabla para `REQUEST`, `DIRECT` y `SUPPORT`.
- **Consecuencia:** las fases 2 y 4 no requieren rediseño.

### ADR-008 — Secuencia por conversación + `client_message_id`
- **Decisión:** orden total por `seq` asignado en BD e idempotencia por id generado en el cliente.
- **Alternativas:** ordenar por timestamp (relojes y empates), UUIDv7 solo (no garantiza ausencia de huecos para detectar faltantes).

### ADR-009 — Outbox transaccional
- **Decisión:** todo evento sale por `outbox_events`.
- **Consecuencia:** cero pérdidas a cambio de un pequeño retraso y una tabla más.

### ADR-010 — Ticket de un solo uso para el WebSocket
- **Decisión:** ver §10.2.
- **Alternativas:** JWT en query string (se filtra en logs), JWT en el primer mensaje (deja conexiones sin autenticar abiertas por un momento; aceptable como plan B), cookies (requiere mismo dominio y protección CSRF).

### ADR-011 — Notificaciones como ejecutor puro; el chat decide y compone
- **Contexto:** en ServiYa el módulo de Notificaciones debe limitarse a la **lógica de entrega** (push/email/in-app) y no ejercer lógica sobre el mensaje. El contrato existente `serviya.notifications` / `notification.requested.v1` ya funciona así: recibe una notificación lista y solo la entrega.
- **Decisión:** **el chat decide** si notificar (conoce `recipient_online`) y **compone** el texto genérico (tiene el nombre del emisor en `user_directory`), y emite el comando `notification.requested.v1` por el contrato existente. Notificaciones solo entrega, sin decidir ni interpretar. El evento de dominio `chat.message.sent` queda **solo para Métricas** y sigue sin contenido (DP-04).
- **Alternativas:** que Notificaciones consuma `chat.message.sent` y decida/componga (rechazada: le daría lógica de negocio del chat y acoplaría ambos módulos).
- **Consecuencias:** + Notificaciones permanece genérico y reutilizable; + reutiliza el contrato que ya existe. − el chat asume la decisión y la plantilla del texto, y necesita el nombre del emisor en su proyección.

### ADR-012 — Estado de la solicitud desnormalizado en la conversación
- **Contexto:** RF-CHAT-31 exige filtrar por estado de la solicitud.
- **Alternativas:** llamar a Solicitudes en cada listado (acoplamiento síncrono y lentitud); unir con `request_directory` en cada consulta (válido, pero más costoso que una columna).
- **Decisión:** columna `context_status` en `conversations`, actualizada por el evento `status_changed` con protección contra eventos fuera de orden (`context_status_at`).
- **Consecuencias:** filtros rápidos y autónomos; consistencia eventual de segundos respecto a Solicitudes.

### ADR-013 — Advertir, no bloquear, los datos de contacto
- **Contexto:** riesgo de que las partes cierren el trato fuera de la plataforma (DP-06).
- **Decisión:** detección en el servidor (expresiones regulares para teléfonos, correos y enlaces) dentro del caso de uso `SendMessage`; el mensaje se guarda y entrega, el ACK incluye `warnings` y el mensaje queda con `flags` para métricas.
- **Consecuencias:** no se frustra al usuario con falsos positivos (precios, direcciones, códigos); la detección es fácil de evadir, así que es disuasiva, no una barrera. La regla vive en el dominio y puede endurecerse después sin cambiar el protocolo.

---

## 14. Pruebas

> **Adaptar al proyecto:** si el proyecto ya define una estrategia de pruebas (convenciones de nombres, cobertura mínima, pipeline de CI, plantillas de la materia), esta sección se ajusta a ella. Las herramientas son recomendaciones para Python; lo que no es negociable es que las áreas marcadas como **Obligatorio** queden cubiertas.

### 14.1 Pirámide de pruebas del chat

```mermaid
flowchart TB
    E2E["E2E / humo<br/>pocas: flujos críticos del MVP"]
    CON["Contrato<br/>eventos consumidos/publicados, REST y WS"]
    INT["Integración<br/>adaptadores con MySQL, Redis y RabbitMQ reales"]
    UNIT["Unitarias<br/>dominio y casos de uso — la mayoría"]
    E2E --- CON --- INT --- UNIT
```

La arquitectura hexagonal es la que hace barata esta pirámide: los casos de uso se prueban con *fakes* en memoria de los puertos (`ConversationRepository`, `RealtimeNotifier`, `RateLimiter`…), sin levantar infraestructura.

### 14.2 Qué es obligatorio y qué recomendable

| Área del microservicio | Tipo | Nivel | Qué verificar como mínimo |
|---|---|---|---|
| Dominio (entidades e invariantes) | Unitaria | **Obligatorio** | Las invariantes de §7.1; `ACTIVE → READ_ONLY` sin retorno en `REQUEST`; `ACTIVE ↔ RESOLVED` en soporte; solo el cliente crea `DIRECT`; mensajes inmutables |
| Casos de uso | Unitaria con fakes de puertos | **Obligatorio** | No participante no lee ni escribe; no se escribe en solo lectura; usuario suspendido no envía; idempotencia por `client_message_id`; evento agregado al outbox; la detección de datos de contacto marca el mensaje y devuelve `warnings` sin bloquearlo (incluir casos que **no** deben detectarse, como precios) |
| Repositorios MySQL | Integración (Testcontainers) | **Obligatorio** | Índices únicos (§7.2), asignación de `seq` sin huecos, paginación por cursor, listado con no leídos; filtros por estado de conversación, por estado de solicitud y combinados; eventos de estado fuera de orden ignorados |
| Concurrencia de envío | Integración | **Obligatorio** | N envíos simultáneos a la misma conversación → `seq` 1..N, sin duplicados ni huecos |
| Outbox relay | Integración (MySQL + RabbitMQ) | **Obligatorio** | Publica pendientes; con el broker caído no marca como publicado ni pierde eventos; reanuda al volver |
| Consumidores de eventos | Integración | **Obligatorio** | Mismo `event_id` dos veces → un solo efecto; payload inválido → DLQ; `service_request.created` abre la conversación; `status_changed` actualiza `context_status`; estado terminal → solo lectura definitiva |
| Autenticación | Unitaria + integración | **Obligatorio** | JWT vencido, firma inválida, `iss`/`aud` incorrectos → 401 |
| Ticket de WebSocket | Integración (Redis) | **Obligatorio** | Ticket válido conecta; reutilizado o expirado → cierre `4401`; la clave tiene TTL |
| API REST | Integración (`httpx` + app FastAPI) | **Obligatorio** | Códigos de estado y formato de error de §8.1; 403 a no participantes; paginación |
| Protocolo WebSocket | Integración | **Obligatorio** | `message.send` → `message.ack` → `message.new` al otro participante; `sync` tras reconexión devuelve lo faltante; heartbeat; cierres `4401/4403`; frame demasiado grande rechazado |
| Contratos de eventos consumidos | Contrato | **Obligatorio** | Ejemplos reales de los eventos de Solicitudes y Usuarios validan contra el esquema de `asyncapi.yaml` del chat |
| Contratos de eventos publicados | Contrato | **Obligatorio** | Cada evento `chat.*` generado valida contra su esquema y su `event_version` |
| Contrato REST | Contrato | **Obligatorio** | El `openapi.json` exportado no cambia de forma incompatible sin subir versión (diff en CI) |
| Reglas de arquitectura | Estática (`import-linter`) | **Obligatorio** | `domain` no importa `application` ni `infrastructure`; ninguna capa interna importa FastAPI, SQLAlchemy, Pydantic, redis o aio-pika |
| Uso del Redis compartido | Integración | **Obligatorio** | Toda clave escrita por el chat tiene prefijo `chat:` y TTL (prueba que hace `SCAN chat:*` y verifica `TTL > 0`) |
| Degradación con Redis lleno o caído | Integración | **Obligatorio** | Simular error de escritura (OOM) y caída: tickets → `503`, rate limiting pasa al limitador en memoria, los mensajes se siguen guardando (§5.5, §11) |
| Difusión entre instancias | Integración (dos instancias del app + `chat-pubsub`) | Recomendable | Mensaje enviado en la instancia 1 llega a un cliente conectado en la instancia 2; con `chat-pubsub` caído, el cliente lo recupera por `sync` |
| Rate limiting | Integración | Recomendable | Superar el umbral produce `error` con código de límite; al pasar la ventana se permite de nuevo |
| E2E del MVP (chat aislado) | E2E con productor *stub* | **Obligatorio** | Publicar `service_request.created` falso → aparece conversación → dos clientes WS intercambian mensajes → se publica `chat.message.sent` |
| Rendimiento | Carga (Locust o k6) | Recomendable antes de cerrar el MVP | RNF-CHAT-01 (p95 < 500 ms) y RNF-CHAT-03 (≥ 1 000 conexiones por instancia) |
| Seguridad de dependencias e imagen | Estática (`pip-audit`, Trivy) | Recomendable | Sin vulnerabilidades críticas conocidas; sin secretos en el repo |
| Adjuntos (fase 3) | Integración con MinIO | **Obligatorio en fase 3** | MIME real, tamaño máximo, URL firmada solo para participantes, expiración |
| Soporte (fase 4) | Unitaria + integración | **Obligatorio en fase 4** | Solo `ADMIN` ve la bandeja y lee conversaciones reportadas; cada lectura queda auditada; un no administrador recibe 403 |

### 14.3 Organización y herramientas

```text
tests/
├── unit/            # dominio y casos de uso, sin I/O, muy rápidas
│   └── fakes/       # implementaciones en memoria de los puertos
├── integration/     # Testcontainers: MySQL, Redis, RabbitMQ
├── contract/        # validación contra openapi.json y asyncapi.yaml
│   └── samples/     # payloads de ejemplo acordados con otros equipos
└── e2e/             # flujo del MVP con productor stub
```

| Necesidad | Herramienta recomendada |
|---|---|
| Ejecutor | `pytest` + `pytest-asyncio` |
| Cliente HTTP / WS de prueba | `httpx`, cliente de pruebas de FastAPI/Starlette, `websockets` |
| Infraestructura real en pruebas | `testcontainers` (MySQL, Redis, RabbitMQ) |
| Datos de prueba | *Builders* propios o `factory-boy` |
| Validación de contratos | `jsonschema` contra los esquemas de AsyncAPI/OpenAPI |
| Cobertura | `pytest-cov` |
| Arquitectura | `import-linter` |
| Carga | Locust o k6 |

### 14.4 Lineamientos

- **Cobertura:** ≥ 80 % en `domain` y `application` (RNF-CHAT-19). En `infrastructure` importa que existan las pruebas de integración de la tabla, no el porcentaje.
- **Nombres que describan comportamiento:** `test_no_participante_no_puede_enviar_mensaje`, `test_evento_duplicado_no_crea_segunda_conversacion`.
- **Una prueba de integración nunca depende de otra**; cada una crea sus datos y limpia (o usa transacción revertida).
- **Nada de `sleep` fijos** en pruebas asíncronas; esperar condiciones con *timeout*.
- **CI:** en cada PR corren lint, tipos (`mypy`), `import-linter`, unitarias y contrato; integración y E2E en cada PR si el tiempo lo permite, o como mínimo antes de fusionar a la rama principal.
- **Muestras de contrato compartidas:** los payloads en `tests/contract/samples/` se acuerdan con los equipos productores; cuando cambien sus eventos, deben actualizar la muestra en el mismo PR que el cambio de versión.

---

## 15. Documentación

> **Adaptar al proyecto:** si el proyecto ya usa una estructura de documentación (wiki, carpeta `docs/` del backend, plantillas de la materia, formato de ADR), se reutiliza y se enlaza desde aquí en lugar de duplicar. Lo indispensable es que exista cada artefacto marcado como **Obligatorio**, en el formato que el proyecto use.

### 15.1 Qué documentar en el microservicio

| Artefacto | Nivel | Formato / cómo se produce | Ubicación |
|---|---|---|---|
| Esta guía | **Obligatorio** | Markdown, se actualiza con cada decisión | `docs/guia-microservicio-chat.md` |
| ADRs | **Obligatorio** | Markdown con la plantilla de §15.3; uno por decisión | `docs/adr/NNNN-titulo.md` |
| API REST | **Obligatorio** | OpenAPI generado por FastAPI, exportado en CI y versionado | `docs/openapi.json` |
| Eventos y protocolo WebSocket | **Obligatorio** | AsyncAPI escrito a mano (o generado si se usa FastStream); incluye canales RabbitMQ y mensajes WS | `docs/asyncapi.yaml` |
| README | **Obligatorio** | Markdown, contenido mínimo en §15.2 | Raíz del repo |
| Variables de entorno | **Obligatorio** | `.env.example` con comentario por variable | Raíz del repo |
| Uso de Redis | **Obligatorio** | Tabla de claves, canales, TTL y presupuesto (mantener §5.5 actualizada) | Esta guía |
| Runbook | Recomendable (obligatorio si hay despliegue real) | Markdown, contenido mínimo en §15.4 | `docs/runbook.md` |
| Diagramas (contexto, componentes, secuencias críticas) | Recomendable | Mermaid dentro de Markdown | Esta guía / `docs/` |
| Docstrings | Recomendable | En puertos y casos de uso: qué hacen, errores que lanzan | Código |
| Changelog | Recomendable | `CHANGELOG.md` siguiendo *Keep a Changelog* | Raíz del repo |

### 15.2 Contenido mínimo del README

1. Qué es el servicio, qué hace y qué **no** hace (enlace a esta guía).
2. Requisitos previos: Docker, red `marketplace-shared`, acceso a RabbitMQ y Redis compartidos.
3. Cómo levantarlo, correr migraciones y ejecutar cada tipo de prueba.
4. Variables de entorno (enlace a `.env.example`).
5. Enlaces a `openapi.json`, `asyncapi.yaml` y la carpeta de ADRs.
6. Cómo probar el WebSocket manualmente (obtener ticket y conectarse con un cliente simple).

### 15.3 Plantilla de ADR

```markdown
# ADR-NNNN: Título corto

- **Estado:** Propuesto | Aceptado | Reemplazado por ADR-XXXX | Obsoleto
- **Fecha:** AAAA-MM-DD
- **Decisores:** nombres

## Contexto
Qué problema o restricción obliga a decidir.

## Decisión
Qué se decidió, en una o dos frases.

## Alternativas consideradas
- Opción A — por qué no.
- Opción B — por qué no.

## Consecuencias
Positivas, negativas y qué habrá que vigilar.
```

Los ADR-001 a 013 de §13 son el punto de partida: se pasan a archivos individuales al aceptarse.

### 15.4 Contenido mínimo del runbook

- Cómo verificar que el servicio está sano (`/health/live`, `/health/ready`, logs, métricas clave).
- Mensajes en la DLQ: cómo inspeccionarlos, corregir y reencolar.
- Outbox que crece sin publicarse: qué revisar (conexión a RabbitMQ, credenciales, relay detenido).
- Redis lleno o caído: síntomas en el chat, impacto en el resto del sistema y a quién avisar.
- Cómo relanzar la carga inicial de proyecciones (`user_directory`, `request_directory`).
- Cómo escalar o reiniciar instancias y qué verán los usuarios (reconexión automática).

### 15.5 Lineamientos

- **La documentación vive junto al código** y se actualiza en el mismo PR que el cambio que la afecta; el PR no se aprueba si cambia un contrato sin actualizar OpenAPI/AsyncAPI.
- **Los contratos son la fuente de verdad:** si el código y el contrato difieren, es un bug.
- **Versionado de contratos:** un cambio incompatible en un evento sube `event_version` y ambas versiones conviven durante la transición; en REST se crea `/v2`.
- **Diagramas como código** (Mermaid) para que se vean en GitHub/GitLab y se mantengan al día.
- **Documentar el porqué**, no solo el qué: para eso son los ADR.

---

## 16. Consideraciones y recomendaciones

**De diseño**

- **Empezar por el contrato.** Escribir primero `asyncapi.yaml` y los esquemas REST, acordarlos con frontend y backend, y luego implementar.
- **El dominio en Python puro.** Usar `dataclasses` en dominio y aplicación; Pydantic solo en adaptadores. Hacer cumplir la regla con `import-linter` en CI.
- **No implementar edición de mensajes.** Si se pide, preferir "eliminar y reenviar".
- **Mensajes de sistema** (`kind = SYSTEM`) para hitos: "La solicitud fue aceptada", "La conversación se cerró". Dan contexto y evitan pantallas confusas.
- **Fechas en UTC** siempre en el servidor; el frontend convierte a la zona del usuario.

**De producto y negocio**

- **Desintermediación (DP-06: advertir).** El chat es el canal por el que las partes podrían intercambiar teléfonos y cerrar por fuera. Se detectan teléfonos, correos y enlaces y se advierte al emisor sin bloquear (ADR-013). Afinar las expresiones regulares con ejemplos reales para evitar falsos positivos con precios y direcciones, y usar los `flags` para medir si el fenómeno crece. Ojo: el perfil ya expone `whatsapp_number`, así que conviene decidir qué papel tiene cada canal.
- **Notificaciones sin contenido (DP-04).** Más privado y simple; a cambio, la notificación no da contexto. Si se reconsidera, el cambio es agregar un campo al evento y subir `event_version`.
- **Retención:** definir cuánto tiempo se conservan las conversaciones cerradas y qué pasa al eliminar una cuenta (recomendado: anonimizar el remitente, no borrar mensajes del otro participante).

**De implementación**

- **Testcontainers** para pruebas de integración con MySQL, Redis y RabbitMQ reales.
- **Cliente de prueba de WebSocket** (script o página HTML mínima) desde la Fase 0 para depurar sin depender del frontend.
- **Pruebas de carga ligeras** antes de cerrar el MVP (p. ej., Locust o k6) para validar RNF-CHAT-01 y 03.
- **Un solo proceso Uvicorn por contenedor**; escalar con más contenedores, no con más *workers* dentro del mismo (simplifica el mapa de conexiones).
- **Carga inicial de proyecciones:** acordar un endpoint interno o un *dump* para poblar `user_directory` y `request_directory` la primera vez, ya que los eventos solo traen cambios futuros.

**Riesgos**

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El backend principal no publica los eventos a tiempo | Bloquea el MVP | Acordar contratos en Fase 0; *mock* de productor para desarrollo |
| Gateway sin soporte adecuado de WebSocket | Bloquea tiempo real | Probar el *upgrade* en Fase 0 |
| Desincronización de proyecciones | Usuarios ven datos viejos o fallan autorizaciones | Idempotencia, DLQ, *backfill* y reproceso |
| Equipo poco familiar con asyncio | Bugs de concurrencia, bloqueos del event loop | Prohibir librerías síncronas en el camino async; revisión en PR |
| Llenar el Redis compartido | Afecta locks y rate limiting de todo el sistema | TTL obligatorio, prefijo y ACL, presupuesto de memoria (DP-10), prueba específica (§14) |

---

## 17. Decisiones pendientes y resueltas

### 17.1 Resueltas

| ID | Pregunta | Decisión | Fecha | Dónde impacta |
|---|---|---|---|---|
| DP-01 | ¿En qué momento de la solicitud se habilita el chat? | **Al crearse** la solicitud | 2026-10-26 | RF-CHAT-01, §9.2 |
| DP-02 | ¿Qué pasa con el chat al cerrarse la solicitud? | **Solo lectura indefinida** | 2026-10-26 | RF-CHAT-05, §7.1 |
| DP-04 | ¿El evento lleva contenido/vista previa, y quién decide y compone la notificación? | **No** lleva contenido. **El chat decide y compone**; emite `notification.requested.v1` por el contrato existente y Notificaciones solo entrega. `chat.message.sent` queda solo para Métricas (revisado 2026-11-06, ADR-011) | 2026-10-26 / 2026-11-06 | RF-CHAT-15, §9.3, ADR-011 |
| DP-05 | ¿Quién inicia un chat directo (sin solicitud)? | **Solo el cliente** | 2026-10-26 | RF-CHAT-06, §7.1, §10.3 |
| DP-06 | ¿Detección de datos de contacto? | **Advertir** sin bloquear | 2026-10-26 | RF-CHAT-32, ADR-013 |
| DP-08 | ¿Dónde vive el Pub/Sub? | **Instancia propia del chat**, separada de locks y rate limiting | 2026-10-26 | §5.5, ADR-005, §12 |
| DP-09 | ¿Dónde vive la presencia? | **Instancia de caché** del proyecto | 2026-10-26 | §5.5, ADR-005 |
| — | ¿Quién atiende el soporte? | Los **administradores** (rol `ADMIN`), no un rol de soporte aparte | 2026-10-26 | RF-CHAT-25 a 28, §7.1 |

### 17.2 Pendientes

| ID | Pregunta | Opciones | Responsable | Estado |
|---|---|---|---|---|
| DP-03 | ¿Algoritmo de firma de los JWT actuales? | HS256 (secreto compartido) / RS256 con JWKS | Equipo Auth | **Resuelto de hecho:** hoy es **HS256 con secreto compartido** (el chat replica lo que ya hace `notifications-service`). Migrar a RS256/JWKS queda como deuda técnica |
| DP-07 | Política de retención de mensajes | — | PO + legal | Abierta |
| DP-10 | Presupuesto de memoria del chat en la instancia de locks/rate limiting | Valor en MB + umbral de alerta | Infra | Abierta |
| DP-11 | ¿Cuáles estados de la solicitud son terminales (pasan el chat a solo lectura)? | Lista según la máquina de estados de Solicitudes | Equipo Solicitudes | **Resuelto por el código:** `REJECTED`, `CANCELLED`, `COMPLETED`, `RESCHEDULED`, `NOT_PROVIDED` (`RequestStatus.isTerminal()`); `PENDING`/`ACCEPTED`/`PRESUMABLY_COMPLETED` no lo son |

---

## 18. Glosario

| Término | Definición |
|---|---|
| ACK | Confirmación del servidor de que un mensaje quedó guardado. |
| ADR | *Architecture Decision Record*, registro breve de una decisión de arquitectura. |
| AsyncAPI | Especificación para documentar APIs basadas en eventos y WebSocket, análoga a OpenAPI. |
| Backfill | Carga inicial de datos históricos en una proyección. |
| DLQ | *Dead Letter Queue*, cola donde van los mensajes que no se pudieron procesar. |
| Desnormalizar | Guardar una copia de un dato en otra tabla para consultarlo sin uniones ni llamadas a otro servicio. |
| Fan-out | Difundir un mismo mensaje a varios destinatarios o instancias. |
| Idempotencia | Propiedad por la que repetir una operación produce el mismo resultado que hacerla una vez. |
| JWKS | Conjunto de claves públicas publicado por el emisor de JWT para verificar firmas. |
| Outbox | Patrón que guarda eventos en la BD dentro de la misma transacción y los publica después. |
| Proyección | Copia local y simplificada de datos de otro servicio, mantenida por eventos. |
| Seq | Número de secuencia de un mensaje dentro de su conversación. |
