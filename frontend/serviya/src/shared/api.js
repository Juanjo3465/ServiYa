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
    return Boolean(getToken());
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
 
    // RF-007 — cambia la contraseña del usuario autenticado
    changePassword: (currentPassword, newPassword) =>
        request('/api/v1/users/me/password', {
            method: 'PATCH',
            auth: true,
            body: { currentPassword, newPassword },
        }),
};

// Ruta de inicio según el rol devuelto por el backend.
export function homePathForRoles(roles = []) {
    if (roles.includes('ADMIN')) return '/admin/dashboard';
    if (roles.includes('OFFERER')) return '/offerer/dashboard';
    return '/dashboard';
}
