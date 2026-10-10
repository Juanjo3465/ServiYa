# Fix B14 — IDOR leve en `markAsRead` (Notifications)

## Resumen del bug
- **ID**: B14
- **Severidad**: BAJO
- **Módulo**: notifications
- **Endpoint afectado**: `POST /api/v1/notifications/{id}/read`

## Descripción
El endpoint `markAsRead` permitía a cualquier usuario autenticado marcar como leída cualquier notificación (delivery) solo conociendo su ID, sin verificar que la notificación perteneciera al usuario actual. Esto es un **IDOR (Insecure Direct Object Reference)** de severidad baja porque:
- Requiere autenticación (el endpoint no está en `permitAll`)
- Solo permite marcar como leída (acción de bajo impacto)
- No expone datos sensibles ni permite modificar datos críticos

## Causa raíz
En `NotificationController.markAsRead()` (línea 47-51), el código recibía el `deliveryId` del path variable y llamaba directamente a `notificationDeliveryService.markAsRead(id)` sin pasar el `userId` del usuario autenticado.

En `NotificationDeliveryService.markAsRead()` (línea 238-243), el servicio buscaba el delivery solo por ID y lo marcaba como leído, sin verificar que el delivery perteneciera a una notificación del usuario actual.

## Archivos modificados

### 1. `NotificationDeliveryPersistencePort.java` (puerto de salida)
- Agregado método `findByIdAndUserId(Long id, Long userId)` para buscar un delivery verificando que pertenece al usuario.

### 2. `NotificationDeliveryRepository.java` (repositorio JPA)
- Agregado método `@Query` personalizado `findByIdAndUserId` que hace JOIN con la tabla `notifications` para filtrar por `userId`.

### 3. `NotificationDeliveryPersistenceAdapter.java` (adaptador de persistencia)
- Implementado el nuevo método `findByIdAndUserId` delegando al repositorio.

### 4. `NotificationDeliveryServicePort.java` (puerto de entrada)
- Cambiada la firma de `markAsRead(Long deliveryId)` a `markAsRead(Long deliveryId, Long userId)`.

### 5. `NotificationDeliveryService.java` (servicio de aplicación)
- Actualizado `markAsRead` para usar `findByIdAndUserId` y lanzar `ResourceNotFoundException` si el delivery no existe o no pertenece al usuario.

### 6. `NotificationController.java` (controlador REST)
- Actualizado `markAsRead` para pasar `currentUserId()` al servicio.

## Cómo probar
1. Iniciar la aplicación backend
2. Autenticarse como usuario A
3. Crear una notificación para el usuario B (o usar una existente)
4. Intentar llamar `POST /api/v1/notifications/{id_del_usuario_B}/read` con el token del usuario A
5. **Resultado esperado**: 404 Not Found (ResourceNotFoundException)
6. Llamar el mismo endpoint con el token del usuario B (dueño de la notificación)
7. **Resultado esperado**: 204 No Content

## Notas
- Este fix es consistente con el patrón usado en `getDeliveries` que ya filtra por `userId` usando `findDeliveriesByUserId`.
- La severidad BAJO se debe a que el impacto es limitado (solo marcar como leída), pero el fix cierra la clase de vulnerabilidad IDOR en el módulo notifications.
- El fix también protege contra el caso donde un usuario intente marcar como leída una notificación que no existe (antes también lanzaba 404, ahora es consistente).