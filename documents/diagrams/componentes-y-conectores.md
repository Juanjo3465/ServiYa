# Componentes y conectores de ServiYa

> Diagramas para entender el monolito modular, sus conectores y el sistema desplegado.
> Fuente de verdad del estado actual: `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` y
> `documents/arquitectura/apis-modulos.md`.
>
> **Cómo renderizarlos:** abre este archivo en GitHub, en VS Code (extensión Mermaid) o pega cada
> bloque en <https://mermaid.live>. Los bloques son independientes.

## Convenciones

- Un **módulo de negocio** publica `com.parosurvivors.serviya.<modulo>.api` (interfaces + DTOs).
- Las llamadas entre módulos son **en proceso**, a través de esas APIs; nunca importan `domain`,
  `infrastructure` ni puertos de persistencia ajenos.
- `metrics`, `notifications`, `profiles` y `services` son **módulos hoja**: solo dependen de `shared`.
- `bff`, `admin`, `reports` y `statistics` son consumidores/orquestadores de lectura o composición.
- `config` es la raíz de composición: cablea implementaciones y adaptadores de integración.
- El grafo de dependencias entre módulos es **acíclico** (`ModuleBoundaryTest`).

## Versiones renderizadas (SVG)

| Diagrama | SVG |
|---|---|
| 1. Vista de contenedores | [01-vista-contenedores.svg](./componentes-y-conectores/01-vista-contenedores.svg) |
| 2. Componentes y conectores del monolito | [02-componentes-conectores-monolito.svg](./componentes-y-conectores/02-componentes-conectores-monolito.svg) |
| 3. Anatomía hexagonal de un módulo | [03-anatomia-hexagonal-modulo.svg](./componentes-y-conectores/03-anatomia-hexagonal-modulo.svg) |
| 4. Grafo de módulos | [04-grafo-modulos.svg](./componentes-y-conectores/04-grafo-modulos.svg) |
| 5. Mapa del BFF | [05-mapa-bff.svg](./componentes-y-conectores/05-mapa-bff.svg) |
| 6. Secuencia de petición HTTP | [06-secuencia-peticion-http.svg](./componentes-y-conectores/06-secuencia-peticion-http.svg) |
| 7. Eventos de dominio | [07-eventos-dominio.svg](./componentes-y-conectores/07-eventos-dominio.svg) |
| 8. Outbox + RabbitMQ | [08-outbox-rabbit.svg](./componentes-y-conectores/08-outbox-rabbit.svg) |
| 9. Mapa de datos | [09-mapa-datos.svg](./componentes-y-conectores/09-mapa-datos.svg) |

---

## 1. Vista de contenedores (runtime)

```mermaid
flowchart LR
    U["Usuarios<br/>cliente · oferente · admin"]

    subgraph EDGE["Borde HTTP"]
        FE["Frontend<br/>React + Vite/Nginx<br/>:5173 dev · :8081 prod"]
        GW["API Gateway<br/>Spring Cloud Gateway<br/>:8080"]
    end

    subgraph CORE["Núcleo transaccional"]
        MONO["Monolito modular<br/>Spring Boot<br/>:8080 interno"]
    end

    subgraph EXTRACTED["Servicio extraído"]
        NOTI["notifications-service<br/>Spring Boot<br/>:8082 interno"]
    end

    subgraph DATA["Datos e infraestructura"]
        MYSQL[("MySQL monolito<br/>:3307 host")]
        NMYSQL[("MySQL notificaciones<br/>:3308 host")]
        RABBIT[("RabbitMQ<br/>:15672 consola")]
        REDIS[("Redis coord<br/>red interna")]
    end

    BREVO["Brevo API<br/>correo externo"]

    U -->|"HTTPS"| FE
    FE -->|"HTTP/JSON"| GW
    GW -->|"/api/v1/notifications/**"| NOTI
    GW -->|"resto de /api/**"| MONO

    MONO -->|"JPA / JDBC"| MYSQL
    MONO -->|"outbox + AMQP"| RABBIT
    RABBIT -->|"notification-requested"| NOTI
    NOTI -->|"JPA / JDBC"| NMYSQL
    NOTI -->|"HTTPS API"| BREVO

    MONO -->|"rate limit + locks"| REDIS
    NOTI -->|"PING ACL"| REDIS
```

**Lectura rápida**

- El gateway es el único punto de entrada HTTP del host.
- Solo `notifications-service` está extraído; el resto sigue en el monolito modular.
- El monolito y el servicio de notificaciones **no comparten base de datos**.
- El bus se usa para el flujo de notificaciones; los eventos de dominio internos del monolito son
  in-process.
- Redis se usa para rate limiting/locks en el monolito y para el sondeo de disponibilidad en
  `notifications-service`.

---

## 2. Componentes y conectores del monolito

```mermaid
flowchart TB
    subgraph INBOUND["Adaptadores de entrada"]
        BFF["BFF REST<br/>PublicOfferer · ServiceCatalog<br/>MetricsRead · AvailabilityTemplate"]
        ADMIN["Admin / Moderation REST<br/>AdminController · ModerationController"]
        REST["REST de módulos<br/>users · profiles · services · requests<br/>feedback · reports · metrics · statistics"]
        LISTENERS["Event listeners<br/>@TransactionalEventListener<br/>metrics · profiles"]
        SCHED["Schedulers<br/>maintenance · outbox relay<br/>password-reset cleanup"]
    end

    subgraph APPS["Capa de aplicación"]
        FACADES["Fachadas de API pública<br/>&lt;modulo&gt;.application.api<br/>traducen snapshots ↔ dominio"]
        USE["Casos de uso<br/>&lt;modulo&gt;.application.services<br/>transacciones · reglas de aplicación"]
        PORTS["Puertos<br/>input / output"]
    end

    subgraph DOMAIN["Dominio"]
        ENTITIES["Entidades · agregados · políticas<br/>&lt;modulo&gt;.domain"]
    end

    subgraph KERNEL["Kernel transversal"]
        SHARED["shared<br/>contract · events · security<br/>ratelimit · textfilter"]
    end

    subgraph OUTBOUND["Adaptadores de salida"]
        PERSIST["Persistencia JPA<br/>adapters + repositories"]
        OUTBOX["Outbox de notificaciones<br/>NotificationOutboxPersistenceAdapter"]
        REDISAD["Redis<br/>rate limiter · locks"]
        EMAILAD["Email recipient adapter<br/>config.integration"]
    end

    subgraph DATA["Datos"]
        DB[("MySQL del monolito")]
        BUS[("RabbitMQ")]
    end

    BFF --> USE
    ADMIN --> USE
    REST --> USE
    LISTENERS --> USE
    SCHED --> USE

    USE --> PORTS
    USE --> ENTITIES
    USE --> SHARED
    FACADES --> USE

    PORTS --> PERSIST
    PORTS --> OUTBOX
    PORTS --> EMAILAD
    SHARED --> REDISAD

    PERSIST --> DB
    OUTBOX --> DB
    SCHED -->|"poll + publish"| OUTBOX
    OUTBOX -->|"AMQP"| BUS
    REDISAD -->|"rate limit / lock"| REDIS[("Redis coord")]
    EMAILAD -->|"users.api → correo"| FACADES
```

### 2.1 Anatomía hexagonal de un módulo

```mermaid
flowchart LR
    subgraph MODULE["Módulo &lt;modulo&gt;"]
        direction LR

        subgraph ADAPTERS_IN["Entrada"]
            HTTP["Controller REST"]
            APIADAPTER["Fachada .api<br/>(para otros módulos)"]
            EVL["EventListener"]
        end

        subgraph APP["Aplicación"]
            INPORT["Puerto de entrada<br/>input port"]
            SERVICE["Servicio de caso de uso<br/>@Transactional"]
            OUTPORT["Puerto de salida<br/>output port"]
        end

        subgraph DOM["Dominio"]
            AGG["Agregado / entidad"]
            POLICY["Regla de dominio"]
        end

        subgraph ADAPTERS_OUT["Salida"]
            JPA["Persistence adapter"]
            EXT["Adapter externo<br/>Redis · email · bus"]
        end
    end

    HTTP --> INPORT
    APIADAPTER --> INPORT
    EVL --> INPORT
    INPORT --> SERVICE
    SERVICE --> AGG
    SERVICE --> POLICY
    SERVICE --> OUTPORT
    OUTPORT --> JPA
    OUTPORT --> EXT
```

**Regla de dependencia**

```text
infrastructure → application → domain
                     ↓
              puertos (interfaces)
```

La infraestructura implementa puertos; el dominio no conoce Spring, JPA ni HTTP.

---

## 3. Grafo de módulos (acíclico)

Las flechas significan “importa la API pública de”. No hay ciclos.

```mermaid
flowchart TB
    subgraph SOURCES["Consumidores / composición"]
        ADMIN["admin<br/>backoffice"]
        BFF["bff<br/>composición de lectura"]
        REPORTS["reports<br/>moderación"]
        STATS["statistics<br/>métricas de plataforma"]
    end

    subgraph CORE["Núcleo de negocio"]
        USERS["users<br/>identity"]
        REQUESTS["requests<br/>workflow"]
        FEEDBACK["feedback"]
    end

    subgraph LEAVES["Proveedores hoja"]
        PROFILES["profiles"]
        SERVICES["services"]
        METRICS["metrics"]
        NOTI["notifications"]
    end

    APIS["APIs públicas de todos los módulos<br/>(admin/reports/statistics consumen este conjunto)"]

    ADMIN -.-> APIS
    REPORTS -.-> APIS
    STATS -.-> APIS

    BFF --> USERS
    BFF --> REQUESTS
    BFF --> FEEDBACK
    BFF --> PROFILES
    BFF --> SERVICES
    BFF --> METRICS

    USERS --> REQUESTS
    USERS --> PROFILES
    USERS --> SERVICES
    USERS --> NOTI

    REQUESTS --> PROFILES
    REQUESTS --> SERVICES
    REQUESTS --> NOTI

    FEEDBACK --> REQUESTS

    classDef leaf fill:#d9f2d9,stroke:#2e7d32,color:#1b5e20;
    classDef composition fill:#e3f2fd,stroke:#1565c0,color:#0d47a1;
    classDef kernel fill:#fff3e0,stroke:#ef6c00,color:#e65100;
    class PROFILES,SERVICES,METRICS,NOTI leaf;
    class BFF,ADMIN,REPORTS,STATS composition;
    class APIS kernel;
```

**Módulos hoja:** cualquier dependencia nueva desde `profiles`, `services`, `metrics` o
`notifications` hacia otro módulo de negocio rompe `ModuleBoundaryTest`.

### 3.1 Matriz resumida de dependencias

| Módulo | Puede depender de | Rol |
|---|---|---|
| `users` | `requests`, `profiles`, `services`, `notifications` | identity/auth |
| `requests` | `profiles`, `services`, `notifications` | workflow de solicitudes |
| `feedback` | `requests` | reseñas |
| `profiles` | — (solo `shared`) | perfiles/direcciones |
| `services` | — (solo `shared`) | catálogo propio |
| `metrics` | — (solo `shared`) | read-model por eventos |
| `notifications` | — (solo `shared`) | outbox; el correo lo resuelve `config` |
| `bff` | APIs de users/requests/profiles/services/metrics/feedback | composición HTTP |
| `admin` | APIs de todos los módulos | backoffice |
| `reports` | APIs de todos los módulos | moderación |
| `statistics` | users/requests/feedback/services | métricas de plataforma |

---

## 4. Mapa de composición del BFF

El BFF no tiene dominio propio: solo compone lecturas con APIs públicas y mantiene las URLs/JSON
históricos.

```mermaid
flowchart LR
    subgraph BFF["Módulo bff"]
        POC["PublicOffererController"]
        POS["PublicOffererQueryService"]
        SCC["ServiceCatalogController"]
        SCS["ServiceCatalogQueryService"]
        MRC["MetricsReadController"]
        MRS["MetricsReadQueryService"]
        SAT["ServiceAvailabilityTemplateController"]
    end

    subgraph APIS["APIs públicas consumidas"]
        PA["profiles.api"]
        SA["services.api"]
        MA["metrics.api"]
        FA["feedback.api"]
        RA["requests.api"]
    end

    POC --> POS
    SCC --> SCS
    MRC --> MRS
    SAT --> PA
    SAT --> SA

    POS --> PA
    POS --> MA
    POS --> SA

    SCS --> SA
    SCS --> PA
    SCS --> MA
    SCS --> FA

    MRS --> MA
    MRS --> FA
    MRS --> RA
```

| Endpoint compuesto | Servicio BFF | APIs |
|---|---|---|
| `GET /offerers/{id}/public-profile` | `PublicOffererQueryService` | profiles, metrics, services |
| `GET /offerers/{id}/summary` | `PublicOffererQueryService` | profiles, metrics |
| `GET /services/search` | `ServiceCatalogQueryService` | services, profiles, metrics |
| `GET /services/{id}/detail` | `ServiceCatalogQueryService` | services, profiles, metrics, feedback |
| `GET /offerers/{offererId}/services` | `ServiceCatalogQueryService` | services |
| `POST /service-availabilities/service/{id}/apply-template` | `ServiceAvailabilityTemplateController` | profiles, services |
| `GET /offerers/{id}/tag-metrics` | `MetricsReadQueryService` | metrics, feedback |
| `GET /clients/{id}/tag-metrics` | `MetricsReadQueryService` | metrics, feedback |
| `GET /clients/{id}/metrics` | `MetricsReadQueryService` | metrics, requests |
| `GET /users/me/metrics` | `MetricsReadQueryService` | metrics, requests |

---

## 5. Ciclo de vida de una petición HTTP (escritura)

Ejemplo genérico: `POST /api/v1/...` con JWT.

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant GW as API Gateway
    participant SEC as Seguridad<br/>JWT + rate limit
    participant CTRL as Controller REST
    participant APP as Servicio de aplicación
    participant DOM as Dominio
    participant PORT as Puerto de salida
    participant DB as MySQL

    FE->>GW: HTTP/JSON + Bearer JWT
    GW->>SEC: enruta al monolito
    SEC->>SEC: valida JWT, roles y cuota (Redis)
    alt token/cuota inválidos
        SEC-->>FE: 401 / 403 / 429
    else válido
        SEC->>CTRL: invoca el handler
        CTRL->>CTRL: Form → Command/Query
        CTRL->>APP: caso de uso
        APP->>DOM: valida reglas y muta el agregado
        APP->>PORT: persiste / publica
        PORT->>DB: INSERT/UPDATE (JPA)
        APP-->>CTRL: Result / dominio
        CTRL-->>GW: Response JSON
        GW-->>FE: 2xx / error de negocio
    end
```

**Conectores de esta ruta**

- `FE → GW`: HTTP/JSON.
- `GW → backend`: HTTP (Spring Cloud Gateway).
- `SecurityConfig` + `JwtAuthenticationFilter`: autenticación/autorización.
- `RateLimitingFilter` + `RedisRateLimiter`: cuota por IP/email.
- `Controller → Application service`: llamada Java in-process.
- `Application service → Persistence port → JPA adapter`: puerto/adaptador.
- `Application service → domain`: reglas de negocio.

---

## 6. Conectores de eventos: in-process vs outbox

### 6.1 Eventos de dominio internos

```mermaid
sequenceDiagram
    autonumber
    participant CMD as Servicio de comando<br/>(requests/feedback)
    participant PUB as ApplicationEventPublisher
    participant M as MetricsEventListener
    participant P as ProfileEventListener

    CMD->>PUB: publica evento de dominio
    Note over PUB: dentro de la transacción del comando
    PUB-->>M: @TransactionalEventListener(AFTER_COMMIT)
    PUB-->>P: @TransactionalEventListener(AFTER_COMMIT)
    M->>M: actualiza read-model de métricas
    P->>P: reacciona al evento de rol
```

Estos eventos **no salen del monolito**; son el mecanismo de desacoplamiento entre módulos para
efectos secundarios (métricas, inicialización de perfiles).

### 6.2 Outbox + RabbitMQ hacia notifications-service

```mermaid
sequenceDiagram
    autonumber
    participant UC as Caso de uso del monolito
    participant NAPI as NotificationCommandApi
    participant NS as NotificationService
    participant OUT as notification_outbox<br/>(MySQL monolito)
    participant RELAY as Outbox relay<br/>@Scheduled
    participant BUS as RabbitMQ
    participant CONSUMER as notifications-service<br/>NotificationRequestedListener
    participant NDB as MySQL notificaciones
    participant EMAIL as EmailApiAdapter

    UC->>NAPI: notify(NotificationCommand)
    NAPI->>NS: traduce canales y delega
    NS->>NS: resuelve correo si hay EMAIL<br/>(config.integration → users.api)
    NS->>OUT: INSERT en la misma transacción
    Note over OUT: outbox transaccional<br/>si la transacción falla, no hay mensaje
    RELAY->>OUT: claim batch
    RELAY->>BUS: publica notification-requested
    BUS-->>CONSUMER: entrega el mensaje
    CONSUMER->>NDB: persiste notification + deliveries
    CONSUMER->>EMAIL: envía EMAIL si está habilitado
    Note over CONSUMER,EMAIL: si Brevo falla o EMAIL_ENABLED=false la entrega EMAIL queda FAILED y la INTERNAL se mantiene
```

**Propiedades**

- El outbox está en la base del monolito; el relay lo publica de forma asíncrona.
- RabbitMQ tiene una cola principal y una DLQ.
- `notifications-service` es idempotente por `eventId`.
- Si RabbitMQ está caído, la operación de negocio sigue; el outbox reintenta.
- Si Redis está caído, `notifications-service` difiere correos (modo degradado) y el monolito deja
  pasar o rechaza según la política del rate limiter.

---

## 7. Mapa de datos y propiedad

```mermaid
flowchart TB
    subgraph MONO_DB["MySQL del monolito"]
        ID["users · roles · user_roles<br/>consents · password_reset_tokens"]
        PROF["user_profiles · offerer_profiles<br/>addresses · offerer_availabilities"]
        CAT["services · categories<br/>service_availabilities"]
        REQ["service_requests<br/>reschedule_proposals"]
        FB["service_feedback · client_feedback<br/>tag catalogs"]
        MET["service_metrics · offerer_metrics<br/>client_metrics · tag_metrics"]
        REP["reports · report_actions<br/>request/service/client feedback reports"]
        OBOX["notification_outbox"]
    end

    subgraph NOTI_DB["MySQL de notificaciones"]
        NOTI_T["notifications<br/>notification_deliveries<br/>notification_channels"]
    end

    subgraph REDIS_DB["Redis coord"]
        RL["rate-limit keys / locks"]
    end

    subgraph BUS["RabbitMQ"]
        Q["notification-requested + DLQ"]
    end

    ID -->|"identity"| MONO_DB
    PROF -->|"profiles"| MONO_DB
    CAT -->|"services"| MONO_DB
    REQ -->|"requests"| MONO_DB
    FB -->|"feedback"| MONO_DB
    MET -->|"metrics"| MONO_DB
    REP -->|"reports"| MONO_DB
    OBOX -->|"outbox"| MONO_DB

    MONO_DB -->|"outbox relay"| Q
    Q -->|"consume"| NOTI_T
    MONO_DB -.->|"rate limit"| RL
```

**Regla de oro:** cada módulo es dueño de sus tablas. Ningún módulo accede a la persistencia de
otro; los datos cruzan por APIs o eventos. `notifications-service` tiene su propia base.

---

## 8. Catálogo de conectores

| Conector | Origen → destino | Tecnología | Tipo |
|---|---|---|---|
| Entrada web | Frontend → API Gateway | HTTP/JSON | síncrono |
| Enrutado | API Gateway → monolito / notifications-service | Spring Cloud Gateway | síncrono |
| Seguridad | Gateway → filtros JWT del servicio | JWT Bearer | síncrono |
| Rate limiting | Monolito → Redis | Redis Lua + circuit breaker | síncrono/degradable |
| API entre módulos | Módulo A → `B.api` / fachada | llamada Java in-process | síncrono |
| Composición BFF | BFF → APIs públicas | llamada Java in-process | síncrono |
| Persistencia | Aplicación → puerto → adapter JPA | Spring Data JPA/JDBC | síncrono |
| Evento de dominio | Aplicación → listeners | `ApplicationEventPublisher` | in-process, after-commit |
| Outbox | Aplicación → `notification_outbox` | JPA/MySQL | misma transacción |
| Relay | Relay programado → RabbitMQ | AMQP | asíncrono |
| Consumo | RabbitMQ → notifications-service | AMQP | asíncrono |
| Correo | notifications-service → Brevo | HTTPS API | síncrono externo |
| Coordinación | notifications-service → Redis | PING ACL | síncrono/degradable |
| Datos | Monolito → MySQL | JDBC | síncrono |
| Datos | notifications-service → MySQL propio | JDBC | síncrono |

---

## 9. Inventario de módulos

| Módulo | Responsabilidad principal | Entrada principal | API pública |
|---|---|---|---|
| `users` | cuentas, autenticación, roles, consentimientos, reset | `AuthController`, `AccountController` | `UserQueryApi`, `UserCommandApi`, `UserContactApi` |
| `profiles` | perfiles, direcciones, disponibilidad de oferente | `UserProfileController`, `AddressController`, `OffererProfileController`, `OffererAvailabilityController` | `ProfileQueryApi`, `ProfileCommandApi` |
| `services` | catálogo de servicios y categorías | `ServiceController`, `CategoryController`, `ServiceAvailabilityController` | `ServiceQueryApi`, `ServiceCommandApi` |
| `requests` | solicitudes, estados, reprogramaciones, historial | `ServiceRequestController`, `RescheduleProposalController` | `RequestQueryApi`, `RequestCommandApi`, `RequestLookupApi`, `RequestCountApi` |
| `feedback` | feedback de servicio y de cliente, catálogo de tags | `ServiceFeedbackController`, `ClientFeedbackController` | `FeedbackQueryApi`, `FeedbackCommandApi` |
| `metrics` | read-model de métricas por eventos | `MetricsController`, listeners | `MetricsQueryApi` |
| `notifications` | outbox, bandeja y entregas | `NotificationCommandApi`, relay | `NotificationCommandApi` |
| `reports` | reportes, acciones de moderación | `ReportController` | `ReportQueryApi`, `ReportCommandApi` |
| `admin` | backoffice: usuarios, roles, moderación | `AdminController`, `ModerationController` | — (consumidor) |
| `statistics` | métricas agregadas de plataforma | `StatisticsController` | — (consumidor) |
| `bff` | composición de lecturas públicas/admin | controllers del BFF | — (consumidor) |
| `shared` | kernel: contratos, eventos, seguridad, rate limit, text filter | — | `shared.contract`, `shared.events` |
| `config` | raíz de composición e integración | — | — |

---

## 10. Lectura recomendada

1. **Vista de contenedores** para entender qué se despliega.
2. **Componentes y conectores del monolito** para ver las capas y los adaptadores.
3. **Grafo de módulos** para entender la dirección de dependencias y los módulos hoja.
4. **Mapa del BFF** para ver dónde se componen las lecturas cross-module.
5. **Secuencias** para seguir una petición y un evento de notificación.
6. **Mapa de datos** para ubicar la propiedad de cada tabla.

> Si estos diagramas y el código discrepan, el código y `ModuleBoundaryTest` son la fuente de
> verdad; actualiza los diagramas en el mismo commit que el cambio arquitectónico.
