# Fix B3 — `POST /categories` sin autenticar (debe ser admin)

**Bug:** B3 (CRÍTICO) — *`POST /categories` sin autenticar (debe ser admin)*  
**Fuente:** `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §4 (Catálogo de bugs), columna *Bug*.  
**Commit:** el fix de código llegó a `main` dentro del commit `a2e1d11` (fix B2), mergeado vía PR #38.  
**Estado:** corregido y verificado (compilación en verde) en `origin/main`.

---

## Qué era el bug

Cualquier persona, **incluso sin estar autenticada**, podía crear una categoría en el marketplace con
solo hacer:

- `POST /api/v1/categories` — crear categoría

Crear categorías es una operación **solo-admin** (define el catálogo del marketplace); permitirla de
forma anónima permitía a cualquiera contaminar/inundar el catálogo sin ningún control.

## Por qué ocurría (causa raíz)

1. **`SecurityConfig.java`** — Las reglas de autorización no cubrían `POST /api/v1/categories`. El
   endpoint no aparecía en ninguna regla explícita y caía en **`.anyRequest().permitAll()`**, quedando
   **abierto sin login** (y sin rol).
2. **`CategoryController.java`** — `create()` no tenía ninguna anotación de seguridad ni chequeo de
   rol; simplemente delegaba en `marketplaceCategory.create(...)`.
3. **Sin `@EnableMethodSecurity` ni `@PreAuthorize`** en todo el backend — las URL-matchers de
   `SecurityConfig` son la única barrera. Al no existir regla para este endpoint, quedaba abierto.

**Causa raíz:** falta de regla de autorización en la capa de seguridad HTTP. Al ser una operación
global (las categorías no tienen "dueño"), el fix correcto es un **gate por rol** (`ADMIN`), no un
check de propiedad.

## Qué se cambió

### 1. `SecurityConfig.java` — exigir rol `ADMIN` en la escritura (defensa en profundidad)

Se añadió esta regla **antes** de `.anyRequest().permitAll()`:

```java
// Escritura de una categoria (modulo 3): solo ADMIN. Las lecturas (GET) siguen publicas.
.requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
```

Efecto:

- `POST /api/v1/categories` → requiere rol **ADMIN**. Anónimo → **401**; autenticado sin rol → **403**.
  El request se rechaza en el filtro, antes de tocar la lógica de negocio.
- `GET /api/v1/categories` y `GET /api/v1/categories/{id}` → **siguen públicos** (visibilidad del
  catálogo para cualquier visitante), porque no hay ninguna regla GET previa que los capture y caen en
  `anyRequest().permitAll()`.

### 2. Regla evaluada en el orden correcto

`POST /api/v1/categories` no coincide con ninguna regla anterior (auth, swagger, `offerers`,
`users/me`, `admin/**`, `reports`, `services`), por lo que la nueva regla es la primera que la captura.
Verificado: el match cae en `hasRole("ADMIN")` y no en `permitAll()`.

### 3. Compatibilidad con el mapeo de authorities del JWT (verificado)

`JwtService.resolve()` construye las authorities como `ROLE_<NAME>` (p. ej. `ROLE_ADMIN`), y
`hasRole("ADMIN")` compara contra esa misma forma → el gate funciona con los tokens existentes sin
cambios en la emisión.

## Archivos modificados (1)

- `backend/serviya/src/main/java/com/parosurvivors/serviya/config/SecurityConfig.java`

## Verificación

```bash
cd backend/serviya
sh mvnw compile        # (mvnw perdió el bit de ejecución; usar sh mvnw o chmod +x mvnw)
```

Resultado: **BUILD SUCCESS**. Como el fix viajó accidentalmente dentro del commit B2 (`a2e1d11`,
PR #38), ya está en `origin/main`; no hizo falta ningún commit adicional de código.

## Nota sobre el patrón

- Defensa en profundidad: regla HTTP por rol (`SecurityConfig`) como primera barrera. Al ser una
  operación de catálogo global (sin dueño), el gate por rol es el fix completo a este nivel.
- Endurecimiento opcional (backlog M1): activar `@EnableMethodSecurity` y anotar
  `CategoryController.create` con `@PreAuthorize("hasRole('ADMIN')")` para que el gate viva también en
  la capa de aplicación; no es estrictamente necesario para cerrar B3, pero elimina la dependencia de
  que toda regla futura exista en `SecurityConfig`.