# Fix B10: Missing `@Version` for Optimistic Locking on All Entities

## Bug Description
**Bug ID:** B10  
**Severity:** MEDIO  
**Module:** Global  
**Description:** Sin `@Version` en ninguna entidad (consistencia)

## Root Cause Analysis

The bug was identified in the `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` document as a medium-severity global issue. The root cause is that **none of the JPA entities in the application had the `@Version` annotation** for optimistic locking.

### Affected Components
All 25 JPA entities across 9 modules were missing the `@Version` field:
- **users module** (5 entities): UserEntity, RoleEntity, PasswordResetTokenEntity, ConsentEntity, UserRoleEntity
- **services module** (3 entities): ServiceEntity, ServiceAvailabilityEntity, CategoryEntity
- **requests module** (2 entities): ServiceRequestEntity, RescheduleProposalEntity
- **metrics module** (6 entities): OffererMetricsEntity, ClientMetricsEntity, ServiceMetricsEntity, OffererTagMetricsEntity, ClientTagMetricsEntity, ServiceTagMetricsEntity
- **notifications module** (3 entities): NotificationEntity, NotificationDeliveryEntity, NotificationChannelEntity
- **profiles module** (4 entities): AddressEntity, OffererAvailabilityEntity, OffererProfileEntity, UserProfileEntity
- **reports module** (5 entities): ReportEntity, ReportActionEntity, ServiceFeedbackReportEntity, ClientFeedbackReportEntity, RequestReportEntity
- **feedback module** (6 entities): ServiceFeedbackEntity, ClientFeedbackEntity, ServiceFeedbackTagEntity, ClientFeedbackTagEntity, ServiceFeedbackTagCatalogEntity, ClientFeedbackTagCatalogEntity

**Note:** `RescheduleProposalEntity` already had `@Version` (using `Long` type).

### Impact
Without optimistic locking (`@Version`), the application is vulnerable to **lost-update anomalies** under concurrent access:

1. **Metrics Module (Critical)**: The `OffererMetrics.registerRating()` method performs read-modify-write operations on `averageRating` and `totalRatings`. Under concurrent requests, updates can be lost, leading to incorrect rating calculations.

2. **Requests Module**: State transitions in `ServiceRequest` (accept, reject, cancel, complete, etc.) could suffer from lost updates when multiple users attempt to modify the same request simultaneously.

3. **All Modules**: Any entity that can be updated concurrently by multiple transactions is at risk of data inconsistency.

## Solution Implemented

Added `@Version` field to all 25 JPA entities (excluding `RescheduleProposalEntity` which already had it).

### Changes Made

For each entity, added:
```java
@Version
private Integer version;
```

Placed immediately after the `@Id` field. Used `Integer` type for consistency across all entities (except `RescheduleProposalEntity` which uses `Long`).

### Files Modified

1. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/entities/UserEntity.java`
2. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/entities/RoleEntity.java`
3. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/entities/PasswordResetTokenEntity.java`
4. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/entities/ConsentEntity.java`
5. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/entities/UserRoleEntity.java`
6. `backend/serviya/src/main/java/com/parosurvivors/serviya/services/infrastructure/entities/ServiceEntity.java`
7. `backend/serviya/src/main/java/com/parosurvivors/serviya/services/infrastructure/entities/ServiceAvailabilityEntity.java` (added missing import)
8. `backend/serviya/src/main/java/com/parosurvivors/serviya/services/infrastructure/entities/CategoryEntity.java`
9. `backend/serviya/src/main/java/com/parosurvivors/serviya/requests/infrastructure/entities/ServiceRequestEntity.java`
10. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/OffererMetricsEntity.java`
11. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ClientMetricsEntity.java`
12. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ServiceMetricsEntity.java`
13. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/OffererTagMetricsEntity.java`
14. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ClientTagMetricsEntity.java`
15. `backend/serviya/src/main/java/com/parosurvivors/serviya/metrics/infrastructure/entities/ServiceTagMetricsEntity.java`
16. `backend/serviya/src/main/java/com/parosurvivors/serviya/notifications/infrastructure/entities/NotificationEntity.java`
17. `backend/serviya/src/main/java/com/parosurvivors/serviya/notifications/infrastructure/entities/NotificationDeliveryEntity.java`
18. `backend/serviya/src/main/java/com/parosurvivors/serviya/notifications/infrastructure/entities/NotificationChannelEntity.java`
19. `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/entities/AddressEntity.java`
20. `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/entities/OffererAvailabilityEntity.java`
21. `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/entities/OffererProfileEntity.java`
22. `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/entities/UserProfileEntity.java`
23. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/entities/ReportEntity.java`
24. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/entities/ReportActionEntity.java`
25. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/entities/ServiceFeedbackReportEntity.java`
26. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/entities/ClientFeedbackReportEntity.java`
27. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/entities/RequestReportEntity.java`
28. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/entities/ServiceFeedbackEntity.java`
29. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/entities/ClientFeedbackEntity.java`
30. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/entities/ServiceFeedbackTagEntity.java`
31. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/entities/ClientFeedbackTagEntity.java`
32. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/entities/ServiceFeedbackTagCatalogEntity.java`
33. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/entities/ClientFeedbackTagCatalogEntity.java`

## Verification

### Build Compilation
```bash
cd backend/serviya && ./mvnw clean compile
```
**Result:** ✅ BUILD SUCCESS

### Test Execution
```bash
cd backend/serviya && ./mvnw test
```
**Result:** Unit tests pass (341/344). The 3 failures are pre-existing issues unrelated to this fix:
- `ServiyaApplicationTests.contextLoads` - Missing database connection (requires Testcontainers/MySQL)
- `JwtServiceTest.failsFastWhenSecretBlank` - Pre-existing test expecting fail-fast behavior
- `JwtServiceTest.failsFastWhenSecretMissing` - Pre-existing test expecting fail-fast behavior

### Database Migration
When the application starts with `ddl-auto: update`, Hibernate will automatically add the `version` column to all tables. For production, a Flyway migration should be created (see M25 in the plan).

## Related Issues
This fix addresses part of **M2 (Bloqueo optimista `@Version` + contadores atómicos)** from the improvement plan, which resolves:
- **B7** (Lost-update en métricas por read-modify-write sin `@Version`/SQL atómico)
- **B8** (Propuestas PENDING duplicadas por carrera)
- **B10** (Sin `@Version` en ninguna entidad)

## Future Considerations

1. **Atomic Counters for Metrics**: For the metrics module, consider replacing read-modify-write patterns with atomic SQL updates (e.g., `UPDATE offerer_metrics SET total_ratings = total_ratings + 1, average_rating = ... WHERE id = ? AND version = ?`) for better performance under high concurrency.

2. **Flyway Migration**: Create a Flyway migration script to add the `version` column with default value 0 to all existing tables for production deployments.

3. **Lock Timeout Configuration**: Consider configuring `@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)` or setting lock timeout for high-contention scenarios.

## References
- [ESTADO-ACTUAL-Y-PLAN-FUTURO.md](../ESTADO-ACTUAL-Y-PLAN-FUTURO.md) - Bug catalog entry B10
- [JPA Optimistic Locking Documentation](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta/persistence/Version)
- Spring Data JPA Reference: Optimistic Locking