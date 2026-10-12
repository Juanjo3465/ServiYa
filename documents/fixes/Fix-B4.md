# Fix B4 — IDOR autenticado en slots de disponibilidad (delete/activate/deactivate)

**Bug:** B4 (ALTO) — *IDOR autenticado en slots de disponibilidad (delete/activate/deactivate)*  
**Fuente:** `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §4 (Catálogo de bugs), columna *Bug*; detalle en §3.1.  
**Rama:** `fix/b4-availability-idor` (creada desde `origin/main`).  
**Estado:** corregido y verificado (compilación en verde + 21 tests nuevos en verde + suite completa sin regresiones).

---

## Qué era el bug

Cualquier usuario **autenticado** (cliente, otro oferente, admin) podía borrar, activar o desactivar
franjas de disponibilidad **de otro oferente** con solo conocer/adivinar su `id`, sin que el sistema
verificara que la franja le perteneciera.

Endpoints afectados (RF-072, módulo 2 — `profiles`):

- `DELETE /api/v1/offerers/me/availability/slots/{id}` — eliminar franja
- `POST   /api/v1/offerers/me/availability/slots/{id}/activate` — activar franja
- `POST   /api/v1/offerers/me/availability/slots/{id}/deactivate` — desactivar franja

Es un **IDOR** (Insecure Direct Object Reference) de tipo *autenticado*: la app confía en el `{id}`
del path sin comprobar que la franja pertenezca al peticionario. A diferencia de B1–B3, **sí** exige
login (luego no es anónimo), por eso en el catálogo es severidad **ALTO** y no CRÍTICO.

## Por qué ocurría (causa raíz)

1. **`OffererAvailabilityController.java`** — `deleteSlot`, `activateSlot` y `deactivateSlot`
   recibían únicamente el `{id}` del path y lo pasaban tal cual al servicio. **Nunca** se usaba
   `currentUserId()` ni `CurrentUser.isAdmin()` (métodos que el *mismo* controller sí usa en
   `getSchedule`/`setSchedule`). La identidad de quien llama nunca llegaba a la capa de aplicación.
2. **`OffererAvailabilityServicePort.java`** — las firmas `deleteSlot/activateSlot/deactivateSlot(Long slotId)`
   no transportaban la identidad del actor: el servicio no tenía **cómo** comprobar la propiedad.
3. **`OffererAvailabilityService.java`** — el núcleo del bug:
   - `deleteSlot` ejecutaba `deleteById(slotId)` **a ciegas** (ni siquiera cargaba la fila → era
     imposible conocer al dueño).
   - `activateSlot`/`deactivateSlot` cargaban el slot y lo mutaban **sin comparar**
     `slot.getOffererId()` con el peticionario.
   - La entidad `OffererAvailability` **ya tenía** el dato necesario (`offererId`); sencillamente nunca
     se usaba para autorizar.
4. **`SecurityConfig.java`** — `/api/v1/offerers/me/**` ya exige `authenticated()`
   (`.requestMatchers("/api/v1/offerers/me/**").authenticated()`), por lo que **no hizo falta ningún
   cambio aquí**: la carencia era 100 % autorización de grano fino en la capa de aplicación.

**Causa raíz:** ausencia de *check de dueño* en el servicio y de propagación de la identidad JWT desde
el controller. Es exactamente el patrón de la clase IDOR que el documento recomienda cerrar con **M1**
(capa de autorización centralizada / `OwnershipChecker`).

## Qué se cambió

### 1. `OffererAvailabilityServicePort.java` — la identidad viaja en la firma

```java
void deleteSlot(Long slotId, Long requesterId, boolean isAdmin);
void activateSlot(Long slotId, Long requesterId, boolean isAdmin);
void deactivateSlot(Long slotId, Long requesterId, boolean isAdmin);
```

### 2. `OffererAvailabilityService.java` — check de dueño

- **`deleteSlot`** ahora **carga** el slot primero, verifica propiedad y recién entonces borra.
- **`activateSlot`/`deactivateSlot`** verifican propiedad antes de mutar.
- Helper + helper de búsqueda, siguiendo el patrón *"propietario O admin"* ya usado en
  `AddressService.requireOwnership` (fix B2) y `MarketplaceService.requireOwnership`:

```java
private OffererAvailability requireSlot(Long slotId) {
    return offererAvailabilityPersistencePort.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Franja de disponibilidad no encontrada con id: " + slotId));
}

/**
 * El actor debe ser el dueño de la franja, o un admin (IDOR: B4).
 * Mismo patron "propietario O admin" que {@code AddressService.requireOwnership}.
 */
private void requireOwnership(OffererAvailability slot, Long requesterId, boolean isAdmin) {
    if (isAdmin) {
        return;
    }
    if (requesterId == null || !requesterId.equals(slot.getOffererId())) {
        throw new UnauthorizedException("El usuario no es el propietario de la franja horaria");
    }
}
```

Caso especial no-dueño → `UnauthorizedException` → **HTTP 401** (convención del proyecto, igual que
B2/B5). El `isAdmin` admite la gestión administrativa futura sin debilitar el principio de menor
privilegio por defecto.

### 3. `OffererAvailabilityController.java` — identidad desde el JWT

```java
offererAvailabilityService.deleteSlot(id, currentUserId(), CurrentUser.isAdmin());
offererAvailabilityService.activateSlot(id, currentUserId(), CurrentUser.isAdmin());
offererAvailabilityService.deactivateSlot(id, currentUserId(), CurrentUser.isAdmin());
```

La identidad sale SIEMPRE del token JWT (`CurrentUser.id()` / `CurrentUser.isAdmin()`), nunca del body
ni del path.

### 4. `SecurityConfig.java` — sin cambios

Los endpoints ya caían bajo `/api/v1/offerers/me/**` → `authenticated()`, antes de
`.anyRequest().permitAll()`. El IDOR no era un problema de autenticación sino de ausencia de chequeo de
propiedad; se corrige íntegro en la capa de aplicación.

### 5. Efecto secundario positivo: not-found coherente

Antes, un slot inexistente en `activate/deactivate` lanzaba `IllegalArgumentException` → **500**. Ahora
`requireSlot` lanza `ResourceNotFoundException` → **404** (consistente con el fix B2 y con la guía
**M10** del documento). `deleteSlot` también devuelve 404 (antes `deleteById` a ciegas). El frontend no
está afectado: estas operaciones no estaban expuestas en `api.js` (§5 del documento).

## Archivos modificados / creados (6)

- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/application/ports/input/OffererAvailabilityServicePort.java`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/application/services/OffererAvailabilityService.java`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/adapters/input/OffererAvailabilityController.java`
- `backend/serviya/src/test/java/com/parosurvivors/serviya/profiles/application/services/OffererAvailabilityServiceTest.java` **(nuevo)**
- `backend/serviya/src/test/java/com/parosurvivors/serviya/profiles/infrastructure/adapters/input/OffererAvailabilityControllerIntegrationTest.java` **(nuevo)**
- `documents/fixes/Fix-B4.md` **(este documento)**

## Verificación

```bash
cd backend/serviya
./mvnw -o test -Dtest='OffererAvailabilityServiceTest,OffererAvailabilityControllerIntegrationTest'
```

Resultado (tests nuevos):

```
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **`OffererAvailabilityServiceTest` (14):** dueño puede borrar/activar/desactivar; no-dueño → 401 y
  **no** se invoca la persistencia; admin puede operar sobre franjas ajenas; `requesterId` nulo → 401;
  slot inexistente → 404; el flag `active` se voltea correctamente.
- **`OffererAvailabilityControllerIntegrationTest` (7, `@WebMvcTest`):** el controller pasa
  `currentUserId()` y `CurrentUser.isAdmin()` del JWT al servicio (el wiring que era el bug); admin
  propaga `isAdmin = true`; un no-dueño (simulado lanzando `UnauthorizedException`) recibe **401** de
  verdad por HTTP para los tres endpoints.

Sobre la **suite completa**: `./mvnw -o test` → **346 tests en verde, 0 fallos**, y una sola
excepción preexistente: `ServiyaApplicationTests.contextLoads` falla por falta de MySQL en el entorno
(`Unable to determine Dialect without JDBC metadata`). Se verificó que **es preexistente y ajeno a este
cambio**: ejecutándolo con los cambios del fix en `stash`, `main` falla con exactamente el mismo error.
(Idéntica nota figura ya en `Fix-B5.md`.)

## Nota sobre el patrón

- **Defensa en profundidad:** el `SecurityConfig` exige login al área `/offerers/me/**`; el check de
  propiedad (grano fino) vive en la capa de aplicación, igual que en B2 (`AddressService`).
- **Convención:** `UnauthorizedException` → 401 para "no propietario"; `ResourceNotFoundException` →
  404 para "no existe"; mismo idioma y mensajes en español que el resto del módulo.
- Queda pendiente como endurecimiento futuro (backlog **M1**): activar `@EnableMethodSecurity` +
  `@PreAuthorize`/`OwnershipChecker` centralizado para cerrar la clase completa de bugs IDOR (B1–B4,
  B14) en vez de parchear endpoint por endpoint.