# Catálogo de claves de Redis — ServiYa

> Registro **obligatorio** de TODA clave **antes** de usarse en código (regla §3.1 de la guía).

**Reglas (§3.1):** separador `:`, kebab-case, minúsculas; **hash tag `{...}`** sobre la entidad
principal (para que claves relacionadas caigan en el mismo slot de Cluster); sin datos personales en
claro (hash SHA-256 truncado si el sujeto es PII/IP); **toda clave con TTL** salvo excepción documentada
aquí; sin `SELECT` (todo en base 0); sin prefijo de entorno (cada entorno tiene sus propias instancias);
máx. ~200 caracteres. La versión `v<N>` sube cuando cambia el **formato** del valor almacenado.

## Claves en uso

> Locks de los 6 `@Scheduled` (deuda #1, **Fase 2** — implementados). Exclusión mutua para correr en ≥2
> instancias sin doble ejecución. Valor = token UUID de la adquisición (string). **Sin `fence:`**: tareas
> idempotentes de mantenimiento (ADR-003). El TTL es la red de seguridad si la instancia muere a mitad;
> holgado frente a la duración real (revisar al medir p99 en Fase 5). Escritas/leídas por
> `RedisDistributedLock` (`lock_acquire` / `lock_release` / `lock_renew`).

| Patrón de clave | Instancia | Tipo | TTL | Tarea (clase) | Versión |
|---|---|---|---|---|---|
| `lock:{sched:reject-expired-pending}` | coord | string | 10 min | `RequestMaintenanceScheduler` — rechazar PENDING vencidas | v1 |
| `lock:{sched:mark-stale-accepted}` | coord | string | 10 min | `RequestMaintenanceScheduler` — marcar ACCEPTED estancadas | v1 |
| `lock:{sched:reject-expired-proposals}` | coord | string | 10 min | `RequestMaintenanceScheduler` — rechazar propuestas vencidas | v1 |
| `lock:{sched:finalize-unconfirmed}` | coord | string | 10 min | `RequestMaintenanceScheduler` — finalizar sin confirmar | v1 |
| `lock:{sched:notif-retry-failed}` | coord | string | 10 min | `NotificationRetryScheduler` — reintento de entregas fallidas | v1 |
| `lock:{sched:reset-token-cleanup}` | coord | string | 5 min | `PasswordResetCleanupScheduler` — borrar tokens caducados | v1 |

> Nota: el "recurso" de estos locks es la **tarea** (singleton), no una entidad con id; por eso el hash
> tag envuelve `sched:<nombre-tarea>`. Sin fencing token asociado.

## Previsto — `fence:{<entidad>}` (fencing, deuda B8)

> El fencing (`lock:{...}` + `fence:{...}` vía `lock_acquire_fenced.lua`) está disponible en la API pero
> **aún sin uso**. Primer caso previsto: carrera de propuestas PENDING duplicadas (deuda **B8**), que
> además requerirá columna `version`/`last_fencing_token` en la tabla protegida.
