# Fix B2 — IDOR en escrituras de direcciones

**Bug:** B2 (CRÍTICO) — *IDOR sin autenticar en `PATCH/DELETE /addresses/{id}`*  
**Fuente:** `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §4 (Catálogo de bugs), columna *Bug*.  
**Commit:** `a2e1d11` (rama `fix/b2-address-idor`, creada desde `main`; commit con únicamente los 4 archivos del fix).  
**Estado:** corregido y verificado.

---

## Qué era el bug

Cualquier persona (incluso **sin estar autenticada**) podía actualizar o borrar una dirección que no le perteneciera con solo conocer/adivinar su `id`:

- `PATCH /api/v1/addresses/{id}` — editar dirección
- `DELETE /api/v1/addresses/{id}` — eliminar dirección

Es un **IDOR** (Insecure Direct Object Reference): la app confiaba en el `{id}` enviado sin verificar que el recurso perteneciera al peticionario.

## Por qué ocurría (causa raíz)

1. **`SecurityConfig.java`** — Las reglas de autorización no cubrían esos endpoints. Solo `/api/v1/users/me/**` estaba protegido; `PATCH/DELETE /api/v1/addresses/{id}` no aparecían en ninguna regla y caían en **`.anyRequest().permitAll()`**, quedando **abiertos sin login**.
2. **`AddressController.java`** — Para `updateAddress` y `deleteAddress` nunca se usaba `currentUserId()`. Se pasaba solo el `{id}` al servicio.
3. **`AddressService.java`** — `updateAddress` y `deleteAddress` solo comprobaban que la dirección **existiera** (`findById`), nunca que `address.userId` coincidiera con el usuario autenticado.

**Causa raíz:** falta de autenticación en la capa de seguridad + ausencia de *check de dueño* en la capa de aplicación.

## Qué se cambió

### 1. `SecurityConfig.java` — exigir login en las escrituras (defensa en profundidad)

Se añadieron reglas antes de `.anyRequest().permitAll()`:

- `PATCH /api/v1/addresses/*` → `authenticated()`
- `DELETE /api/v1/addresses/*` → `authenticated()`

Con esto, una petición anónima recibe **401** en el filtro, antes de llegar a la lógica de negocio.

### 2. `AddressServicePort.java` — pasar identidad al servicio

Las firmas cambiaron para forzar la verificación de propiedad:

```java
void deleteAddress(Long addressId, Long requesterId, boolean isAdmin);
Address updateAddress(UpdateAddressCommand command, Long requesterId, boolean isAdmin);
```

### 3. `AddressService.java` — check de dueño

Se añadió el helper siguiendo el patrón usado en `MarketplaceService`:

```java
/**
 * El actor debe ser el dueño de la direccion, o un admin (IDOR: B2).
 * Mismo patron "propietario O admin" que {@code MarketplaceService.requireOwnership}.
 */
private void requireOwnership(Address address, Long requesterId, boolean isAdmin) {
    if (isAdmin) {
        return;
    }
    if (requesterId == null || !requesterId.equals(address.getUserId())) {
        throw new UnauthorizedException("El usuario no es el propietario de la dirección");
    }
}
```

Aplicado a `updateAddress` y `deleteAddress`. Si el requester no es dueño ni admin, se lanza `UnauthorizedException` (mismo convenio que el resto del código).

### 4. `AddressController.java` — obtener identidad desde el JWT

```java
addressService.updateAddress(mapper.toCommand(form, id), currentUserId(), CurrentUser.isAdmin());
addressService.deleteAddress(id, currentUserId(), CurrentUser.isAdmin());
```

La identidad sale SIEMPRE del token JWT (`CurrentUser.id()` / `CurrentUser.isAdmin()`), nunca del body ni del path.

## Archivos modificados (4)

- `backend/serviya/src/main/java/com/parosurvivors/serviya/config/SecurityConfig.java`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/application/ports/input/AddressServicePort.java`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/application/services/AddressService.java`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/adapters/input/AddressController.java`

## Verificación

```bash
cd backend/serviya
chmod +x mvnw
./mvnw compile
./mvnw test-compile
```

Ambas compilaciones pasan correctamente. El commit `a2e1d11` contiene únicamente los 4 archivos modificados para este fix.

## Nota sobre el patrón

- Defensa en profundidad: seguridad por regla HTTP (`SecurityConfig`) + validación de propiedad en capa de aplicación (`AddressService`).
- Convención: uso de `UnauthorizedException` para "no propietario/no autorizado" siguiendo el patrón existente en el proyecto (coherente con `MarketplaceService`, `ServiceRequest*`, etc.).
- Propagación de `isAdmin` para permitir acciones administrativas futuras sin romper el principio de menor privilegio por defecto.