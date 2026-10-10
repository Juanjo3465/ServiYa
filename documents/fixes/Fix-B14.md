# Fix B14: IDOR leve en `markAsRead` endpoint de notificaciones

## Resumen del Bug

**Severidad:** BAJO (LOW)
**Módulo:** notifications
**Endpoint afectado:** `POST /api/v1/notifications/{id}/read`

## Causa Raíz

El endpoint `markAsRead` en `NotificationController` no estaba protegido explícitamente en la configuración de seguridad (`SecurityConfig`). Aunque el controlador usa `CurrentUser.id()` que lanza `UnauthorizedException` para usuarios no autenticados, la regla de seguridad por defecto `.anyRequest().permitAll()` permitía que las peticiones no autenticadas llegaran al controlador, resultando en un error 401 en lugar de una denegación de acceso apropiada (401/403) a nivel de filtro de seguridad.

Además, el endpoint `GET /api/v1/notifications` (listar notificaciones) y `GET /api/v1/notification-channels` tampoco tenían protección explícita.

## Archivos Relacionados

1. **`backend/serviya/src/main/java/com/parosurvivors/serviya/config/SecurityConfig.java`** - Configuración de seguridad donde se debía agregar la protección
2. **`backend/serviya/src/main/java/com/parosurvivors/serviya/notifications/infrastructure/adapters/input/NotificationController.java`** - Controlador afectado
3. **`backend/serviya/src/main/java/com/parosurvivors/serviya/notifications/application/services/NotificationDeliveryService.java`** - Servicio que ya validaba propiedad vía `findByIdAndUserId`

## Solución Implementada

Se agregaron reglas explícitas en `SecurityConfig.java` para requerir autenticación en los endpoints de notificaciones:

```java
// Notificaciones del usuario (modulo 8): listar y marcar como leida requieren autenticacion.
// Los canales pueden ser publicos, pero la API declara bearerAuth.
.requestMatchers("/api/v1/notifications/**").authenticated()
.requestMatchers("/api/v1/notification-channels").authenticated()
```

Estas reglas se colocan **antes** de la regla `.anyRequest().permitAll()` para asegurar que tomen precedencia.

## Verificación

1. **Compilación exitosa**: `./mvnw compile` pasa sin errores
2. **Tests unitarios**: `NotificationDeliveryServiceTest` pasa correctamente
3. **Coherencia**: La interfaz `NotificationApi` ya declaraba `@SecurityRequirement(name = "bearerAuth")`, confirmando la intención de diseño

## Impacto

- Los endpoints de notificaciones ahora rechazan peticiones no autenticadas a nivel de filtro de seguridad (401) antes de llegar al controlador
- El comportamiento es consistente con otros endpoints similares como `/api/v1/users/me/**` y `/api/v1/offerers/me/**`
- Se elimina la clase de bug IDOR de bajo nivel en notificaciones

## Notas Adicionales

Este fix es parte de la mejora transversal **M1 — Capa de autorización centralizada** documentada en `ESTADO-ACTUAL-Y-PLAN-FUTURO.md`, que busca eliminar de raíz la clase de bugs IDOR (B1–B4, B14) mediante `@EnableMethodSecurity` + `@PreAuthorize` y verificación de propiedad uniforme.