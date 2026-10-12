# Fix B1 — IDOR en escrituras de servicios

**Bug:** B1 (CRÍTICO) — *Escrituras IDOR sin autenticar (update/delete/activate/deactivate/soft-delete)*
**Fuente:** `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §4 (Catálogo de bugs), columna *Bug*.
**Commit:** `28cb9c5` (rama `main`).
**Estado:** corregido y verificado (325 tests de unidad en verde).

---

## Qué era el bug

Cualquier persona (incluso **sin estar autenticada**) podía hacer estas operaciones sobre un servicio
que no fuera de ella y mutarlo, con solo conocer/adivinar su `id`:

- `PATCH /api/v1/services/{id}` — editar
- `DELETE /api/v1/services/{id}` — borrar
- `PATCH /api/v1/services/{id}/soft-delete`
- `POST /api/v1/services/{id}/activate`
- `POST /api/v1/services/{id}/deactivate`

Es un **IDOR** (Insecure Direct Object Reference): la app confiaba en el `{id}` enviado por el cliente
sin verificar que el recurso perteneciera al peticionario.

## Por qué ocurría (causa raíz)

No había ningún chequeo de dueño en toda la ruta de escritura. Tres causas que se sumaban:

1. **`SecurityConfig`** terminaba con `.anyRequest().permitAll()` y ninguna regla cubría esos endpoints:
   las escrituras quedaban **abiertas incluso sin login**.
2. **`ServiceController`** tomaba el `{id}` del path y lo mandaba directo al servicio; `CurrentUser.id()`
   solo se usaba en `create`, nunca en los métodos de escritura por id.
3. **`MarketplaceService`** cargaba la entidad por `id`, la mutaba y guardaba, sin comparar nunca el
   `offererId` del servicio con el usuario autenticado.

**Causa raíz:** la app confiaba ciegamente en el `{id}` del request — no existía el *check de dueño*.

## Qué se cambió

### 1. `SecurityConfig.java` — exigir login en las escrituras (defensa en profundidad)

Se añadieron estas reglas antes de `.anyRequest().permitAll()`:

- `POST /api/v1/services` → `authenticated()`
- `PATCH /api/v1/services/*/soft-delete` → `authenticated()`
- `PATCH /api/v1/services/*` → `authenticated()`
- `DELETE /api/v1/services/*` → `authenticated()`
- `POST /api/v1/services/*/activate` y `.../deactivate` → `authenticated()`

Con esto, un request anónimo recibe **401** en el filtro, antes de tocar la lógica de negocio.

### 2. `MarketplaceService.java` — el chequeo de dueño (el fix real)

Los métodos de escritura ahora reciben `requesterId` + `isAdmin` y validan propiedad antes de mutar:

```java
private void requireOwnership(Service service, Long requesterId, boolean isAdmin) {
    if (isAdmin) {
        return;
    }
    if (requesterId == null || !requesterId.equals(service.getOffererId())) {
        throw new UnauthorizedException("El usuario no es el propietario del servicio");
    }
}
```

Se aplicó en `update`, `delete`, `softDelete`, `activate` y `deactivate`.
Patrón **"actorId + isAdmin"**, el mismo que ya usaba `ServiceRequestQueryService.getRequestHistory`.

### 3. `ServiceController.java` — identidad desde el JWT

Todas las llamadas de escritura ahora pasan `CurrentUser.id(), CurrentUser.isAdmin()`.
La identidad SIEMPRE sale del token JWT, nunca del body ni del path.

### 4. Path admin preservado (no se rompió)

El admin sí puede borrar cualquier servicio (moderación) vía `DELETE /api/v1/admin/services/{id}`.
Para no romperlo con el nuevo chequeo de dueño, se propaga la identidad admin:

- `AdminController.deleteService` → `adminService.deleteService(id, CurrentUser.id(), CurrentUser.isAdmin())`
- `AdminService.deleteService(serviceId, adminId, isAdmin)` → `marketplaceServicePort.delete(...)`
- `AdminServicePort.deleteService` misma firma.

### 5. `MarketplaceServiceTest.java` — tests del fix

Se actualizaron las firmas de los tests y se añadieron tests nuevos:

- `updateThrowsWhenNotOwner` / `deleteThrowsWhenNotOwner` / `softDeleteThrowsWhenNotOwner` /
  `activateThrowsWhenNotOwner` / `deactivateThrowsWhenNotOwner` → un usuario ajeno recibe
  `UnauthorizedException` y la persistencia **nunca** se toca.
- `deleteAllowsAdmin` → un admin sí puede borrar el servicio de otro.

## Archivos modificados (8)

- `backend/.../config/SecurityConfig.java`
- `backend/.../services/application/ports/input/MarketplaceServicePort.java`
- `backend/.../services/application/services/MarketplaceService.java`
- `backend/.../services/infrastructure/adapters/input/ServiceController.java`
- `backend/.../admin/application/ports/input/AdminServicePort.java`
- `backend/.../admin/application/services/AdminService.java`
- `backend/.../admin/infrastructure/adapters/input/AdminController.java`
- `backend/.../test/.../services/application/services/MarketplaceServiceTest.java`

## Verificación

```bash
cd backend/serviya
./mvnw test
```

Resultado: **325 tests, 0 fallos, 0 errores** (excepto `ServiyaApplicationTests.contextLoads`, que
falla por entorno: necesita MySQL activo vía `${DB_URL}`/`${DB_USER}`/`${DB_PASSWORD}` — no está
relacionado con este fix).

## Nota de patrón usado

- Excepción de no-propietario: `UnauthorizedException` (→ 401). Es la convención que ya usaba
  `ReportController.assertSelfOrAdmin` y los comandos de requests. Semánticamente un no-dueño
  autenticado sería 403, pero se mantuvo la convención del código para no introducir un error
  nuevo sin migrar el resto.
- Los 5 métodos de escritura cambiaron de firma a `(…, Long requesterId, boolean isAdmin)` siguiendo
  el patrón ya presente en el código (`getRequestHistory(id, CurrentUser.id(), CurrentUser.isAdmin())`).