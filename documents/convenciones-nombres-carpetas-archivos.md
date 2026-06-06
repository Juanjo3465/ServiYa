# Convenciones de nombres de carpetas y archivos

Guía de referencia para nombrar carpetas y archivos de forma consistente en el proyecto. El proyecto se divide en tres partes: **frontend** (JavaScript + React, HTML, CSS), **backend** (Java + Spring) y **documentos**. Cada parte sigue la convención propia de su lenguaje o ecosistema.

> **Regla de oro:** dentro de cada sección sé 100 % consistente. Mezclar estilos *entre* las tres secciones está bien, porque cada lenguaje tiene su cultura; pero nunca mezcles estilos *dentro* de una misma sección.

---

## Tabla resumen

| Sección | Carpetas | Archivos |
|---|---|---|
| **Frontend** | `kebab-case` | `PascalCase` (componentes) / `camelCase` (JS) / `kebab-case` (CSS, HTML) |
| **Backend (Java)** | minúsculas (todo junto) | `PascalCase` (= nombre de la clase) |
| **Documentos** | `kebab-case` | `kebab-case` |

---

## Recordatorio de estilos

Antes de entrar en cada sección, estos son los estilos de nombrado que se mencionan:

- **camelCase** → primera palabra en minúscula, las siguientes con mayúscula inicial: `formatDate`, `userProfile`.
- **PascalCase** → todas las palabras con mayúscula inicial: `FormatDate`, `UserProfile`.
- **snake_case** → todo en minúscula separado por guion bajo: `format_date`, `user_profile`.
- **kebab-case** → todo en minúscula separado por guion: `format-date`, `user-profile`.

---

## 1. Frontend (React, JS, HTML, CSS)

### Carpetas → `kebab-case`

Todo en minúsculas y separado por guiones. Es la opción más segura porque evita problemas de mayúsculas/minúsculas entre sistemas operativos (Windows no distingue mayúsculas, pero Linux sí, y eso provoca errores difíciles de detectar en producción).

```
src/
  components/
  user-profile/
  shared-hooks/
  auth/
  api-services/
```

### Archivos → depende del tipo

**Componentes de React → `PascalCase`**

Esta es la convención casi universal en React. Permite distinguir de un vistazo qué archivo es un componente.

```
UserCard.jsx
LoginForm.jsx
Navbar.jsx
ProductList.jsx
```

**Hooks, utilidades y servicios JS → `camelCase`**

Los hooks personalizados siempre empiezan con `use`.

```
useAuth.js
useFetchData.js
apiClient.js
formatDate.js
validators.js
```

**CSS**

- Con **CSS Modules**, haz que el archivo coincida con el componente: `UserCard.module.css`.
- Con **CSS normal**, usa `kebab-case`: `user-card.css`, `global-styles.css`.

**HTML → `kebab-case`** en minúsculas: `index.html`.

### Ejemplo de estructura completa (frontend)

```
src/
  components/
    Navbar.jsx
    Navbar.module.css
  user-profile/
    UserProfile.jsx
    UserProfile.module.css
    userProfileApi.js
  hooks/
    useAuth.js
  utils/
    formatDate.js
    validators.js
  styles/
    global-styles.css
index.html
```

---

## 2. Backend (Java + Spring)

En Java las reglas son más estrictas: no son solo preferencias, sino convenciones oficiales (y en el caso de los archivos, una exigencia del compilador).

### Carpetas (paquetes) → minúsculas, todo junto

Los nombres de paquetes van **siempre en minúsculas**, aunque tengan varias palabras. **No se usa camelCase, snake_case ni guiones.** Cuando hay varias palabras, lo recomendado es pegarlas todas juntas en minúsculas.

```
✅ com.tuempresa.proyecto.userprofile
✅ com.tuempresa.proyecto.ordermanagement

❌ com.tuempresa.proyecto.userProfile    (camelCase, no)
❌ com.tuempresa.proyecto.user_profile   (snake_case, no)
❌ com.tuempresa.proyecto.user-profile   (los guiones ni siquiera son válidos)
```

Como `userprofile` todo junto cuesta leerlo, en la práctica muchos equipos prefieren **mantener los nombres de paquetes cortos, de una sola palabra**, para evitar el problema. Así el árbol queda limpio:

```
com.tuempresa.proyecto
  ├── controller
  ├── service
  ├── repository
  ├── model
  ├── dto
  └── config
```

> El guion bajo (`_`) solo se permite en casos muy puntuales (por ejemplo, cuando una palabra coincide con una palabra reservada de Java). En el día a día no lo vas a necesitar; evítalo.

### Archivos → `PascalCase` (= nombre de la clase)

El nombre del archivo debe coincidir **obligatoriamente** con el nombre de la clase pública que contiene. Esto no es opcional: el compilador lo exige.

```
UserController.java        → class UserController
UserService.java           → class UserService
UserRepository.java        → interface UserRepository
UserDto.java               → class UserDto
SecurityConfig.java        → class SecurityConfig
```

### Ejemplo de estructura completa (backend)

```
src/main/java/com/tuempresa/proyecto/
  controller/
    UserController.java
    OrderController.java
  service/
    UserService.java
    OrderService.java
  repository/
    UserRepository.java
  model/
    User.java
    Order.java
  dto/
    UserDto.java
  config/
    SecurityConfig.java
```

---

## 3. Carpeta de documentos

### Carpetas → `kebab-case`

```
documentos/
  guias/
  api-reference/
  diagramas/
  actas-reunion/
```

### Archivos → `kebab-case`

Todo en minúsculas, sin espacios ni mayúsculas. Evitar espacios y mayúsculas previene problemas al compartir entre sistemas operativos y al enlazar archivos.

```
setup-guide.md
arquitectura.md
api-reference.md
guia-despliegue.md
```

**Versiones o fechas:** si manejas documentos con fecha, un prefijo en formato ISO (`AAAA-MM-DD`) funciona muy bien porque los archivos se ordenan solos cronológicamente.

```
2026-06-05-acta-reunion.md
2026-06-12-acta-reunion.md
```

### Ejemplo de estructura completa (documentos)

```
documentos/
  guias/
    setup-guide.md
    guia-despliegue.md
  api-reference/
    endpoints.md
    autenticacion.md
  diagramas/
    arquitectura.md
    flujo-datos.md
  actas-reunion/
    2026-06-05-acta-reunion.md
```

---

## Resumen de buenas prácticas

1. **Consistencia por sección:** respeta la convención de cada lenguaje y no la rompas dentro de su carpeta.
2. **Minúsculas en carpetas siempre que el lenguaje lo permita:** evita errores entre Windows y Linux.
3. **Sin espacios en ningún nombre:** usa guiones o el estilo que corresponda.
4. **En React, PascalCase = componente:** ayuda a leer la estructura de un vistazo.
5. **En Java, el archivo se llama igual que la clase:** es obligatorio.
6. **Nombres descriptivos pero concisos:** que el nombre explique qué contiene sin volverse interminable.
