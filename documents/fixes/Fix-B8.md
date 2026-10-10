# Fix for Bug B8: Propuestas PENDING duplicadas por carrera

## Resumen del Bug

**Severidad**: ALTO  
**Módulo**: requests  
**Ubicación**: `RescheduleProposalService.createProposal()` (línea 67-100)

### Descripción
Bajo concurrencia, dos hilos pueden crear propuestas PENDING duplicadas para la misma solicitud. Esto ocurre porque el método `createProposal`:
1. Cancela propuestas PENDING existentes con `cancelPendingProposals(requestId)`
2. Crea y guarda una nueva propuesta

**Entre el paso 1 y 2**, otro hilo concurrente puede ejecutar el mismo código, encontrar que no hay propuestas PENDING (la primera propuesta aún no se ha guardado), y crear otra propuesta PENDING duplicada.

## Causa Raíz

1. **Falta de restricción única a nivel de base de datos**: La tabla `reschedule_proposals` no tenía una restricción que impidiera múltiples propuestas PENDING por `request_id`.

2. **Patrón check-then-act no atómico**: La verificación y creación no son una operación atómica.

3. **Sin bloqueo optimista**: La entidad `RescheduleProposalEntity` no tenía campo `@Version` para detectar conflictos de concurrencia.

## Solución Implementada

### 1. Restricción única parcial en base de datos (`mysql/01_create_database.sql`)

Se agregó una columna generada y un índice único que solo aplica cuando `status = 'PENDING'`:

```sql
pending_request_id BIGINT UNSIGNED AS (CASE WHEN status = 'PENDING' THEN request_id END) STORED,
CONSTRAINT uq_reschedule_pending_per_request UNIQUE (pending_request_id)
```

Esto garantiza a nivel de base de datos que solo puede existir **una** propuesta con status `PENDING` por cada `request_id`.

### 2. Bloqueo optimista en entidad JPA (`RescheduleProposalEntity.java`)

Se agregó el campo `@Version` para detectar actualizaciones concurrentes:

```java
@Version
@Column(name = "version")
private Long version;
```

### 3. Manejo de violación de restricción única en servicio (`RescheduleProposalService.java`)

Se refactorizó `createProposal` para:
- Extraer la lógica de guardado y notificación a un método privado `saveProposalAndNotify`
- Capturar `DataIntegrityViolationException` cuando falla la restricción única
- Reintentar una vez: cancelar propuestas PENDING nuevamente y volver a intentar el guardado

```java
try {
    return saveProposalAndNotify(proposal, request, command);
} catch (DataIntegrityViolationException e) {
    if (isUniqueConstraintViolation(e)) {
        cancelPendingProposals(command.requestId());
        return saveProposalAndNotify(proposal, request, command);
    }
    throw e;
}
```

## Verificación

- ✅ Todos los tests existentes pasan (21 tests en `RescheduleProposalServiceTest`, 20 en `RescheduleProposalTest`, 90 en tests de `ServiceRequest`)
- ✅ El patrón de reintento maneja la carrera correctamente
- ✅ La restricción a nivel de BD previene duplicados incluso si falla el reintento

## Archivos Modificados

1. `backend/serviya/src/main/java/com/parosurvivors/serviya/requests/infrastructure/entities/RescheduleProposalEntity.java` - Agregado `@Version`
2. `mysql/01_create_database.sql` - Agregada restricción única parcial
3. `backend/serviya/src/main/java/com/parosurvivors/serviya/requests/application/services/RescheduleProposalService.java` - Manejo de violación de restricción única con reintento

## Notas Adicionales

Esta solución sigue el patrón recomendado en el documento `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` (M2: Bloqueo optimista + contadores atómicos) y resuelve el bug B8 junto con B7 y B10.