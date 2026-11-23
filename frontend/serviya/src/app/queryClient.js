import { QueryClient } from "@tanstack/react-query";

/**
 * Cliente de React Query (M46 — capa de estado de servidor).
 *
 * El refresh de sesión NO cambia: los fetchers siguen siendo las funciones de `api.js`
 * (`request()`/`doRequest()`), que ya hacen 401 -> refresh (una vez, single-flight) -> reintento.
 * React Query solo ve el resultado final. Por eso NO reintentamos en 401/403: si llega un 401 es que el
 * refresh ya falló (api.js limpió la sesión y redirigió a /login), y el 403 es "sin permiso" (no se reintenta).
 */
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: (failureCount, error) => {
                const status = error?.status;
                if (status === 401 || status === 403) return false;
                return failureCount < 2; // errores de red / 5xx: un par de reintentos
            },
            staleTime: 30_000,          // 30 s "fresco": evita refetch inmediato al re-montar/navegar
            refetchOnWindowFocus: false, // sin refetch sorpresa al volver a la pestaña (se puede activar luego)
        },
        mutations: {
            retry: false,
        },
    },
});
