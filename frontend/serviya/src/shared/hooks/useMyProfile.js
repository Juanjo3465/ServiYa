import { useQuery } from '@tanstack/react-query';
import { profileApi } from '../api';

/**
 * Perfil del usuario autenticado (`me/profile`) como query compartida y cacheada (M49).
 * Varias páginas lo necesitan (dashboards, perfil, disponibilidad…); al compartir la misma queryKey se
 * pide una sola vez y se reutiliza, en lugar de un fetch-en-efecto por página.
 */
export function useMyProfile(options = {}) {
    return useQuery({
        queryKey: ['me-profile'],
        queryFn: () => profileApi.getMyProfile(),
        staleTime: 5 * 60 * 1000,
        ...options,
    });
}
