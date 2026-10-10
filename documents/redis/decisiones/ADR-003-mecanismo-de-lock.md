# ADR-003: Mecanismo de lock distribuido

- **Estado:** Aceptado
- **Fecha:** 2026-10-10
- **Decisión relacionada:** D-04 (y D-05 fencing, D-08 fallo)

## Contexto

Primer uso: dar **exclusión mutua** a las 6 tareas `@Scheduled` del backend (requests ×4, reintento de
notificaciones, limpieza de reset-tokens) para poder desplegar el monolito en **≥2 instancias** sin
doble ejecución (deuda #1 de `ESTADO-ACTUAL-Y-PLAN-FUTURO.md`). Horizonte: migración a microservicios y
**posibles servicios en otros lenguajes**, por lo que el locking debería ser interoperable y reutilizable.

## Opciones consideradas

1. **Redisson** (`RLock` con watchdog) — pros: completo, watchdog de renovación. Contras: **solo Java**,
   no interoperable; formato de claves/pubsub propietario.
2. **Protocolo propio** (`SET NX PX` + Lua) detrás de la interfaz `DistributedLock` — pros: un **único**
   sistema coherente, interoperable (Lua compartido), encaja con el estilo hexagonal del repo, reutilizable
   para otras secciones críticas. Contras: algo más de código que una librería.
3. **ShedLock** (proveedor Redis, solo para `@Scheduled`) — pros: rapidísimo de añadir. Contras: usa su
   **propio esquema de claves opaco** (no `lock:{...}`), **no** pasa por la interfaz `DistributedLock`, no
   usa los scripts Lua ni la convención de ACL `~lock:*`; sería un **segundo** sistema de lock en paralelo.

## Decisión

**Opción B (protocolo propio).** ShedLock (3) **descartado** por incoherencia con el resto de la guía
(interfaz única, convención de claves, Lua, interop). Redisson (1) descartado por no interoperable y por
redundar con B. El **fencing token** (D-05) estará disponible en la API `LockHandle`, pero **NO se usa en
los schedulers**: son tareas idempotentes de mantenimiento, sin escritura protegida por-entidad que
versionar — basta la exclusión mutua. Si Redis falla, el job **no se ejecuta** (D-08).

## Consecuencias

Un solo sistema de locking, coherente y reutilizable para la carrera de propuestas PENDING (deuda **B8**)
y para futuros locks (incluso multi-lenguaje). A cambio de algo más de código inicial que ShedLock.
**Revisar** si nunca aparecen ni multi-lenguaje ni nuevos locks: B seguiría siendo válido, solo con más
esfuerzo inicial relativo.
