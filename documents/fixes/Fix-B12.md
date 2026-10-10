# Fix B12: Address Verification Endpoint Returns 500 (Placeholder)

## Bug Description

**Bug ID:** B12
**Severity:** MEDIO
**Module:** profiles
**Endpoint:** `POST /api/v1/addresses/verify`

The endpoint `/addresses/verify` was returning HTTP 500 because the implementation methods `AddressService.verifyAddress()` and `AddressService.getCoordinates()` were placeholders that threw `UnsupportedOperationException`.

## Root Cause

In `AddressService.java` (lines 70-77), both methods were implemented as stubs:

```java
@Override
public boolean verifyAddress(String addressLine, String city) {
    throw new UnsupportedOperationException("TODO: verifyAddress — placeholder, ver estructura-servicios.docx");
}

@Override
public CoordinatesResult getCoordinates(String addressLine, String city) {
    throw new UnsupportedOperationException("TODO: getCoordinates — placeholder, ver estructura-servicios.docx");
}
```

When the controller called these methods, the exception propagated up and resulted in a 500 Internal Server Error.

## Files Related to the Bug

1. **Main implementation:** `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/application/services/AddressService.java`
2. **Port interface:** `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/application/ports/input/AddressServicePort.java`
3. **Controller:** `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/adapters/input/AddressController.java`
4. **DTOs:**
   - `VerifyAddressForm.java` (input)
   - `AddressVerificationResponse.java` (output)
   - `CoordinatesResult.java` (application result)

## Solution Implemented

### 1. Created Geocoding Port (Hexagonal Architecture)

Created `GeocodingPort` interface in `application/ports/output/` to abstract the geocoding implementation:

```java
public interface GeocodingPort {
    Optional<CoordinatesResult> geocode(String addressLine, String city);
}
```

### 2. Implemented Nominatim Adapter

Created `NominatimGeocodingAdapter` in `infrastructure/adapters/output/` using OpenStreetMap's free Nominatim API:

- No API key required
- Configurable base URL, user agent, and timeout
- Can be disabled via `app.geocoding.enabled=false` for testing
- Proper error handling with logging
- Returns `Optional<CoordinatesResult>` to handle not-found cases gracefully

### 3. Updated AddressService

Modified `AddressService` to:
- Inject `GeocodingPort` via constructor (using `@RequiredArgsConstructor`)
- Implement `verifyAddress()` by checking if geocoding returns a result
- Implement `getCoordinates()` by returning coordinates or null values if not found
- Removed the `UnsupportedOperationException` placeholders

### 4. Added Configuration

Added geocoding configuration to `application.yaml`:

```yaml
app:
  geocoding:
    enabled: ${GEOCODING_ENABLED:true}
    nominatim:
      base-url: ${GEOCODING_NOMINATIM_BASE_URL:https://nominatim.openstreetmap.org}
      user-agent: ${GEOCODING_NOMINATIM_USER_AGENT:ServiYa/1.0}
      timeout-seconds: ${GEOCODING_NOMINATIM_TIMEOUT_SECONDS:5}
```

### 5. Added WebClient Configuration

Created `ProfilesConfig` with a `WebClient` bean for the geocoding adapter.

### 6. Added Dependency

Added `spring-boot-starter-webflux` to `pom.xml` for reactive WebClient support.

## Behavior

### When Geocoding is Enabled (Default)
- Calls Nominatim API with the address
- Returns `valid: true` + coordinates if found
- Returns `valid: false` + null coordinates if not found
- Handles network errors gracefully (returns `valid: false`)

### When Geocoding is Disabled
- Returns `valid: false` + null coordinates immediately
- Useful for testing environments without internet access

## Testing

The fix can be tested by:

1. **Unit tests:** Mock `GeocodingPort` to return predefined coordinates
2. **Integration tests:** Start application with `GEOCODING_ENABLED=false` to verify no 500 errors
3. **Manual testing:** Call `POST /api/v1/addresses/verify` with valid address data

Example request:
```bash
curl -X POST http://localhost:8080/api/v1/addresses/verify \
  -H "Content-Type: application/json" \
  -d '{"addressLine": "Calle Mayor 10", "city": "Madrid"}'
```

Expected response (when geocoding enabled and address found):
```json
{
  "valid": true,
  "latitude": 40.4153,
  "longitude": -3.7074
}
```

Expected response (when geocoding disabled or address not found):
```json
{
  "valid": false,
  "latitude": null,
  "longitude": null
}
```

## Future Improvements

1. **Caching:** Add caching layer for frequently queried addresses
2. **Rate limiting:** Respect Nominatim's usage policy (1 request/second)
3. **Alternative providers:** Support Google Maps, Mapbox, etc. via different adapter implementations
4. **Batch geocoding:** Add endpoint for bulk address verification
5. **Test coverage:** Add unit and integration tests for the geocoding functionality

## Related Requirements

- **RF-009:** Verificación de direcciones
- **RNF-019:** Geocodificación de direcciones