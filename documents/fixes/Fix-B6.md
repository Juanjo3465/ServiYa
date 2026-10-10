# Fix B6: Secretos con defaults inseguros embebidos (JWT/PII)

## Descripción del Bug

**Severidad:** ALTO  
**Módulo:** config  
**Archivos afectados:** `JwtService`, `PiiAttributeConverter`, `.env.example`

El sistema permitía el uso de claves criptográficas por defecto/inseguras (`JWT_SECRET` y `ENCRYPTION_KEY`) si no se configuraban explícitamente. Esto permitía:
- Firma de tokens JWT con secreto conocido públicamente → tokens falsificables
- Cifrado de datos PII (document_number, phone_number) con passphrase conocida → PII descifrable

## Causa Raíz

1. **`.env.example` contenía valores por defecto inseguros:**
   ```
   JWT_SECRET=dev-serviya-jwt-secret-change-me-in-production
   ENCRYPTION_KEY=dev-serviya-pii-encryption-key-change-me
   ```

2. **Validación insuficiente en el código:** La validación solo verificaba que las variables no fueran `null` o vacías, pero **no rechazaba valores débiles/conocidos** que venían de copiar `.env.example` a `.env` sin modificar.

3. **Historia:** En commits anteriores (antes de `c62c3e0`), el código tenía defaults embebidos directamente en `@Value` y constantes `DEFAULT_PASSPHRASE`. El commit `c62c3e0` añadió fail-fast para valores vacíos, pero no para valores débiles conocidos.

## Solución Implementada

### 1. Actualización de `.env.example`
- Valores vacíos para `JWT_SECRET` y `ENCRYPTION_KEY`
- Comentarios claros indicando que **deben** configurarse valores seguros (≥32 chars aleatorios)
- Advertencia de que la app falla al arrancar si usa valores débiles

### 2. Validación reforzada en `JwtService.java`
```java
private static boolean isWeakJwtSecret(String secret) {
    String lower = secret.toLowerCase();
    return lower.contains("dev-")
            || lower.contains("change-me")
            || lower.contains("example")
            || lower.contains("default")
            || lower.equals("secret")
            || secret.length() < 32;
}
```
Lanza `IllegalStateException` al detectar valores débiles.

### 3. Validación reforzada en `PiiAttributeConverter.java`
```java
private static boolean isWeakEncryptionKey(String passphrase) {
    String lower = passphrase.toLowerCase();
    return lower.contains("dev-")
            || lower.contains("change-me")
            || lower.contains("example")
            || lower.contains("default")
            || passphrase.length() < 32;
}
```
Lanza `IllegalStateException` al detectar valores débiles.

### 4. Actualización de test `ServiyaApplicationTests.java`
- Valores de test actualizados a strings fuertes (≥32 chars, sin patrones débiles)

## Verificación

- ✅ Compilación exitosa
- ✅ 344 tests unitarios/integración pasan (excluyendo `ServiyaApplicationTests` que tiene dependencia H2 pre-existente faltante)
- ✅ La app falla al arrancar con:
  - `JWT_SECRET` vacía o valor débil conocido
  - `ENCRYPTION_KEY` vacía o valor débil conocido
- ✅ La app arranca correctamente con valores seguros (≥32 chars aleatorios)

## Impacto

- **Seguridad:** Elimina la posibilidad de arrancar en producción con claves por defecto
- **Desarrollo:** Fuerza a desarrolladores a generar secretos reales al configurar entorno local
- **Operaciones:** Fail-fast claro en despliegues mal configurados

## Archivos Modificados

1. `.env.example` - Valores vacíos + documentación
2. `backend/serviya/src/main/java/com/parosurvivors/serviya/users/infrastructure/security/JwtService.java` - Validación `isWeakJwtSecret`
3. `backend/serviya/src/main/java/com/parosurvivors/serviya/shared/security/PiiAttributeConverter.java` - Validación `isWeakEncryptionKey`
4. `backend/serviya/src/test/java/com/parosurvivors/serviya/ServiyaApplicationTests.java` - Valores de test fuertes