# Estructura del frontend de ServiYa

Documento de referencia y repaso sobre **cómo está construido el frontend**: qué
tecnologías usa, cómo se organizan las carpetas, cómo arranca la aplicación, cómo navega
entre pantallas y cómo se comunica con el backend. Está escrito para consulta futura y
como material de estudio, así que explica también los conceptos de fondo (React, Vite,
enrutamiento, estado) para quien no venga del mundo frontend.

> **Ubicación en el repo:** `frontend/serviya/`. El frontend es una de las tres piezas del
> monorepo (junto a `backend/serviya` y `mysql/`), cableadas por `docker-compose.yml`.

> **Nota sobre `CLAUDE.md`:** ese archivo describe el frontend como "el starter por
> defecto de Vite, sin UI real". Esa afirmación quedó **desactualizada**: hoy el frontend
> tiene ~30 páginas, sistema de rutas completo, una capa de API que cubre casi todos los
> endpoints del backend y un sistema de diseño CSS propio.

---

## 1. El stack (tecnologías) y por qué cada una

| Pieza | Rol | Analogía con el backend |
|---|---|---|
| **React 19** | Librería para construir la interfaz con **componentes**. | El "modelo de programación" (como Spring MVC define cómo escribes controladores). |
| **Vite 8** | Herramienta que **ejecuta en desarrollo y empaqueta para producción**. | El "Maven/`mvnw`" del frontend: compila y produce el artefacto desplegable. |
| **react-router-dom 7** | **Enrutador**: decide qué pantalla mostrar según la URL. | El mapeo de rutas (`@GetMapping("/...")`), pero del lado del navegador. |
| **react-hook-form 7** | Manejo cómodo de **formularios** (valores, validación, envío). | Bean Validation + binding de formularios. |
| **leaflet / react-leaflet** | **Mapas** interactivos (ubicaciones de servicios). | — |
| **ESLint** | **Linter**: revisa errores y estilo del código. | Checkstyle / SpotBugs. |

**Lenguaje:** JavaScript con **JSX** (extensión `.jsx`). **No hay TypeScript**, es decir,
no hay tipado estático: las variables no declaran su tipo, lo que hace el código más
simple pero también más propenso a errores silenciosos (un `undefined` no se detecta hasta
ejecutar).

### 1.1. ¿Qué es React, en concreto?

Una **librería de JavaScript para construir interfaces**. Tres ideas clave:

1. **Componentes.** La UI se arma con funciones que devuelven algo parecido a HTML. Ese
   "HTML dentro de JS" se llama **JSX**. Un componente puede usar otros como piezas de
   Lego.

   ```jsx
   function Saludo() {
       return <h1>Hola</h1>;   // JSX: no es HTML de verdad, Vite lo traduce
   }
   ```

2. **Estado y re-render.** Describes **cómo se ve la pantalla en función de unos datos**
   (el "estado"). Cuando el estado cambia, React **repinta** el componente solo. No
   manipulas el HTML a mano.

3. **`react-dom`.** Es la parte que conecta React con el navegador (el DOM). Toma el
   componente raíz y lo dibuja dentro de un `<div>` del HTML.

### 1.2. ¿Qué es una SPA?

ServiYa es una **SPA (Single Page Application)**: existe **un solo** archivo HTML
(`index.html`) prácticamente vacío, y JavaScript dibuja todo lo demás dentro de él. Al
navegar de una pantalla a otra **la página no se recarga**; React intercambia el contenido.
Esto da una experiencia fluida (como una app de escritorio) a costa de que el primer
arranque cargue todo el JavaScript.

---

## 2. Árbol de carpetas

```
frontend/serviya/
├── index.html            ← ÚNICO archivo HTML. Punto de entrada del navegador.
├── package.json          ← dependencias + scripts (npm run dev/build/lint/preview)
├── package-lock.json     ← versiones exactas bloqueadas de cada dependencia
├── vite.config.js        ← configuración de Vite (plugin de React, polling para Docker)
├── eslint.config.js      ← reglas del linter (flat config)
├── Dockerfile            ← imagen de PRODUCCIÓN (build + Nginx)
├── Dockerfile.dev        ← imagen de DESARROLLO (npm run dev con --host)
├── nginx.conf            ← config del servidor Nginx que sirve el build en producción
├── public/               ← estáticos servidos tal cual (logo.svg, favicon.svg, icons.svg)
├── dist/                 ← SALIDA del build (generada por Vite; NO se edita a mano)
└── src/                  ← TODO el código fuente
    ├── main.jsx          ← arranque: monta React en el #root del HTML
    ├── app/
    │   ├── App.jsx       ← componente raíz (hoy un envoltorio mínimo)
    │   └── routes.jsx    ← ★ el mapa URL → página (react-router)
    ├── styles/
    │   └── globals.css   ← sistema de diseño global (variables + clases .btn, .input…)
    ├── shared/           ← código REUTILIZABLE entre módulos
    │   ├── api.js        ← ★ toda la comunicación con el backend + manejo del JWT
    │   ├── index.js      ← "barrel": re-exporta todo lo compartido en un punto
    │   ├── navConfig.js  ← definición de los menús laterales por rol
    │   ├── components/    ← componentes compartidos (Navbar, Modal, Icon, Stars…)
    │   └── hooks/        ← hooks reutilizables (useToast)
    └── modules/          ← ★ un folder por área de negocio (espejo del backend)
        ├── users/        ← login, registro, recuperar contraseña
        ├── profiles/     ← perfil propio y perfil público del oferente
        ├── services/     ← home, búsqueda, detalle, servicios del oferente, disponibilidad
        ├── requests/     ← solicitudes, agendas, historiales, reprogramaciones
        ├── notifications/
        ├── admin/        ← panel de administración (usuarios, reportes, feedback, servicios)
        ├── metrics/      feedback/  reports/  config/
```

**Idea rectora:** la estructura de `modules/` **imita la del backend**. El backend tiene
módulos por capacidad (`users`, `profiles`, `services`, `requests`, `feedback`, `metrics`,
`reports`, `notifications`, `admin`); aquí es lo mismo. Esto es deliberado: facilita ubicar
dónde vive cada cosa y mapear una funcionalidad de punta a punta.

### 2.1. Patrón interno de cada módulo

Cada módulo se organiza igual (ejemplo `services/`):

```
modules/services/
├── index.js              ← re-exporta las páginas del módulo (barrel)
├── pages/                ← PÁGINAS completas (una por URL/ruta)
│   ├── HomePage/
│   │   ├── HomePage.jsx  ← el componente de la página
│   │   └── HomePage.css  ← estilos SOLO de esa página
│   ├── SearchPage/
│   └── ServiceDetailPage/
└── components/           ← PIEZAS reutilizables dentro del módulo
    ├── ServiceCard/      (ServiceCard.jsx + ServiceCard.css)
    ├── CategoryCard/
    └── Hero/
```

Convenciones constantes en todo el proyecto:

- **Cada componente en su propia carpeta**, con su `.jsx` y su `.css` al lado (mismo
  nombre). El CSS de esa carpeta aplica solo a ese componente.
- **Página vs. componente:** una *página* (`pages/`) es una pantalla completa que
  corresponde a una URL. Un *componente* (`components/`) es una pieza menor reutilizable
  (una tarjeta, un modal, un botón especial).
- **Nombres** (según `convenciones-nombres-carpetas-archivos.md`): carpetas en
  `kebab-case` en teoría, aunque en la práctica las carpetas de componente usan el nombre
  del componente; **componentes React en `PascalCase`** (`ServiceCard.jsx`); utilidades y
  hooks en `camelCase` (`useToast.js`, `utils.js`); CSS junto al componente con su mismo
  nombre.

### 2.2. El truco de los `index.js` ("barrels")

Un **barrel** es un archivo sin lógica que solo **re-exporta** cosas de otros archivos,
para lograr imports limpios. Ejemplo, `modules/users/index.js`:

```js
export { LoginPage } from './pages/LoginPage/LoginPage';
export { RegisterPage } from './pages/RegisterPage/RegisterPage';
export { RecoverPasswordPage } from './pages/RecoverPasswordPage/RecoverPasswordPage';
```

Gracias a esto, el resto del código escribe:

```js
import { LoginPage, RegisterPage } from '../modules/users';   // limpio
// en vez de:
import { LoginPage } from '../modules/users/pages/LoginPage/LoginPage';
```

`shared/index.js` hace lo mismo a lo grande: reúne **todos** los componentes compartidos y
**toda** la API en un solo punto, por eso las páginas hacen
`import { Icon, authApi, saveToken } from '../../../../shared'`.

---

## 3. Arranque y enrutamiento (el flujo más importante)

Sigue la cadena de principio a fin:

**Paso 1 — `index.html`.** Lo primero que carga el navegador. Casi vacío:

```html
<div id="root"></div>
<script type="module" src="/src/main.jsx"></script>
```

Un `<div>` vacío donde React pintará todo, y una etiqueta que dice "ejecuta `main.jsx`".

**Paso 2 — `src/main.jsx`.** El arranque:

```jsx
ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <RouterProvider router={router} />
    </React.StrictMode>
);
```

"Busca el `div#root` y píntame dentro el enrutador (`router`)". Aquí también se importa
`globals.css`, por eso los estilos globales aplican a toda la app. `<React.StrictMode>` es
solo una ayuda de desarrollo que avisa de malas prácticas (no afecta a producción).

**Paso 3 — `src/app/routes.jsx`.** La tabla **URL → página**, con `createBrowserRouter`:

```jsx
export const router = createBrowserRouter([
    { path: "/",             ...page(<HomePage />) },        // pública
    { path: "/login",        ...page(<LoginPage />) },
    { path: "/services/:id", ...page(<ServiceDetailPage />) },
    { path: "/dashboard",    ...page(<ClientDashboardPage />) },   // cliente
    { path: "/offerer/dashboard", ...page(<OffererDashboardPage />) }, // oferente
    { path: "/admin/users",  ...page(<AdminUsersPage />) },  // admin
    // …
]);
```

- Cada objeto asocia una **ruta** (URL) con la **página** a mostrar.
- `:id` es un **parámetro variable**: `/services/42` y `/services/99` usan la misma página
  `ServiceDetailPage`, que luego lee ese id de la URL con el hook `useParams()`.
- Las rutas están **agrupadas por comentarios** en: públicas, de cuenta (compartidas),
  de cliente, de oferente y de admin — coincidiendo con los tres roles del sistema.
- `page(...)` es un helper local que envuelve cada página en `<App>`:
  `const page = (element) => ({ element: <App>{element}</App> });`
- `src/app/App.jsx` hoy es un envoltorio mínimo (`return children;`). Es el punto donde en
  el futuro irían cosas comunes a todas las páginas (un proveedor de contexto global, el
  contenedor de toasts, etc.).

### 3.1. Navegación entre páginas

Dentro de los componentes hay dos formas de moverse (ambas de react-router, **sin recargar
la página**):

- **`<Link to="/register">`** — como un `<a href>` pero interno de la SPA.
- **`useNavigate()`** — para navegar desde código; p. ej. tras un login exitoso:
  `navigate('/dashboard')`.

### 3.2. Limitación actual: rutas sin protección

Hoy **las rutas no están protegidas en el frontend**. Cualquiera puede escribir
`/admin/users` en la barra de direcciones y llegar al componente (aunque el **backend**
rechaza las llamadas sin un JWT de admin, así que no se filtran datos). Falta un "guardia
de rutas" (route guard) que redirija a `/login` si no hay sesión, o al home del rol si no
tienes permiso. Es una mejora típica pendiente. Las piezas para construirlo ya existen:
`isAuthenticated()`, `rolesFromToken()` y `homePathForRoles()` en `shared/api.js`.

---

## 4. Comunicación con el backend: `src/shared/api.js`

El archivo **más importante** para entender cómo el frontend habla con la API de Spring
Boot. Usa el `fetch` nativo del navegador (**no hay axios**). Tiene tres partes.

### 4.1. La función base `request(...)`

Envoltorio que centraliza todo lo repetitivo de una llamada HTTP:

```js
async function request(path, { method = 'GET', body, auth = false, formData = false }) {
    const headers = {};
    if (!formData) headers['Content-Type'] = 'application/json';
    if (auth) {
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;   // ← el JWT
    }
    const res = await fetch(`${API_BASE}${path}`, { method, headers, body: ... });
    const data = isJson ? await res.json() : null;
    if (!res.ok) throw new Error(data?.message || `Error ${res.status}`);
    return data;
}
```

Qué hace por ti:

- Pone `Content-Type: application/json` (salvo envíos `multipart`/`formData`, p. ej. subir
  una foto).
- Si `auth: true`, adjunta `Authorization: Bearer <token>` — así se conecta con la
  seguridad JWT del backend.
- Si la respuesta es un error (status ≠ 2xx), **lanza una excepción** con el mensaje que
  devolvió el backend. Por eso las páginas usan `try { … } catch (e) { setError(e.message) }`.

`API_BASE` sale de `import.meta.env.VITE_API_URL` o, por defecto, `http://localhost:8080`.
Las variables que empiezan con `VITE_` son la forma de Vite de exponer variables de entorno
al frontend.

### 4.2. Manejo del token (JWT)

```js
saveToken(token)      // guarda el JWT en localStorage (clave 'serviya_token')
getToken()            // lo lee
clearToken()          // lo borra (logout)
isAuthenticated()     // decodifica el JWT y comprueba que no haya expirado (claim 'exp')
rolesFromToken()      // lee el claim 'roles' del JWT
homePathForRoles()    // ruta de inicio según rol (ADMIN → /admin/dashboard, etc.)
```

El JWT que devuelve el backend al hacer login se guarda en **`localStorage`** (almacén del
navegador que persiste aunque cierres la pestaña). El frontend **decodifica el JWT por su
cuenta** (con `atob`, que descodifica base64) para leer expiración y roles, sin volver a
preguntar al backend. **Importante:** esto solo sirve para decidir qué UI mostrar; la
seguridad real la valida el backend en cada petición.

### 4.3. Endpoints agrupados por dominio

El resto del archivo son objetos que agrupan las llamadas por área, mapeando 1:1 con los
endpoints del backend. Los comentarios referencian los requisitos del backlog
(`RF-001`, `RF-005`…), así que están alineados con el diseño documentado en
`estructura-endpoints.md`.

| Objeto | Cubre |
|---|---|
| `authApi` | `/api/v1/auth/**` — login, register. |
| `accountApi` | cuenta propia (`/users/me`): roles, adquirir rol, borrar cuenta. |
| `profileApi` | perfil propio y público, foto, dirección principal, cambio de contraseña. |
| `serviceApi` | servicios del oferente, CRUD, disponibilidades, búsqueda, detalle. |
| `addressApi` | direcciones del usuario. |
| `requestApi` | solicitudes: crear, listar (cliente/oferente), aceptar/rechazar/cancelar, completar, agenda. |
| `proposalApi` | propuestas de reprogramación (crear/aceptar/rechazar/cancelar, recibidas/enviadas). |
| `availabilityApi` | disponibilidad general del oferente (GET/PUT). |
| `categoryApi`, `platformApi` | categorías y estadísticas públicas de la plataforma. |
| `metricsApi` | métricas propias, del oferente y de tags. |
| `notificationApi` | notificaciones y canales. |
| `reportApi`, `moderationApi` | crear reportes y acciones de moderación (advertir/banear/revertir/cerrar). |
| `adminApi`, `adminFeedbackApi`, `adminServiceApi` | panel de administración. |
| `feedbackApi` | feedback de servicio (cliente→servicio) y de cliente (oferente→cliente). |
| `userApi` | utilidades para resolver nombres a mostrar. |

Ejemplo de método (cada uno es una línea que traduce a una llamada HTTP):

```js
export const authApi = {
    login: (email, password) =>
        request('/api/v1/auth/login', { method: 'POST', body: { email, password } }),
};
```

**Consecuencia práctica:** cuando implementes una página nueva, casi siempre **el método de
API ya existe** en este archivo; solo lo importas desde `shared` y lo llamas.

---

## 5. Estilos: `globals.css` + CSS por componente

El proyecto usa **CSS plano** (sin Tailwind ni librerías de componentes tipo Material UI),
en dos niveles:

1. **`src/styles/globals.css` — el sistema de diseño.** Define en `:root` variables CSS
   (colores de marca, radios, sombras) y un conjunto de **clases utilitarias** reutilizables
   (`.btn`, `.btn-primary`, `.input`, `.card`, `.nav`…). Por eso en el JSX ves
   `className="btn btn-primary btn-lg"` y funciona sin importar nada: esas clases viven aquí
   y aplican globalmente. Fue portado de un mockup original.

   ```css
   :root {
     --c-primary: #06B6D4;   /* cyan de la marca */
     --c-text: #0F172A;
     --r: 10px;              /* radio de borde estándar */
     --sh: 0 4px 16px …;     /* sombra estándar */
   }
   ```

2. **CSS por componente** (`LoginPage.css`, `ServiceCard.css`…) — estilos específicos de esa
   pantalla o pieza, importados dentro de su `.jsx`:

   ```jsx
   import './auth.css';
   import './LoginPage.css';
   ```

> **Ojo — el CSS no está aislado por defecto.** En React, importar un `.css` lo añade
> globalmente; **no es "scoped"**. Si defines `.card` en dos archivos, se pisan. La
> convención aquí es usar nombres específicos por sección (`.auth-side`, `.role-tab`) para
> evitar choques.

### 5.1. Dos gotchas de JSX que confunden al empezar

- **`className`, no `class`.** En JSX el atributo HTML `class` se escribe **`className`**
  (porque `class` es palabra reservada de JavaScript). Es la confusión #1 de quien llega
  desde HTML.
- **Estilos en línea con objeto:** `style={{ height: '24px' }}` — doble llave y propiedades
  en `camelCase` (`marginBottom`, no `margin-bottom`).

### 5.2. El componente `Icon`

En vez de imágenes o una librería de iconos, hay un componente propio
`<Icon name="bell" size={16} />`. Internamente (`shared/components/Icon/Icon.jsx`) mantiene
un diccionario `PATHS` con los trazos SVG de cada icono (estilo Feather, 24×24,
`stroke = currentColor`, así heredan el color del texto). Muy usado en toda la UI.

---

## 6. Anatomía de una página (patrón típico)

`LoginPage.jsx` sirve de plantilla mental de casi cualquier página:

```jsx
export function LoginPage() {
    const navigate = useNavigate();               // para redirigir
    const [email, setEmail] = useState('');       // ESTADO del componente
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleLogin = async () => {
        setLoading(true);
        try {
            const auth = await authApi.login(email, password);  // llama al backend
            saveToken(auth.token);                              // guarda el JWT
            const roles = rolesFromToken(auth.token);
            navigate(homePathForRoles(roles));                  // redirige por rol
        } catch (e) {
            setError(e.message);                                // muestra el error
        } finally {
            setLoading(false);
        }
    };

    return ( /* JSX del formulario */ );
}
```

Los conceptos de React que aparecen aquí y verás en todas partes:

- **`useState`** — el **estado**. `const [email, setEmail] = useState('')` crea una variable
  `email` (valor actual) y una función `setEmail` (para cambiarla). Nunca haces
  `email = …` a mano; siempre `setEmail(…)`, y eso dispara un **re-render**.
- **Inputs controlados** (*controlled inputs*):
  `<input value={email} onChange={e => setEmail(e.target.value)} />`. El input muestra el
  estado y cada tecla lo actualiza, así el componente siempre "sabe" lo que el usuario
  escribió.
- **Hooks** — funciones especiales que empiezan por `use` (`useState`, `useNavigate`,
  `useParams`, `useToast`). Dan superpoderes a los componentes. **Regla:** solo se llaman en
  el nivel superior del componente, nunca dentro de `if`, bucles o funciones anidadas.

`useToast` (`shared/hooks/useToast.js`) es un hook **propio** para mostrar notificaciones
flotantes ("toasts") que desaparecen a los 4 segundos.

---

## 7. Componentes y utilidades compartidas (`src/shared/`)

Piezas reutilizables entre módulos, re-exportadas por `shared/index.js`:

| Componente / util | Para qué |
|---|---|
| `Icon`, `WhatsAppIcon` | Set de iconos SVG (`<Icon name="…" size={…} />`). |
| `Navbar`, `AppNavbar` | Barras de navegación superiores (pública / autenticada). |
| `Sidebar`, `DashboardLayout` | Menú lateral y layout de las pantallas de panel. |
| `Footer` | Pie de página. |
| `Modal` | Ventana modal genérica. |
| `Stars`, `StarRating` | Mostrar y elegir calificación por estrellas. |
| `StatCard` | Tarjeta de métrica/estadística. |
| `WhatsAppButton` | Botón flotante de contacto. |
| `Toast` / `ToastContainer` + `useToast` | Notificaciones flotantes temporales. |
| `navConfig.js` (`CLIENT_NAV`, `OFFERER_NAV`) | Definición declarativa de los menús laterales por rol (label, ruta, icono, badge). |

---

## 8. Cómo ejecutar el frontend

### 8.1. En local con npm (lo más ágil para tocar UI)

Requiere **Node.js 22+** y el **backend corriendo en `:8080`**. Desde `frontend/serviya/`:

```bash
npm install     # instala dependencias (1ª vez o si cambió package.json)
npm run dev     # Vite en http://localhost:5173 con hot-reload (HMR)
npm run build   # compila a dist/ (producción)
npm run preview # sirve el dist/ para probarlo
npm run lint    # ESLint sobre todo el proyecto
```

Para apuntar a otro backend, crea `frontend/serviya/.env.local` con
`VITE_API_URL=http://otra-url:puerto`.

### 8.2. Con Docker (todo el sistema junto)

Desde la raíz del repo. Modo desarrollo (hot-reload dentro del contenedor):

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

- `Dockerfile.dev` usa `node:22`, instala dependencias y corre `npm run dev -- --host`
  (el `--host` expone el servidor fuera del contenedor). `vite.config.js` activa
  `watch: { usePolling: true }` porque, dentro de Docker en Windows, la detección normal de
  cambios de archivos falla y hay que sondear.
- `Dockerfile` (producción) + `nginx.conf` construyen con Vite (`npm run build`) y sirven
  los estáticos con Nginx.

> Detalle completo (incluido el gotcha del volumen anónimo `node_modules` al añadir
> dependencias, que exige el flag `-V`) en **`documents/GUIA-EJECUCION-SERVIYA.md`**.

---

## 9. Glosario rápido

- **Componente:** función que devuelve JSX y representa un trozo de UI.
- **JSX:** sintaxis que mezcla HTML dentro de JavaScript; Vite la traduce.
- **Estado (`state`):** datos internos de un componente; al cambiar, la UI se repinta.
- **Hook:** función `use…` que añade capacidades a un componente (estado, navegación…).
- **Prop:** dato que un componente recibe de su "padre" (como un parámetro de función).
- **Render / re-render:** el acto de React de (re)pintar un componente en pantalla.
- **SPA:** una sola página HTML; JS dibuja y cambia todo sin recargar.
- **Barrel:** archivo `index.js` que solo re-exporta, para imports limpios.
- **HMR (Hot Module Replacement):** recarga instantánea al guardar, sin perder el estado.
- **Bundle / build:** el paquete optimizado que produce Vite en `dist/`.
- **`localStorage`:** almacén clave-valor del navegador que persiste entre sesiones (aquí
  guarda el JWT).

---

## 10. Documentos relacionados

- `documents/GUIA-EJECUCION-SERVIYA.md` — comandos para levantar/mantener el proyecto
  (Docker y npm local).
- `documents/convenciones-nombres-carpetas-archivos.md` — cómo nombrar carpetas y archivos
  (incluye la sección de frontend).
- `documents/project-structure/estructura-endpoints.md` — la API REST del backend a la que
  `shared/api.js` llama.
- `documents/project-structure/GUIA_DTOS.txt` — convención de DTOs del backend (contexto de
  los cuerpos que envía/recibe el frontend).
