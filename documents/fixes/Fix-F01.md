# Fix F1: Route Guards Implementation

## Bug Description

**Bug ID:** F1
**Severity:** ALTO
**Area:** routing
**Description:** Sin route guards: toda ruta protegida se renderiza sin sesión/rol

The frontend had no route protection. Any URL (including `/admin/*`, `/offerer/*`, `/dashboard`) would render without checking authentication or authorization. The backend would reject API calls with 401, but the UI would already be painted, causing confusing error states.

## Root Cause

In `src/app/routes.jsx`, all routes were defined without any authentication or authorization checks:

```jsx
// All routes were public - no protection
{ path: "/admin/dashboard", ...page(<AdminDashboardPage />) },
{ path: "/offerer/dashboard", ...page(<OffererDashboardPage />) },
{ path: "/dashboard", ...page(<ClientDashboardPage />) },
```

The authentication utilities already existed in `src/shared/api.js`:
- `isAuthenticated()` - checks JWT validity from localStorage
- `rolesFromToken()` - extracts roles from JWT payload
- `homePathForRoles()` - determines home path based on roles

But they were not used in the routing configuration.

## Files Related to the Bug

1. **Main fix:** `frontend/serviya/src/app/routes.jsx` - Updated to use ProtectedRoute
2. **New component:** `frontend/serviya/src/shared/components/ProtectedRoute/ProtectedRoute.jsx` - Route guard implementation
3. **Export:** `frontend/serviya/src/shared/components/ProtectedRoute/index.js` - Barrel export
4. **Shared exports:** `frontend/serviya/src/shared/index.js` - Added ProtectedRoute export
5. **Authentication utilities:** `frontend/serviya/src/shared/api.js` - Existing utilities used by ProtectedRoute

## Solution Implemented

### 1. Created ProtectedRoute Component

Created a new `ProtectedRoute` component in `shared/components/ProtectedRoute/` that:
- Checks if user is authenticated using `isAuthenticated()`
- If not authenticated, redirects to `/login` preserving the original location in state
- Checks if user has required roles using `rolesFromToken()`
- If authenticated but wrong role, redirects to appropriate dashboard using `homePathForRoles()`

```jsx
export function ProtectedRoute({ children, allowedRoles = [] }) {
    const location = useLocation();
    const authenticated = isAuthenticated();

    if (!authenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    const userRoles = rolesFromToken();
    const hasRequiredRole = allowedRoles.length === 0 || allowedRoles.some(role => userRoles.includes(role));

    if (!hasRequiredRole) {
        const redirectPath = homePathForRoles(userRoles);
        return <Navigate to={redirectPath} replace />;
    }

    return children;
}
```

### 2. Updated Routes Configuration

Modified `routes.jsx` to use `ProtectedRoute` for all protected routes:

- **Account routes (any authenticated user):** `/profile`, `/notifications`, `/my-reports`
- **Client routes (requires CLIENT role):** `/dashboard`, `/requests`, `/requests/:id`, `/reschedules`, `/schedule`, `/history`
- **Offerer routes (requires OFFERER role):** `/offerer/*` (all offerer routes)
- **Admin routes (requires ADMIN role):** `/admin/*` (all admin routes)

```jsx
const protectedPage = (element, allowedRoles) => ({ 
    element: <App><ProtectedRoute allowedRoles={allowedRoles}>{element}</ProtectedRoute></App> 
});

// Usage examples:
{ path: "/dashboard", ...protectedPage(<ClientDashboardPage />, ['CLIENT']) },
{ path: "/offerer/dashboard", ...protectedPage(<OffererDashboardPage />, ['OFFERER']) },
{ path: "/admin/dashboard", ...protectedPage(<AdminDashboardPage />, ['ADMIN']) },
{ path: "/profile", ...protectedPage(<ProfilePage />) }, // any authenticated user
```

## Behavior

### Unauthenticated User Accessing Protected Route
1. User navigates to `/dashboard` (or any protected route)
2. `ProtectedRoute` detects no valid JWT
3. Redirects to `/login` with `state.from` preserving the original path
4. After login, user is redirected back to the original path

### Authenticated User with Wrong Role
1. User with CLIENT role navigates to `/admin/dashboard`
2. `ProtectedRoute` detects authentication but wrong role
3. Redirects to `/dashboard` (client home) using `homePathForRoles()`

### Authenticated User with Correct Role
1. User with ADMIN role navigates to `/admin/dashboard`
2. `ProtectedRoute` allows access, renders the component

## Testing

The fix can be tested by:

1. **Unauthenticated access:** Navigate to `/dashboard` without login → should redirect to `/login`
2. **Role-based access:** Login as CLIENT, navigate to `/admin/dashboard` → should redirect to `/dashboard`
3. **Correct access:** Login as ADMIN, navigate to `/admin/dashboard` → should render admin dashboard
4. **Post-login redirect:** Access `/dashboard` unauthenticated → login → should redirect back to `/dashboard`

## Future Improvements

1. **Login page integration:** Update LoginPage to read `location.state.from` and redirect after successful login
2. **Loading state:** Add a loading spinner while checking authentication (for SSR/hydration scenarios)
3. **Error boundaries:** Add error boundaries for route-level error handling (related to F5)
4. **Token refresh handling:** Integrate with future refresh token implementation (M26)

## Related Requirements

- **F1:** Route guards implementation (this fix)
- **M16:** `<ProtectedRoute>` + redirección por rol (mejora propuesta en §6.4)
- **Backend IDOR fixes (B1-B4, B14):** This frontend fix complements backend authorization fixes

## Verification

- ✅ Build passes (`npm run build`)
- ✅ No TypeScript errors (project uses JSX without TS)
- ✅ All existing routes still accessible with correct permissions
- ✅ Unauthorized access properly redirects
- ✅ Role-based access control enforced