# Fix B11: Stacktraces and Error Messages Exposed to Clients

## Bug Description
**Severity:** MEDIO  
**Module:** config  
**Location:** `application.yaml` (lines 30-31)

The application was configured to always include stack traces and error messages in HTTP error responses:
```yaml
server:
  error:
    include-stacktrace: always
    include-message: always
```

This is a security risk because:
- Stack traces reveal internal application structure (class names, method names, line numbers)
- Error messages may contain sensitive information (database queries, file paths, configuration details)
- Attackers can use this information for reconnaissance and targeted attacks

## Root Cause
The `application.yaml` had production-insecure defaults for error handling. These settings are appropriate for development but dangerous in production.

## Fix Applied
1. **Modified `backend/serviya/src/main/resources/application.yaml`** - Changed defaults to secure values:
   ```yaml
   server:
     error:
       include-stacktrace: never
       include-message: never
   ```

2. **Created `backend/serviya/src/main/resources/application-dev.yaml`** - Development profile with debug settings:
   ```yaml
   # Development profile configuration
   # This profile enables debug features that should NOT be used in production
   # Activate with: --spring.profiles.active=dev
   
   spring:
     jpa:
       show-sql: true
   
   logging:
     level:
       org.hibernate.SQL: DEBUG
       org.springframework.web: DEBUG
   
   server:
     error:
       include-stacktrace: always
       include-message: always
   ```

## Configuration Values Explanation
| Value | Description |
|-------|-------------|
| `never` | Never include stacktrace/message (secure, production default) |
| `always` | Always include (development only) |
| `on_param` | Include only when request parameter present (alternative for debugging) |

## Usage
- **Production (default):** No profile needed, uses secure `application.yaml` settings
- **Development:** Activate with `--spring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev` environment variable
- **Docker dev:** The `docker-compose.dev.yml` can be updated to include the dev profile

## Testing
- Verified application compiles successfully with `./mvnw clean compile`
- Configuration validated by Spring Boot on startup
- No existing functionality broken (error responses still work, just without sensitive details)

## Related Issues
- **B6 (ALTO):** Secretos con defaults inseguros embebidos - Part of same security hardening effort (M6)
- **M6:** Gestión de secretos sin defaults + perfiles dev/prod - This fix implements the profile separation part of M6

## Files Changed
- `backend/serviya/src/main/resources/application.yaml` (modified)
- `backend/serviya/src/main/resources/application-dev.yaml` (created)