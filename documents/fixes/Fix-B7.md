# Fix B7: Lost Update in Metrics Due to Missing Optimistic Locking

## Bug Description
**Bug ID:** B7
**Severity:** ALTO
**Module:** metrics

**Root Cause:** The metrics entities (`OffererMetricsEntity`, `ClientMetricsEntity`, `ServiceMetricsEntity`, `OffererTagMetricsEntity`, `ClientTagMetricsEntity`, `ServiceTagMetricsEntity`) lacked JPA `@Version` field for optimistic locking. This caused lost updates when concurrent events (e.g., multiple feedback submissions, request status changes) modified the same metrics row simultaneously.

The services used a **read-modify-write** pattern:
1. Read the metrics entity
2. Modify counters (e.g., `registerRating()`, `incrementAccepted()`)
3. Save the entity

Without optimistic locking, concurrent transactions would overwrite each other's changes, causing:
- Incorrect average ratings
- Lost counter increments
- Data inconsistency under load

## Affected Files

### Java Entities (6 files modified)
1. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/OffererMetricsEntity.java`
2. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ClientMetricsEntity.java`
3. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ServiceMetricsEntity.java`
4. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/OffererTagMetricsEntity.java`
5. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ClientTagMetricsEntity.java`
6. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ServiceTagMetricsEntity.java`

### Database Schema (1 file modified)
1. `mysql/01_create_database.sql` - Added `version BIGINT UNSIGNED NOT NULL DEFAULT 0` column to:
   - `offerer_metrics`
   - `client_metrics`
   - `service_metrics`
   - `service_tag_metrics`
   - `offerer_tag_metrics`
   - `client_tag_metrics`

## Fix Implementation

### Code Changes
Added `@Version` annotation to all metrics JPA entities:

```java
@Version
private Long version;
```

This enables JPA/Hibernate optimistic locking:
- On entity read, the version value is loaded
- On entity update, Hibernate includes `WHERE version = ?` in the UPDATE statement
- If version mismatch occurs (another transaction modified the row), `OptimisticLockException` is thrown
- The `@Transactional(propagation = Propagation.REQUIRES_NEW)` on service methods will cause the transaction to rollback and retry (if configured) or fail fast

### Database Changes
Added `version` column with default value 0 to all metrics tables. This ensures:
- Existing rows get version = 0
- New rows start at version = 0
- Each successful update increments the version

## Testing
- All 344 existing unit tests pass
- The fix introduces no behavioral changes to the domain logic
- Under concurrent load, `OptimisticLockException` will be thrown instead of silent data corruption
- Applications should handle this exception appropriately (retry logic or user feedback)

## Related Issues
This fix addresses part of **M2** (Bloqueo optimista + contadores atómicos) from the improvement backlog. It resolves:
- B7 (this bug): Lost-update in metrics
- B10: Missing @Version in all entities (metrics module complete)
- Contributes to fixing B8: Propuestas PENDING duplicadas (same pattern in requests module)

## Migration Notes
- Run the updated `mysql/01_create_database.sql` on clean databases
- For existing databases, add the column manually:
  ```sql
  ALTER TABLE offerer_metrics ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0;
  ALTER TABLE client_metrics ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0;
  ALTER TABLE service_metrics ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0;
  ALTER TABLE service_tag_metrics ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0;
  ALTER TABLE offerer_tag_metrics ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0;
  ALTER TABLE client_tag_metrics ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0;
  ```
- The application will automatically start using optimistic locking after deployment