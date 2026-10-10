# Catálogo de claves de Redis — ServiYa

> Registro **obligatorio** de TODA clave **antes** de usarse en código (regla §3.1 de la guía).

**Reglas (§3.1):** separador `:`, kebab-case, minúsculas; **hash tag `{...}`** sobre la entidad
principal (para que claves relacionadas caigan en el mismo slot de Cluster); sin datos personales en
claro (hash SHA-256 truncado si el sujeto es PII/IP); **toda clave con TTL** salvo excepción documentada
aquí; sin `SELECT` (todo en base 0); sin prefijo de entorno (cada entorno tiene sus propias instancias);
máx. ~200 caracteres. La versión `v<N>` sube cuando cambia el **formato** del valor almacenado.

## Claves en uso

| Patrón de clave | Instancia | Tipo Redis | TTL | Dueño | Escrita por | Leída por | Propósito | Versión |
|---|---|---|---|---|---|---|---|---|
| _(ninguna todavía)_ | | | | | | | | |

## Previsto — Fase 2 (locks de los `@Scheduled`, deuda #1)

> Exclusión mutua de las 6 tareas programadas para correr en ≥2 instancias sin doble ejecución.
> **Sin `fence:`** (tareas idempotentes de mantenimiento — ver ADR-003). El TTL es la red de seguridad
> si la instancia muere a mitad de tarea; confirmar los valores al medir la duración real en Fase 2.

| Patrón de clave | Instancia | Tipo | TTL previsto | Tarea (clase) |
|---|---|---|---|---|
| `lock:{sched:reject-expired-pending}` | coord | string | ~10 min | `RequestMaintenanceScheduler` — rechazar PENDING vencidas |
| `lock:{sched:mark-stale-accepted}` | coord | string | ~10 min | `RequestMaintenanceScheduler` — marcar ACCEPTED estancadas |
| `lock:{sched:reject-expired-proposals}` | coord | string | ~10 min | `RequestMaintenanceScheduler` — rechazar propuestas vencidas |
| `lock:{sched:finalize-unconfirmed}` | coord | string | ~10 min | `RequestMaintenanceScheduler` — finalizar sin confirmar |
| `lock:{sched:notif-retry-failed}` | coord | string | ~10 min | `NotificationRetryScheduler` — reintento de entregas fallidas |
| `lock:{sched:reset-token-cleanup}` | coord | string | ~5 min | `PasswordResetCleanupScheduler` — borrar tokens caducados |

> Nota: el "recurso" de estos locks es la **tarea** (singleton), no una entidad con id; por eso el hash
> tag envuelve `sched:<nombre-tarea>`. Sin fencing token asociado.
