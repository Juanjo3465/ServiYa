# Fix B15: JWT Roles Not Revalidated Per Request

## Bug Description
**Severity**: BAJO (LOW)  
**Module**: security  
**Location**: `JwtAuthenticationFilter`

**Problem**: Roles embedded in the JWT token were not revalidated against the database on each request. When an admin revoked a user's role (e.g., removed ADMIN role), the user retained that role until their JWT token expired.

## Root Cause
In `JwtAuthenticationFilter.isActiveUser()`, the filter only checked if the user was active (not banned, not deleted) by calling `userReadPort.findById(userId).map(User::isActive)`. It did **not** fetch the current roles from the database and update the `Authentication` object with those fresh roles.

The roles were taken directly from the JWT token claims in `JwtService.resolve()` (lines 103-109), which meant role changes only took effect after token expiration.

## Files Modified
1. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/security/JwtAuthenticationFilter.java`
2. `backend/serviya/src/test/java/com/parosurvivors/serviya/users/infrastructure/security/JwtAuthenticationFilterTest.java`

## Fix Implementation
Modified `JwtAuthenticationFilter` to:
1. Renamed `isActiveUser()` to `revalidateRoles()` to better reflect its new responsibility
2. After verifying the user is active, fetch the current roles from the database (`user.getRoles()`)
3. Build a new `UsernamePasswordAuthenticationToken` with the fresh roles as authorities
4. Update the `SecurityContext` with the new `Authentication` containing current roles

### Key Changes in `JwtAuthenticationFilter.java`:
- **Before**: Only checked `User::isActive`, kept JWT roles
- **After**: Checks `User::isActive` AND rebuilds authorities from `user.getRoles()`

```java
private Authentication revalidateRoles(Authentication authentication) {
    // ... extract userId ...
    return userReadPort.findById(userId)
            .filter(User::isActive)
            .map(user -> buildAuthenticationWithFreshRoles(user, authentication))
            .orElse(null);
}

private Authentication buildAuthenticationWithFreshRoles(User user, Authentication originalAuth) {
    List<SimpleGrantedAuthority> freshAuthorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();
    return new UsernamePasswordAuthenticationToken(user.getId(), null, freshAuthorities);
}
```

## Tests Added
Added 3 new test cases to `JwtAuthenticationFilterTest.java`:
1. `revalidatesRolesFromDatabaseOnEachRequest()` - Verifies roles are refreshed from DB (JWT has ADMIN, DB has CLIENT → resulting auth has CLIENT)
2. `rejectsDeletedUsers()` - Verifies soft-deleted users are rejected
3. `rejectsNonExistentUsers()` - Verifies users not found in DB are rejected

Original test `rejectsBannedUsersBeforeContinuingTheChain()` still passes.

## Verification
- All 364 unit tests pass (excluding 2 pre-existing environment-related failures)
- The fix ensures role revocation takes effect immediately on the next request
- No breaking changes to existing functionality

## Impact
- **Security**: Role revocation is now immediate (no need to wait for JWT expiry)
- **Consistency**: Authentication always reflects current database state
- **Performance**: One additional DB query per authenticated request (already existed for `isActive` check)

## Related Issues
This fix addresses part of the authorization concerns documented in §3.1 of `ESTADO-ACTUAL-Y-PLAN-FUTURO.md`:
- **[MEDIO] Roles del JWT no se revalidan por request** → revocar un rol no surte efecto hasta que expira el token

This is a prerequisite for the full authorization fix (M1 - Capa de autorización centralizada with `@PreAuthorize`).