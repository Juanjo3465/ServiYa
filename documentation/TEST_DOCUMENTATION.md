# Test Documentation - ServiYa

**Last Updated:** 2026-10-10  
**Test Framework:** JUnit 5 (Jupiter) with Mockito, Spring Boot Test  
**Build Tool:** Maven (./mvnw)  
**Total Tests:** 365  
**Passed:** 362  
**Failed:** 3  
**Skipped:** 0

---

## Test Execution Summary

```bash
./mvnw test
```

**Results:**
- Tests run: 365
- Failures: 0
- Errors: 3
- Skipped: 0
- Time elapsed: ~17s

---

## Failed Tests (3)

### 1. `ServiyaApplicationTests.contextLoads`
- **Location:** `src/test/java/com/parosurvivors/serviya/ServiyaApplicationTests.java:19`
- **Type:** Integration test (SpringBootTest)
- **Purpose:** Verify Spring application context loads successfully with test configuration (H2 in-memory database)
- **Error:** `Cannot load driver class: org.h2.Driver` - H2 database dependency missing from test classpath
- **Fix:** Add H2 dependency to `pom.xml` test scope:
  ```xml
  <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
  </dependency>
  ```

### 2. `PiiAttributeConverterTest.encryptsAndDecryptsRoundTrip`
- **Location:** `src/test/java/com/parosurvivors/serviya/shared/security/PiiAttributeConverterTest.java:18`
- **Type:** Unit test
- **Purpose:** Verify PII encryption/decryption round-trip works correctly (RNF-005)
- **Error:** `ENCRYPTION_KEY no esta definida` - Missing ENCRYPTION_KEY environment variable
- **Fix:** Set ENCRYPTION_KEY in test environment or add `@SpringBootTest` with properties

### 3. `PiiAttributeConverterTest.usesRandomIv_soSameInputProducesDifferentCiphertext`
- **Location:** `src/test/java/com/parosurvivors/serviya/shared/security/PiiAttributeConverterTest.java:28`
- **Type:** Unit test
- **Purpose:** Verify encryption uses random IV so identical inputs produce different ciphertexts
- **Error:** Same as above - Missing ENCRYPTION_KEY

---

## Test Categories by Module

### 1. Services Module (`services`)

#### Domain Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `ServiceTest` | 8 | ✅ Pass | Domain entity behavior: activate/deactivate, soft delete, availability checks |
| `ServiceAvailabilityTest` | 3 | ✅ Pass | Domain entity: availability slot validation |
| `CategoryPersistenceAdapterTest` | 4 | ✅ Pass | Repository adapter: category CRUD operations |
| `ServicePersistenceAdapterTest` | 7 | ✅ Pass | Repository adapter: service CRUD, search, find by offerer |
| `ServiceAvailabilityPersistenceAdapterTest` | 6 | ✅ Pass | Repository adapter: availability slot CRUD |

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `MarketplaceServiceTest` | 31 | ✅ Pass | Service lifecycle: create, read, update, delete, softDelete, activate/deactivate, search, metrics, IDOR protection (B1) |
| `ServiceAvailabilityServiceTest` | 11 | ✅ Pass | Availability slot management: CRUD, validation, overlap detection |
| `ServicePhotoStorageServiceTest` | 8 | ✅ Pass | Photo upload/delete, storage limits, file validation |
| `MarketplaceCategoryTest` | 4 | ✅ Pass | Category hierarchy and validation |

---

### 2. Requests Module (`requests`)

#### Domain Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `ServiceRequestTest` | 43 | ✅ Pass | Request domain logic: state transitions (PENDING→ACCEPTED→IN_PROGRESS→COMPLETED/CANCELLED), expiration, rescheduling |
| `RescheduleProposalTest` | 20 | ✅ Pass | Reschedule proposal domain: creation, acceptance, rejection, expiration |

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `ServiceRequestCommandServiceTest` | 33 | ✅ Pass | Write operations: create, accept, reject, cancel, complete, reschedule, IDOR protection |
| `ServiceRequestQueryServiceTest` | 14 | ✅ Pass | Read operations: find by ID, by client, by offerer, filters, pagination |
| `RescheduleProposalServiceTest` | 21 | ✅ Pass | Reschedule workflow: propose, accept, reject, auto-expire |
| `RequestMaintenanceServiceTest` | 9 | ✅ Pass | Scheduled maintenance: expire old requests, cleanup |

---

### 3. Profiles Module (`profiles`)

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `OffererAvailabilityServiceTest` | 14 | ✅ Pass | Availability slot management: CRUD, validation, conflict detection |
| `OffererProfileServiceTest` | 1 | ✅ Pass | Offerer profile CRUD |
| `UserProfileServiceTest` | 4 | ✅ Pass | User profile management: update, avatar, preferences |

#### Integration Tests (Controller)
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `OffererAvailabilityControllerIntegrationTest` | 7 | ✅ Pass | **IDOR (B4)**: Verify controller passes JWT identity to service for ownership checks on delete/activate/deactivate slots |

---

### 4. Notifications Module (`notifications`)

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `NotificationDeliveryServiceTest` | 6 | ✅ Pass | Delivery retry logic, channel selection, failure handling |

#### Infrastructure Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `EmailApiAdapterTest` | 2 | ✅ Pass | Email sending: missing email handling, send failures |

---

### 5. Admin Module (`admin`)

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `AdminActionGuardTest` | 7 | ✅ Pass | Admin authorization: role checks, self-action prevention |
| `AdminServiceTest` | 5 | ✅ Pass | Admin operations: user ban/unban, delete, role management |
| `ModerationServiceTest` | 13 | ✅ Pass | Content moderation: report review, service suspension, user warnings |

---

### 6. Reports Module (`reports`)

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `ReportActionServiceTest` | 3 | ✅ Pass | Report actions: resolve, dismiss, escalate |
| `ReportServiceTest` | 9 | ✅ Pass | Report CRUD: create, list, filter by status/type |
| `RequestReportServiceTest` | 5 | ✅ Pass | Service request specific reports |

---

### 7. Users Module (`users`)

#### Application Service Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `UserAuthenticationServiceTest` | 15 | ✅ Pass | **HU-001, RF-003**: Login (bcrypt, banned/deleted checks), registration (role restriction), password reset flow (request, validate, confirm) |
| `UserCreationServiceTest` | 8 | ✅ Pass | User account creation: validation, role assignment, email uniqueness |
| `UserRoleServiceTest` | 4 | ✅ Pass | Role management: assignment, removal, validation |
| `UserServiceTest` | 5 | ✅ Pass | User CRUD and queries |
| `PasswordResetTokenServiceTest` | 12 | ✅ Pass | Token lifecycle: create, validate, consume, expiration, anti-enumeration |

#### Security Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `JwtAuthenticationFilterTest` | 1 | ✅ Pass | JWT filter chain integration |
| `JwtServiceTest` | 4 | ✅ Pass | Token generation, validation, parsing, expiration |
| `RateLimiterServiceTest` | 6 | ✅ Pass | Rate limiting: bucket consumption, refill, key resolution |

#### Integration Tests (Controller)
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `AccountControllerIntegrationTest` | 4 | ✅ Pass | Account endpoints: profile, password change, deletion |

---

### 8. Shared Module (`shared`)

#### Security Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `PiiAttributeConverterTest` | 3 | ❌ 2 Fail, 1 Pass | **RNF-005**: PII encryption (AES-GCM), round-trip, random IV, null handling |

#### Text Filter Tests
| Test Class | Tests | Status | Purpose |
|------------|-------|--------|---------|
| `ConfigurableWordFilterAdapterTest` | 4 | ✅ Pass | Profanity filtering: word matching, case insensitivity, replacement |

---

## Test Types Breakdown

| Type | Count | Description |
|------|-------|-------------|
| Unit Tests (Mockito) | ~320 | Fast, isolated tests with mocked dependencies |
| Integration Tests (@SpringBootTest/@WebMvcTest) | ~18 | Controller tests with Spring context |
| Domain Tests | ~71 | Pure domain logic without Spring |

---

## Security-Focused Tests

The test suite includes specific tests for security requirements:

| Requirement | Test Coverage |
|-------------|---------------|
| **IDOR (B1)** | `MarketplaceServiceTest` - 6 tests for ownership validation on service operations |
| **IDOR (B4)** | `OffererAvailabilityControllerIntegrationTest` - 7 tests for JWT identity propagation |
| **HU-001 (Login)** | `UserAuthenticationServiceTest` - 5 tests for bcrypt, banned/deleted rejection |
| **RF-003 (Password Reset)** | `UserAuthenticationServiceTest` - 10 tests for anti-enumeration, token validation, generic errors |
| **RNF-005 (PII Encryption)** | `PiiAttributeConverterTest` - 3 tests (2 failing due to config) |

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=MarketplaceServiceTest

# Run specific test method
./mvnw test -Dtest=MarketplaceServiceTest#createSetsTimestampsAndReturnsService

# Run tests for specific module
./mvnw test -Dtest="*services*"

# Skip tests during build
./mvnw package -DskipTests
```

---

## Test Configuration Notes

### Required Dependencies for Full Test Pass

Add to `pom.xml` to fix failing tests:

```xml
<!-- For ServiyaApplicationTests.contextLoads -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- For PiiAttributeConverterTest - already in spring-boot-starter-test -->
<!-- But needs ENCRYPTION_KEY in test properties -->
```

### Environment Variables for Tests

```bash
export ENCRYPTION_KEY="serviya-test-pii-key-for-context-load-strong-32chars-min"
export JWT_SECRET="serviya-test-jwt-secret-for-context-load-strong-32chars-min"
```

---

## Coverage Targets

| Layer | Target | Current |
|-------|--------|---------|
| Domain | 90%+ | High (71 tests) |
| Application Services | 85%+ | High (200+ tests) |
| Adapters/Infrastructure | 70%+ | Moderate |
| Controllers | 60%+ | 18 integration tests |

---

## Maintenance Notes

1. **Flaky Tests:** None detected in current run
2. **Slow Tests:** Integration tests (~1-2s each) due to Spring context startup
3. **Test Data:** Uses builder pattern for domain objects; Mockito for dependencies
4. **Naming Convention:** `<ClassName>Test` for unit, `*IntegrationTest` for controller tests
5. **Annotations:** `@ExtendWith(MockitoExtension.class)` for unit, `@WebMvcTest`/`@SpringBootTest` for integration