// Capa ligera de acceso a la API del backend (RF-001/002/004/005).
// Usa fetch nativo (no hay axios en el proyecto) y guarda el JWT en localStorage.

const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const TOKEN_KEY = 'serviya_token';

// --- Token (JWT) ---
export function saveToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}
export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}
export function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}
export function isAuthenticated() {
    if (!getToken()) return false;
    try {
        const payload = JSON.parse(atob(getToken().split('.')[1]));
        return Math.floor(Date.now() / 1000) < payload.exp;
    } catch {
        return false;
    }
}

// Lee los roles del payload del JWT (el backend los incluye como claim "roles").
export function rolesFromToken(token = getToken()) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.roles ?? [];
    } catch {
        return [];
    }
}

// Llama a la API y devuelve el JSON; si la respuesta es de error, lanza con el mensaje del backend.
async function request(path, { method = 'GET', body, auth = false } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth) {
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;
    }

    const res = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });

    const isJson = res.headers.get('content-type')?.includes('application/json');
    const data = isJson ? await res.json() : null;

    if (!res.ok) {
        const message = data?.message || `Error ${res.status}`;
        throw new Error(message);
    }
    return data;
}

// --- Endpoints ---
export const authApi = {
    // RF-001
    login: (email, password) =>
        request('/api/v1/auth/login', { method: 'POST', body: { email, password } }),
    // RF-002 / RF-004
    register: (payload) =>
        request('/api/v1/auth/register', { method: 'POST', body: payload }),
};

export const profileApi = {
    // RF-005 — identidad tomada del JWT por el backend
    getMyProfile: () => request('/api/v1/users/me/profile', { auth: true }),
    getProfile: (id) => request(`/api/v1/offerers/${id}`, { auth: true }),
    changeMainAddress: (payload) => request(`/api/v1/users/me/main-address`, { method: 'PATCH', body: payload, auth: true }),
 
    // RF-007 — cambia la contraseña del usuario autenticado
    changePassword: (currentPassword, newPassword) =>
        request('/api/v1/users/me/password', {
            method: 'PATCH',
            auth: true,
            body: { currentPassword, newPassword },
        }),
};

export const serviceApi = {
    getMyServices: (offererId) => request(`/api/v1/offerers/${offererId}/services`, { auth: true }),
    createService: (payload) => request('/api/v1/services', { method: 'POST', body: payload, auth: true }),
    updateService: (id, payload) => request(`/api/v1/services/${id}`, { method: 'PATCH', body: payload, auth: true }),
    deleteService: (id) => request(`/api/v1/services/${id}`, { method: 'DELETE', auth: true }),
    searchServices: (params) => {
        const queryParams = new URLSearchParams();
        Object.entries(params || {}).forEach(([key, val]) => {
            if (val !== undefined && val !== null && val !== '') {
                queryParams.append(key, val);
            }
        });
        return request(`/api/v1/services/search?${queryParams.toString()}`, { auth: true });
    },
    getServiceDetail: (id) => request(`/api/v1/services/${id}/detail`, { auth: true }),
};

export const addressApi = {
    getMyAddresses: () => request('/api/v1/users/me/addresses', { auth: true }),
    createAddress: (payload) => request('/api/v1/users/me/addresses', { method: 'POST', body: payload, auth: true }),
    updateAddress: (id, payload) => request(`/api/v1/addresses/${id}`, { method: 'PATCH', body: payload, auth: true }),
    deleteAddress: (id) => request(`/api/v1/addresses/${id}`, { method: 'DELETE', auth: true }),
};

export const requestApi = {
    createRequest: (payload) => request('/api/v1/service-requests', { method: 'POST', body: payload, auth: true }),
    getMyClientRequests: (params = {}) => {
        const queryParams = new URLSearchParams();
        Object.entries(params).forEach(([key, val]) => {
            if (val !== undefined && val !== null && val !== '') {
                queryParams.append(key, val);
            }
        });
        const qs = queryParams.toString();
        return request(`/api/v1/users/me/client-requests${qs ? '?' + qs : ''}`, { auth: true });
    },
    cancelRequest: (id) => request(`/api/v1/service-requests/${id}/cancel`, { method: 'POST', auth: true }),
};

export const categoryApi = {
    getCategories: () => request('/api/v1/categories', { auth: true }),
}

export const reportApi = {
    createRequestReport: (payload) => request('/api/v1/reports/requests', { method: 'POST', body: payload, auth: true }),
    createServiceFeedbackReport: (payload) => request('/api/v1/reports/service-feedback', { method: 'POST', body: payload, auth: true }),
    createClientFeedbackReport: (payload) => request('/api/v1/reports/client-feedback', { method: 'POST', body: payload, auth: true }),
    getAll: (page = 0, size = 20) => request(`/api/v1/reports?page=${page}&size=${size}`, { auth: true }),
};

export const userApi = {
    getDisplayName: async (id) => {
        if (!id) return 'Usuario sin asignar';

        try {
            const data = await request(`/api/v1/offerers/${id}/summary`, { auth: true });
            if (data.fullName || data.name) {
                return data.fullName || data.name;
            }
        } catch {
            // Intenta con el perfil del usuario autenticado si no existe resumen de oferente.
        }

        try {
            const data = await request('/api/v1/users/me/profile', { auth: true });
            if (data.fullName || data.name) {
                return data.fullName || data.name;
            }
        } catch {
            // Si no existe un perfil cargado, conserva el valor por defecto.
        }

        return `Usuario ${id}`;
    },
};

export const metricsApi = {
    getMyMetrics: () => request('/api/v1/users/me/metrics', { auth: true }),
};

export const notificationApi = {
    getNotifications: (params = {}) => {
        const qs = new URLSearchParams();
        if (params.read !== undefined) qs.set('read', params.read);
        if (params.channelId) qs.set('channelId', params.channelId);
        if (params.status) qs.set('status', params.status);
        if (params.page !== undefined) qs.set('page', params.page);
        if (params.size) qs.set('size', params.size);
        const query = qs.toString();
        return request(`/api/v1/notifications${query ? '?' + query : ''}`, { auth: true });
    },
    markAsRead: (id) =>
        request(`/api/v1/notifications/${id}/read`, { method: 'POST', auth: true }),
    getChannels: () =>
        request('/api/v1/notification-channels', { auth: true }),
};

// Ruta de inicio según el rol devuelto por el backend.
export function homePathForRoles(roles = []) {
    if (roles.includes('ADMIN')) return '/admin/dashboard';
    if (roles.includes('OFFERER')) return '/offerer/dashboard';
    return '/dashboard';
}
