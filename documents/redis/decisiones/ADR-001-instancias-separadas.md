# ADR-001: Dos instancias Redis separadas (coord / cache)

- **Estado:** Aceptado
- **Fecha:** 2026-10-10
- **Decisión relacionada:** D-01

## Contexto

Redis se usará para coordinación (locks, luego rate limit) y para caché. La **política de expulsión
de memoria es global por instancia**: los locks **nunca** deben expulsarse (perder un lock es un bug
de corrección), mientras que la caché **sí** debe poder expulsarse bajo presión de memoria. Además las
necesidades de persistencia son opuestas (los locks quieren durabilidad; la caché es descartable).

## Opciones consideradas

1. **Una sola instancia compartida** — pros: menos infra. Contras: una única `maxmemory-policy` no
   sirve a ambos usos; una expulsión por presión de caché podría tirar locks; mezcla durabilidades.
2. **Dos instancias por propósito** — pros: aísla políticas de memoria y persistencia, escalan por
   separado. Contras: dos contenedores a operar.

## Decisión

Dos instancias: `redis-coord` (`noeviction` + AOF/RDB) y `redis-cache` (`allkeys-lfu`, sin
persistencia). En la adopción **Vía B** solo se despliega `redis-coord` ahora; `redis-cache` llega en
la Fase 6. El **contrato de configuración de ambas** (`app.redis.coord.*` / `app.redis.cache.*`) existe
desde la Fase 1 para no cambiarlo después.

## Consecuencias

Aislamiento correcto de políticas desde el día 1. El coste del segundo contenedor se mitiga difiriendo
`redis-cache` a cuando se use. **Revisar** si se migra a un servicio gestionado que fuerce una sola
instancia o modo Cluster (D-16/D-17, Fase 8).
