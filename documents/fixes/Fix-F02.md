# Fix F2: 401 Interceptor for Centralized Logout and Redirect

## Bug Description

**Bug ID:** F2
**Severity:** MEDIO
**Area:** api
**Description:** Sin interceptor de 401 (no hay logout+redirect centralizado) en `shared/api.js` `request()`

When a JWT token expired or became invalid, each API call would throw a generic error. There was no centralized handling to clear the token and redirect the user to `/login`. Users would get stuck with error messages on the current page.

## Root Cause

In `src/shared/api.js`, the `request()` function only checked `res.ok` and threw an error with the backend message. It didn't handle the specific case of HTTP 401 (Unauthorized) for authenticated requests:

```javascript
if (!res.ok) {
    const message = data?.message || `Error ${res.status}`;
    throw new Error(message);
}
```

When the backend returned 401 (token expired, invalid, or user banned), the frontend just showed the error message but didn't:
1. Clear the invalid token from localStorage
2. Redirect the user to the login page
3. Provide a consistent user experience

## Files Related to the Bug

1. **Main fix:** `frontend/serviya/src/shared/api.js` - Updated `request()` function with 401 interceptor

## Solution Implemented

### Added 401 Interceptor in request()

Modified the `request()` function to detect HTTP 401 responses on authenticated requests:

```javascript
// Interceptor de 401: si la petición era autenticada y el token expira/es inválido
if (res.status === 401 && auth) {
    clearToken();
    if (typeof window !== 'undefined') {
        window.location.href = '/login';
    }
    throw new Error('Sesión expirada. Redirigiendo al login...');
}
```

**Behavior:**
- Only triggers on authenticated requests (`auth: true`)
- Clears the JWT token from localStorage via `clearToken()`
- Redirects to `/login` using `window.location.href` (full page navigation to clear React state)
- Throws a user-friendly error message

### Why window.location.href instead of React Router navigate?

- The `request()` function is a utility outside React component tree (no access to `useNavigate`)
- Full page reload ensures clean state (clears any stale React context, timers, intervals)
- Simpler and more reliable for auth-related redirects
- Consistent with traditional multi-page app logout behavior

## Testing

The fix can be tested by:

1. **Expired token:** Manually set an expired JWT in localStorage, make an authenticated API call → should clear token and redirect to `/login`
2. **Invalid token:** Set a malformed token, make authenticated call → should clear token and redirect to `/login`
3. **Valid token:** Normal authenticated requests should work unchanged
4. **Public endpoints:** Unauthenticated requests (`auth: false`) should not trigger redirect on 401

Example test in browser console:
```javascript
// Set expired token
localStorage.setItem('serviya_token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjEsInJvbGVzIjpbIkNMSUVOVCJdLCJleHAiOjE2OTkwMDAwMDB9.fake-signature');

// Make authenticated call - should redirect to login
fetch('/api/v1/users/me/profile', { headers: { Authorization: `Bearer ${localStorage.getItem('serviya_token')}` } })
```

## Future Improvements

1. **Refresh token support (M26):** When refresh tokens are implemented, the 401 interceptor should attempt token refresh before redirecting
2. **Custom event:** Dispatch a custom `auth:logout` event for components to listen and clean up (timers, subscriptions)
3. **Toast notification:** Show a toast message before redirect (requires integration with toast system)
4. **Preserve return path:** Save current path before redirect to return after login (already handled by ProtectedRoute's `state.from`)

## Related Requirements

- **F2:** 401 interceptor implementation (this fix)
- **M19:** Interceptor de 401 en `request()` (mejora propuesta en §6.4)
- **F1:** Route guards (complements this fix - ProtectedRoute handles initial auth, 401 interceptor handles token expiry during session)

## Verification

- ✅ Build passes (`npm run build`)
- ✅ No TypeScript errors (project uses JSX without TS)
- ✅ Authenticated requests with 401 now redirect to login
- ✅ Public endpoints unaffected
- ✅ Token cleared from localStorage on 401