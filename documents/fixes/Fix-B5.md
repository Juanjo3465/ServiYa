# Fix B5 — Sin guard de auto-acción / último-admin (lockout del panel admin)

**Bug:** B5 (ALTO) — *Sin guard auto-acción/último-admin → lockout*  
**Fuente:** `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` §4 (Catálogo de bugs), columna *Bug*; detalle en §3.1.  
**Rama:** `fix/b5-admin-self-action-guard`.  
**Estado:** corregido y verificado (compilación en verde + 25 tests del módulo `admin` en verde).

---

## Qué era el bug

`AdminService` y `ModerationService` reciben el identificador del administrador que ejecuta la acción
(`adminId`, obtenido siempre del JWT vía `CurrentUser.id()`), pero **lo descartaban sin usarlo**. Eso
permitía:

1. **Auto-acción:** un admin podía banear o eliminar su **propia** cuenta, o retirarse su **propio**
   rol `ADMIN`.
2. **Lockout:** si el objetivo era el **único** administrador del sistema, la acción dejaba el sistema
   **sin ningún admin activo** y sin forma de recuperarlo por API.

Endpoints afectados:

- `POST   /api/v1/admin/users/{id}/ban` → `AdminService.banUser(adminId, userId, reason)`
- `DELETE /api/v1/admin/users/{id}` → `AdminService.deleteUser(adminId, userId)`
- `DELETE /api/v1/admin/users/{id}/roles/{role}` → `AdminService.revokeRoleByAdmin(adminId, userId, role)`
  (solo cuando `role == ADMIN`)
- `POST   /api/v1/reports/{id}/actions/ban` → `ModerationService.banUserFromReport(reportId, adminId, reason)`

El escenario es auto-infligido y de bloqueo total: el `JwtAuthenticationFilter` revalida
`isActive()` contra la BD en **cada** request, así que en el mismo instante en que el admin se banea
o se elimina, su propia sesión queda cortada; y si era el único admin, nadie puede volver a conceder
roles ni desbloquear la cuenta.

## Por qué ocurría (causa raíz)

1. **`AdminService`** — `banUser`, `deleteUser` y `revokeRoleByAdmin` recibían `adminId` pero nunca lo
   comparaban con `userId` ni consultaban si existían otros admins. Delegaban directamente en
   `UserServicePort` / `UserDeletionServicePort` / `UserRoleServicePort`.
2. **`ModerationService`** — `banUserFromReport` resolvía el reporte y baneaba a
   `report.reportedUserId()` sin comprobar que no fuera el propio `adminId` ni que quedara algún admin.
3. **Invariante no implementada:** no existía ninguna guarda que garantizara "el sistema siempre
   conserva al menos un administrador activo" ni que prohibiera acciones destructivas sobre la propia
   cuenta del actor.

**Causa raíz:** ausencia de una política de auto-acción / último-admin en la capa de aplicación. No es
un problema de autorización HTTP (ambos endpoints ya exigen `hasRole("ADMIN")` en `SecurityConfig`),
sino de **regla de negocio** dentro del módulo `admin`.

## Qué se cambió

### 1. Nuevo `AdminActionGuard` (regla centralizada)

`admin/application/services/AdminActionGuard.java` centraliza las dos invariantes, reutilizando puertos
de entrada ya existentes (`UserRoleServicePort`, `UserQueryServicePort`; no toca persistencia ajena):

```java
/** Rechaza que un admin ejecute una accion destructiva sobre su propia cuenta. */
public void requireNotSelf(Long adminId, Long targetUserId) {
    if (adminId != null && adminId.equals(targetUserId)) {
        throw new InvalidStateException(
                "No puedes realizar esta acción sobre tu propia cuenta de administrador");
    }
}

/** Rechaza una accion que dejaria al sistema sin ningun administrador activo. */
public void requireAnotherActiveAdmin(Long targetUserId) {
    if (!userRoleServicePort.hasRole(targetUserId, RoleName.ADMIN.name())) {
        return; // el objetivo no es admin: no puede provocar lockout
    }
    boolean anotherActiveAdmin = userRoleServicePort.findUserIdsByRole(RoleName.ADMIN).stream()
            .filter(id -> !id.equals(targetUserId))
            .anyMatch(this::isActiveUser);
    if (!anotherActiveAdmin) {
        throw new InvalidStateException(
                "La acción dejaría al sistema sin ningún administrador activo");
    }
}
```

Se considera "admin activo" a quien tiene el rol `ADMIN` y **ni está baneado ni eliminado**
(`User.isActive()`), porque una cuenta baneada/eliminada no puede operar el panel.

### 2. `AdminService` — aplicar las guardas antes de la acción

- `banUser(adminId, userId, reason)`: `requireNotSelf` + `requireAnotherActiveAdmin` antes de
  `userServicePort.banUser`.
- `deleteUser(adminId, userId)`: `requireNotSelf` + `requireAnotherActiveAdmin` antes de
  `userDeletionServicePort.deleteUser`.
- `revokeRoleByAdmin(adminId, userId, roleName)`: las guardas aplican **solo si `role == ADMIN`**
  (retirar `CLIENT`/`OFFERER` no puede provocar lockout).

### 3. `ModerationService` — misma guarda al banear desde un reporte

`banUserFromReport` aplica `requireNotSelf(adminId, report.reportedUserId())` +
`requireAnotherActiveAdmin(report.reportedUserId())` antes de banear. Cubre el caso de un admin que se
banea a sí mismo moderando (p. ej. reportándose y resolviendo el reporte).

## Archivos modificados / creados (5)

- `backend/serviya/src/main/java/com/parosurvivors/serviya/admin/application/services/AdminActionGuard.java` **(nuevo)**
- `backend/serviya/src/main/java/com/parosurvivors/serviya/admin/application/services/AdminService.java`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/admin/application/services/ModerationService.java`
- `backend/serviya/src/test/java/com/parosurvivors/serviya/admin/application/services/AdminActionGuardTest.java` **(nuevo)**
- `backend/serviya/src/test/java/com/parosurvivors/serviya/admin/application/services/AdminServiceTest.java` **(nuevo)**
- `backend/serviya/src/test/java/com/parosurvivors/serviya/admin/application/services/ModerationServiceTest.java`

## Verificación

```bash
cd backend/serviya
sh mvnw -o test -Dtest='AdminActionGuardTest,AdminServiceTest,ModerationServiceTest'
```

Resultado:

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Cobertura de los tests nuevos:

- **`AdminActionGuardTest` (7):** auto-acción rechazada; acción sobre otro permitida; `adminId` nulo no
  rompe; guard saltado si el objetivo no es admin; permitido si hay otro admin activo; rechazado si el
  objetivo es el único admin; rechazado si los demás admins están baneados/eliminados.
- **`AdminServiceTest` (5):** `banUser` y `deleteUser` ejecutan las guardas **antes** de la acción;
  `banUser` aborta si la guarda rechaza; `revokeRoleByAdmin("ADMIN")` aplica guardas; retirar un rol no
  admin (`CLIENT`) **no** aplica guardas de admin.
- **`ModerationServiceTest` (13):** se añadieron 2 casos que verifican que `banUserFromReport` usa
  `adminId` y aborta si la guarda rechaza (el resto, de regresión, sigue en verde).

> Nota: `ServiyaApplicationTests.contextLoads` falla en este entorno por falta de MySQL
> (`Unable to determine Dialect without JDBC metadata`); **es preexistente y no está relacionado** con
> este cambio (se verificó ejecutándolo con los cambios en `stash`). El resto de la suite (339 tests)
> pasa.

## Nota sobre el patrón

- La guarda vive en la **capa de aplicación** (regla de negocio), no en `SecurityConfig`: la
  autorización HTTP ya exige `ROLE_ADMIN`; lo que faltaba era la política de auto-acción/lockout.
- Se reutilizan puertos de entrada de `users` (no persistencia ajena), manteniendo la dirección de
  dependencias del módulo `admin` como orquestador.
- `InvalidStateException` → **HTTP 409** (convención del proyecto para violaciones de reglas de
  negocio, igual que `UserRoleService.acquireRole` al impedir la auto-asignación de `ADMIN`).
- Queda pendiente como endurecimiento futuro (backlog **M1/M30**) activar `@EnableMethodSecurity` y
  mapear reglas de negocio a **422**, pero no es necesario para cerrar B5.
