# Fix F5: Error Boundaries and Route Error Handling

## Bug Description

**Bug ID:** F5
**Severity:** BAJO
**Area:** ux
**Description:** Sin error boundaries / `errorElement` → error de render = app en blanco

The frontend had no error boundaries. If a React component threw an error during render, the entire app would show a blank white screen with no user-friendly error message or recovery options.

## Root Cause

1. **No ErrorBoundary component** - React's error boundary mechanism was not implemented
2. **App.jsx was a pass-through** - Just returned `children` without any error handling
3. **No route-level errorElement** - React Router v6's `errorElement` was not configured
4. **No fallback UI** - Users saw blank screen on any render error

## Files Related to the Bug

1. **ErrorBoundary component:** `frontend/serviya/src/shared/components/ErrorBoundary/ErrorBoundary.jsx` (created)
2. **App.jsx:** `frontend/serviya/src/app/App.jsx` - Updated to wrap children with ErrorBoundary
3. **ErrorPage component:** `frontend/serviya/src/app/ErrorPage.jsx` (created) - Route-level error page
4. **Routes config:** `frontend/serviya/src/app/routes.jsx` - Added `errorElement` to root route
4. **Export:** `frontend/serviya/src/shared/components/ErrorBoundary/index.js` - Barrel export

## Solution Implemented

### 1. ErrorBoundary Component (Class Component)

Created a class-based ErrorBoundary (required for `componentDidCatch`):

```javascript
class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error('ErrorBoundary caught an error:', error, errorInfo);
        this.setState({ errorInfo });
    }

    render() {
        if (this.state.hasError) {
            // Render fallback UI with recovery options
            return this.props.fallback || (
                <div>...friendly error UI with actions...</div>
            );
        }
        return this.props.children;
    }
}
```

**Features:**
- Catches render errors in child component tree
- Logs errors to console for debugging
- Shows user-friendly fallback UI with:
  - Error icon and message
  - "Ir al inicio" (home) button
  - "Iniciar sesión" (login) button
  - "Recargar página" (reload) button
  - Technical details in development mode (collapsible)

### 2. App.jsx - Wrap with ErrorBoundary

```javascript
export default function App({ children }) {
    return (
        <ErrorBoundary>
            {children}
        </ErrorBoundary>
    );
}
```

This catches errors from any page component.

### 3. Route-Level ErrorPage with errorElement

Created `ErrorPage.jsx` using React Router's `useRouteError()`:

```javascript
export function ErrorPage() {
    const error = useRouteError();
    // Detect error type (404, 401, 500, etc.)
    // Show appropriate message and actions
}
```

**Error types handled:**
- **404 Not Found** - "Página no encontrada" with home/back buttons
- **401 Unauthorized** - "No autorizado" with login button
- **500 Server Error** - "Error del servidor" with retry
- **Generic** - Fallback for other errors

Updated router to use nested routes with errorElement on root:

```javascript
export const router = createBrowserRouter([
    {
        path: "/",
        errorElement: <ErrorPage />,
        children: [
            // all routes as children
        ],
    },
]);
```

### 4. Development Mode Debug Info

Both ErrorBoundary and ErrorPage show technical details in development:
- Error message and stack trace
- Component stack (for ErrorBoundary)
- Raw error object (for ErrorPage)
- Collapsible `<details>` element to avoid clutter

## Behavior

### Component Render Error
1. User navigates to a page
2. Component throws during render
3. ErrorBoundary catches it
4. Shows fallback UI with recovery options
5. Logs error to console

### Route/Loader Error
1. Route loader or action throws
2. React Router catches it
3. Renders `errorElement` (ErrorPage)
4. Shows appropriate message based on error status

### Recovery Options
- **Home** - Navigate to `/`
- **Login** - Navigate to `/login` (for 401)
- **Back** - `history.back()` 
- **Reload** - `window.location.reload()` (clears React state)

## Testing

The fix can be tested by:

1. **Manual error injection:** Add `throw new Error('test')` in a component render → should show ErrorBoundary fallback
2. **404 test:** Navigate to `/non-existent-route` → should show ErrorPage with 404 message
3. **401 simulation:** Clear token, access protected route → ProtectedRoute redirects, but if loader throws 401 → ErrorPage shows
4. **Development details:** In dev mode, click "Detalles técnicos" → shows stack trace
5. **Recovery:** Click "Ir al inicio" → navigates to home page

## Future Improvements

1. **Error reporting service:** Integrate with Sentry, LogRocket, or similar
2. **Error boundary per route:** Add specific error boundaries for different route sections
3. **Retry mechanism:** Add "Reintentar" button that re-attempts the failed operation
4. **Error categorization:** Better error type detection for more specific user messages
5. **Accessibility:** Improve ARIA attributes and keyboard navigation in error UI

## Related Requirements

- **F5:** Error boundaries implementation (this fix)
- **M20:** Error boundaries / `errorElement` por ruta (mejora propuesta en §6.4)

## Verification

- ✅ Build passes (`npm run build`)
- ✅ ErrorBoundary catches render errors
- ✅ ErrorPage handles route errors (404, 401, 500)
- ✅ Recovery buttons work (home, login, back, reload)
- ✅ Development mode shows technical details
- ✅ No circular dependency issues