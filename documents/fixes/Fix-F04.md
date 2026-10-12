# Fix F4: RequestServicePage - Implement Functional Request Wizard

## Bug Description

**Bug ID:** F4
**Severity:** BAJO
**Area:** routing
**Description:** `RequestServicePage` = maqueta 100% (0 llamadas API) en ruta muerta `/request-service`

The RequestServicePage was a completely static mock with hardcoded data, no API integration, and no real functionality. The route `/request-service` was public and led to a non-functional wizard.

## Root Cause

The original `RequestServicePage.jsx` contained:
- Hardcoded service data ("Reparación de tuberías", "Carlos Martínez")
- Static time slots array
- Hardcoded address options
- No API calls whatsoever
- Mock success modal with no real request creation
- Public route accessible without authentication

The page was essentially a UI prototype that was never connected to the backend.

## Files Related to the Bug

1. **Main fix:** `frontend/serviya/src/modules/requests/pages/RequestServicePage/RequestServicePage.jsx` - Complete rewrite
2. **Routes:** `frontend/serviya/src/app/routes.jsx` - Updated route to `/request-service/:serviceId` with CLIENT role protection
3. **API:** `frontend/serviya/src/shared/api.js` - Uses `serviceApi.getServiceDetail`, `serviceApi.getServiceAvailability`, `addressApi.getMyAddresses`, `addressApi.createAddress`, `requestApi.createRequest`

## Solution Implemented

### 1. Route Changes

**Changed from:** `/request-service` (public)
**Changed to:** `/request-service/:serviceId` (protected, requires CLIENT role)

```jsx
{ path: "/request-service/:serviceId", ...protectedPage(<RequestServicePage />, ['CLIENT']) }
```

This ensures:
- User must be authenticated
- User must have CLIENT role
- Service ID comes from URL parameter (linked from service detail page)

### 2. Complete Page Rewrite

The new implementation:

**Step 1 - Service Confirmation:**
- Fetches service details via `serviceApi.getServiceDetail(serviceId)`
- Displays real service title, description, offerer name, category, price, duration, radius
- Validates service exists before showing wizard

**Step 2 - Date/Time Selection:**
- Fetches service availability via `serviceApi.getServiceAvailability(service.id)`
- Filters active time slots
- Date picker with min date = today
- Time slot selection from real availability data

**Step 3 - Address Selection:**
- Loads user addresses via `addressApi.getMyAddresses()`
- Dropdown to select existing address
- Option to create new address via `addressApi.createAddress()`
- Shows selected address with map placeholder

**Step 4 - Confirmation & Submit:**
- Shows all selected details for review
- Calls `requestApi.createRequest()` with payload:
  ```javascript
  {
      serviceId: service.id,
      scheduledAt: `${selectedDate}T${selectedSlot}:00`,
      addressId: Number(selectedAddressId),
  }
  ```
- On success: shows modal with request ID, redirects to `/requests`

### 3. Error Handling & UX

- Loading state while fetching service
- Error toasts for failed API calls
- Redirect to `/services` if service not found
- Form validation (required fields)
- Disabled buttons during submission
- Success toast and modal with navigation to requests list

## Usage Flow

1. User browses services on `/services` (SearchPage)
2. Clicks service to view `/services/:id` (ServiceDetailPage)
3. ServiceDetailPage should have "Solicitar servicio" button linking to `/request-service/:serviceId`
4. RequestServicePage loads service data and guides through 4-step wizard
5. On submit, creates request via API and redirects to `/requests`

## Testing

The fix can be tested by:

1. **Navigation:** Go to `/services`, click a service, click "Solicitar servicio" → should go to `/request-service/:id`
2. **Authentication:** Access `/request-service/1` without login → redirects to `/login`
3. **Role check:** Login as OFFERER, access `/request-service/1` → redirects to `/offerer/dashboard`
4. **Full flow:** Login as CLIENT, complete wizard → creates request, shows success, redirects to `/requests`
5. **Error cases:** Invalid service ID → error toast + redirect to `/services`
6. **Address creation:** Add new address in step 3 → saves and selects it

## Future Improvements

1. **ServiceDetailPage integration:** Add "Solicitar servicio" button on ServiceDetailPage linking to this wizard
2. **Availability calendar:** Show available dates in calendar view instead of just date picker
3. **Price calculation:** Show estimated total based on duration
4. **Address verification:** Integrate with geocoding (B12) to validate address
5. **Draft saving:** Save wizard progress to localStorage for recovery

## Related Requirements

- **F4:** RequestServicePage implementation (this fix)
- **M21:** Decidir destino de `RequestServicePage` (cablear o borrar la ruta muerta) (mejora propuesta en §6.4)
- **RF-023:** Creación de solicitudes de servicio (backend endpoint exists)

## Verification

- ✅ Build passes (`npm run build`)
- ✅ Route requires CLIENT role authentication
- ✅ Service data loaded from API
- ✅ Availability loaded from API
- ✅ Addresses loaded from API
- ✅ New address creation works
- ✅ Request creation via API works
- ✅ Success redirect to `/requests`
- ✅ Error handling with toasts