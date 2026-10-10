# redis-scripts — scripts Lua compartidos

**Único origen** de los algoritmos atómicos para todos los lenguajes (regla §3.4 de la guía).

- Nombre: `<dominio>_<accion>.lua` (p. ej. `lock_release.lua`).
- Encabezado obligatorio en cada script: `name`, `version`, `keys`, `args`, `returns`, `cluster-safe`.
- Todas las claves se pasan en `KEYS` (nunca se construyen dentro del script) y comparten **hash tag**.
- Un cambio incompatible crea un script nuevo (`version` + 1), nunca se modifica en caliente.
- Ejecución con `EVALSHA` (en Spring: `DefaultRedisScript`, que maneja `SCRIPT LOAD`/`NOSCRIPT`).

## Scripts previstos

- `lock_acquire.lua`, `lock_release.lua`, `lock_renew.lua` — **Fase 2** (deuda #1, lock de `@Scheduled`).
- `ratelimit_token_bucket.lua` — **Fase 3** (rate limiting distribuido, #6 / M13).
