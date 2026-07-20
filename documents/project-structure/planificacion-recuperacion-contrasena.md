# Planificación: sistema de recuperación de contraseña con link

## 1. Objetivo

Diseñar un flujo de recuperación de contraseña por medio de un enlace de un solo uso enviado al correo del usuario. Como el usuario olvidó su contraseña, no se le puede autenticar por ese medio; en su lugar se usa un canal que ya sabemos que le pertenece (su correo) para entregarle una prueba de posesión temporal —un token secreto dentro de un link— que le permite establecer una nueva contraseña.

## 2. Contexto técnico

El sistema se construye sobre la siguiente arquitectura:

- **Frontend:** React, organizado de forma modular. Vive en su propio origen.
- **Backend:** Java con Spring Boot, siguiendo arquitectura hexagonal (puertos y adaptadores). Vive en un origen separado.
- **Base de datos:** MySQL.
- **Envío de correos:** API de Brevo (endpoint transaccional `POST https://api.brevo.com/v3/smtp/email`).
- **Autenticación general de la app:** JWT en la cabecera `Authorization: Bearer ...`.

## 3. Flujo general

El proceso completo consta de cinco pasos:

1. **Solicitud.** El usuario escribe su correo en el formulario de "olvidé mi contraseña".
2. **Generación del token.** El servidor crea un token aleatorio, guarda su hash asociado al usuario con una fecha de expiración.
3. **Envío del link.** Se envía un correo con un enlace que incluye el token.
4. **Validación.** El usuario hace clic; el servidor verifica que el token existe, no expiró y no fue usado.
5. **Cambio de contraseña.** Si es válido, se muestra el formulario de nueva contraseña y, al guardarla, se invalida el token para que no pueda reutilizarse.

## 4. Anatomía del enlace

Como el frontend y el backend están separados, **el enlace apunta al frontend**, no al backend:

```
https://tufrontend.com/reset-password?token=XYZ
```

Partes:

- **Dominio y ruta** (`https://tufrontend.com/reset-password`): la vista de React que procesará el restablecimiento. Siempre HTTPS.
- **El token** (`token=...`): el corazón del sistema. Valor aleatorio criptográficamente seguro, largo e imposible de adivinar. Se genera con `SecureRandom` (nunca con generadores no seguros).

La página de React lee el token del query string, opcionalmente lo valida al montar, y al enviar el formulario lo reenvía al backend. El token nunca se guarda en `localStorage`; vive solo en la URL y en el estado del componente mientras dura la operación.

### 4.1. Dónde vive la URL: el backend la construye, nunca el cliente

**Decisión:** la URL base del frontend se guarda en la configuración del backend y el enlace se construye ahí. El backend **nunca** confía en una URL que venga en el cuerpo de la petición. El frontend solo manda el email en `/request`, nada más.

**Por qué no dejar que el frontend mande el dominio:** hacerlo abre una vulnerabilidad conocida, el *password reset poisoning* (una variante de host header injection). Como `/request` no está autenticado, un atacante podría enviar:

```
POST /api/auth/password-reset/request
{ "email": "victima@correo.com", "resetUrl": "https://sitio-malo.com/reset" }
```

Y el servidor le enviaría a la víctima un correo legítimo, desde el dominio propio, con un enlace que apunta al sitio del atacante. La víctima confía en el correo, hace clic, y el token de un solo uso viaja hasta el servidor del atacante, que lo usa en `/confirm` para tomar la cuenta. El principio de fondo: el token es un secreto que solo debe llegar a un destino que el propio backend controla; si el cliente decide el destino, se pierde ese control.

**Implementación (Spring Boot):** la URL base vive en configuración como variable de entorno.

```yaml
# application.yml
app:
  frontend:
    reset-url: ${FRONTEND_RESET_URL}   # p. ej. https://tufrontend.com/reset-password
```

```java
@Component
public class ResetLinkBuilder {
    @Value("${app.frontend.reset-url}")
    private String resetBaseUrl;

    public String build(String rawToken) {
        return resetBaseUrl + "?token=" + rawToken;
    }
}
```

**Manejo de entornos:** la razón habitual para querer mandar la URL desde el frontend (soportar local, staging y producción) se resuelve con variables de entorno por entorno, sin ceder el control al cliente:

- Local: `FRONTEND_RESET_URL=http://localhost:5173/reset-password`
- Staging: `FRONTEND_RESET_URL=https://staging.tuapp.com/reset-password`
- Producción: `FRONTEND_RESET_URL=https://tufrontend.com/reset-password`

Cada despliegue del backend levanta con su variable (o con perfiles de Spring: `application-dev.yml`, `application-prod.yml`).

**Caso de varios frontends legítimos:** si un mismo backend sirviera a varios frontends de verdad, no se acepta la URL cruda; se guarda una lista blanca de dominios permitidos en la configuración del backend y el frontend manda a lo sumo un identificador corto ("web", "app-b") que el servidor traduce a una URL de su lista. El cliente elige entre opciones que el backend controla, nunca una URL arbitraria. Para este sistema, con un solo frontend, basta la variable de entorno simple.

## 5. Modelo de datos (MySQL)

Los tokens se guardan **hasheados** en su propia tabla:

```sql
CREATE TABLE password_reset_tokens (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  token_hash   CHAR(64) NOT NULL,          -- SHA-256 en hex
  expires_at   DATETIME NOT NULL,
  used_at      DATETIME NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uq_token_hash (token_hash),
  KEY idx_user (user_id)
);
```

## 6. Hasheo del token vs. hasheo de la contraseña

Son dos cosas distintas y usan algoritmos distintos:

- **La contraseña** es de baja entropía (la elige un humano). Se protege con **BCrypt** (o Argon2): lento y con salt por hash.
- **El token** es aleatorio y de alta entropía (lo genera `SecureRandom`). Se protege con **SHA-256**: rápido y determinista. Es determinista a propósito, para poder buscar por `token_hash` con un índice. Con BCrypt no se podría, porque cada hash lleva su propio salt.

Generación del token (32 bytes → Base64 URL-safe, ~43 caracteres):

```java
byte[] bytes = new byte[32];
new SecureRandom().nextBytes(bytes);
String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
// se envía rawToken en el link; se guarda sha256(rawToken)
```

## 7. Encaje en la arquitectura hexagonal

La lógica de negocio (generar, validar e invalidar tokens) vive en el **dominio**; MySQL, Brevo y el envío HTTP entran como **adaptadores** detrás de puertos, de modo que el core no sabe que existe Brevo ni JPA.

- **Puertos de entrada (casos de uso):** `RequestPasswordReset`, `ConfirmPasswordReset`.
- **Puertos de salida:** `UserRepository`, `PasswordResetTokenRepository`, `EmailSender`, `TokenHasher`, `PasswordEncoder`, `Clock`.
- **Adaptadores de entrada:** un `@RestController`.
- **Adaptadores de salida:** repositorio JPA (MySQL), cliente de Brevo, BCrypt, SHA-256.

## 8. Endpoints

### 8.1. Solicitar el reset

```
POST /api/auth/password-reset/request   { "email": "..." }
→ 200 SIEMPRE, con mensaje genérico
```

Internamente: se busca el usuario; si existe, se invalidan sus tokens previos, se genera uno nuevo, se guarda el hash con expiración (15–30 min) y se encola el correo. Si no existe, no se hace nada. La respuesta y el tiempo deben ser idénticos en ambos casos, por lo que el correo se envía de forma asíncrona.

### 8.2. Validar al abrir el link (opcional)

```
GET /api/auth/password-reset/validate?token=...
→ 200 válido | 400 inválido
```

Solo lectura: se hashea el token, se busca, y se revisa expiración y `used_at`. **No se consume el token aquí** (algunos clientes de correo previsualizan enlaces y "tocarían" la URL).

### 8.3. Confirmar la nueva contraseña

```
POST /api/auth/password-reset/confirm   { "token": "...", "newPassword": "..." }
```

La validación real, dentro de una transacción:

```java
@Transactional
public void confirm(String rawToken, String newPassword) {
    String hash = tokenHasher.sha256(rawToken);
    var prt = tokenRepo.findByTokenHash(hash)
        .filter(t -> t.getUsedAt() == null)
        .filter(t -> t.getExpiresAt().isAfter(clock.now()))
        .orElseThrow(() -> new InvalidResetTokenException()); // error genérico

    passwordPolicy.validate(newPassword);
    var user = userRepo.findById(prt.getUserId()).orElseThrow();
    user.setPasswordHash(passwordEncoder.encode(newPassword)); // BCrypt aquí

    prt.markUsed(clock.now());
    tokenRepo.invalidateAllForUser(user.getId()); // quema los demás
    sessionRegistry.revokeAll(user.getId());       // cierra sesiones activas
    emailSender.sendPasswordChangedNotice(user.getEmail()); // aviso
}
```

El `@Transactional` junto con el `UNIQUE` en `token_hash` protegen de la condición de carrera (dos peticiones casi simultáneas con el mismo token).

## 9. Contenido del correo

El correo de reset debe ser deliberadamente aburrido: un solo propósito, un solo enlace.

**Sí debe contener:** un mensaje claro de qué es y por qué llega; el botón/enlace con el token (y la URL en texto plano como respaldo); el tiempo de expiración explícito; una salida por si no fue el usuario ("si no solicitaste esto, ignora este correo"); una identidad de remitente clara y reconocible; y un canal de soporte si aplica.

**No debe contener:** la contraseña actual ni una nueva contraseña (jamás); datos personales de más; enlaces de rastreo agresivos o redirecciones que puedan exponer el token; ni otros enlaces o llamados a la acción de marketing.

En Brevo, conviene autenticar el dominio con DKIM y usar un remitente verificado; de lo contrario Brevo reemplaza el dominio de envío por `@brevosend.com`, lo que perjudica la entrega. Lo ideal es usar una plantilla (`templateId`) y pasarle solo la `RESET_URL` como parámetro, para no armar el HTML en el backend. La API key de Brevo va en el header `api-key` y **nunca** en el repositorio: variable de entorno o gestor de secretos.

## 10. Validaciones en dos momentos

Hay dos momentos y las validaciones no son iguales. La regla de oro es que **el token se consume solo al enviar la nueva contraseña, no al abrir el link.**

**Al abrir el link (GET):** verificación ligera. Se comprueba que el token existe (por su hash), que no expiró y que no fue usado. Si pasa, se muestra el formulario. No se marca ni se borra nada todavía.

**Al enviar la nueva contraseña (POST):** verificación real. Se repiten las tres comprobaciones anteriores contra la base de datos en ese momento, se valida la nueva contraseña, y todo ocurre en una operación atómica. Una vez guardada la contraseña: se invalida el token usado, se invalidan los demás tokens del usuario, se cierran sus sesiones activas y, opcionalmente, se envía un correo de confirmación del cambio.

Detalles a no olvidar: el mensaje de error siempre genérico (tratar igual "no existe", "expiró" y "ya usado"); nunca confiar en datos del cliente como el `uid` o el email de la URL (la fuente de verdad es el token buscado en el almacén).

## 11. Barreras de seguridad

### 11.1. Prevención de enumeración de usuarios

El endpoint `/request` responde **siempre lo mismo**, exista o no el correo: *"Si ese correo está registrado, te enviamos un enlace."* Distinguir los casos convertiría el formulario en una herramienta para averiguar qué correos tienen cuenta (útil para phishing y relleno de credenciales).

La respuesta idéntica en texto no basta: el **tiempo** también puede delatar. Si con un correo existente el servidor tarda más (genera token, hashea, guarda, envía), un atacante mide la diferencia y enumera igual (timing attack). Por eso el envío del correo se hace de forma asíncrona, para que la respuesta al navegador tarde lo mismo en ambos casos. El mismo principio aplica al login: mensaje genérico "correo o contraseña incorrectos".

### 11.2. Rate limiting

Limita cuántas veces se puede llamar a un endpoint en un periodo (por ejemplo, 5 solicitudes por clave cada 15 minutos); al superarlo se responde `429 Too Many Requests`. Evita el bombardeo de correos (que llena la bandeja de la víctima y quema la cuota de Brevo), la fuerza bruta sobre el token en `/confirm`, y los ataques automatizados de enumeración por volumen.

**Ubicación en hexagonal:** es una preocupación técnica de la capa de entrega, no de dominio. Va en infraestructura como un **filtro** que se ejecuta **antes** del filtro JWT, para frenar al atacante en la puerta sin gastar recursos validando tokens que se van a rechazar de todos modos.

Se implementa con la librería **Bucket4j** (algoritmo token bucket). Piezas:

1. Un `RateLimiterService` que guarda un `Bucket` por clave en un `ConcurrentHashMap`.
2. Un filtro `RateLimitingFilter` (extiende `OncePerRequestFilter`) que solo actúa sobre `/api/auth/password-reset/**`, consume una ficha del balde de esa IP y, si está vacío, corta con `429` y cabecera `Retry-After`.
3. Registro en la cadena de seguridad antes del filtro de autenticación:
   `http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);`

**IP vs. email:** el filtro limita por IP (dato de cabeceras). El límite por email (para que no bombardeen a un usuario desde muchas IPs) se aplica dentro del caso de uso o del controlador, donde el email ya está parseado, llamando al mismo `RateLimiterService` con una clave como `"reset-email:" + email`. Esto evita leer el cuerpo dentro del filtro (que rompería la lectura posterior del controlador).

Para obtener la IP real detrás de un proxy o balanceador se lee la cabecera `X-Forwarded-For`.

**Almacén:** el `ConcurrentHashMap` en memoria sirve con una sola instancia del backend. Si se escala a varias instancias, cada una tendría su propio conteo; en ese punto se migra el almacén a Redis (soportado por Bucket4j de forma nativa), sin cambiar la lógica del filtro.

**Ubicación de archivos:** infraestructura de entrada, por ejemplo `infrastructure/adapter/in/web/security/` o `infrastructure/config/`. El dominio no se entera de que existe rate limiting.

### 11.3. CORS (Cross-Origin Resource Sharing)

Los navegadores aplican el Same-Origin Policy: por defecto bloquean que el JavaScript de un origen (protocolo + dominio + puerto) haga peticiones a otro origen. Como el React y el Spring Boot están en orígenes distintos, el navegador bloquearía las llamadas sin una configuración de CORS que autorice explícitamente el origen del frontend.

Se configura en el backend. Con Spring Security:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://tufrontend.com")); // exacto, no "*"
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Advertencias: no usar `*` en producción (listar el dominio exacto del frontend); y `allowCredentials(true)` es incompatible con `*`. El navegador puede enviar antes una petición de sondeo automática (`OPTIONS`, el *preflight*) que Spring responde solo si CORS está configurado.

### 11.4. CSRF (Cross-Site Request Forgery)

CSRF es un ataque en el que un sitio malicioso hace que el navegador de la víctima envíe una petición a otro sitio donde ya está logueada, aprovechando que **las cookies se envían automáticamente** en cada petición a su dominio.

**Decisión para este sistema:** como la autenticación usa **JWT en la cabecera `Authorization`**, CSRF prácticamente no aplica. El navegador adjunta cookies solas, pero **no** adjunta cabeceras `Authorization` solas; un sitio malicioso no puede leer ni escribir el token JWT (se lo impide el Same-Origin Policy). Por tanto se **desactiva** la protección CSRF de Spring:

```java
http.csrf(csrf -> csrf.disable());
```

Nota sobre los endpoints de reset: `/request` y `/confirm` son endpoints **sin autenticar** (el usuario justamente no puede loguearse), así que no hay sesión que robar y el riesgo clásico de CSRF no aplica ahí. Lo que los protege es el token de un solo uso, el rate limiting y el mensaje genérico.

## 12. Flujo en el frontend (React)

El módulo de recuperación tiene dos vistas:

1. **"Olvidé mi contraseña":** formulario con el email → `POST /request`. Siempre muestra el mismo mensaje ("si el correo existe, te enviamos un enlace").
2. **"Nueva contraseña":** lee el `token` del query string, opcionalmente llama a `/validate` al montar para mostrar el formulario o un error, y al enviar hace `POST /confirm`. Tras el éxito, redirige al login.

## 13. Checklist de implementación

- [ ] Respuesta y tiempo idénticos en `/request` (envío de correo asíncrono) para evitar enumeración de usuarios.
- [ ] Token generado con `SecureRandom`, guardado como SHA-256; contraseña con BCrypt.
- [ ] Expiración de 15–30 minutos, un solo uso, e invalidación de tokens previos al generar uno nuevo.
- [ ] Confirmación atómica con `@Transactional` + `UNIQUE(token_hash)`.
- [ ] Cierre de sesiones activas y correo de aviso al cambiar la contraseña.
- [ ] Rate limiting en `/request` (por IP y por email) y `/confirm` (por IP), con `429` y `Retry-After`.
- [ ] Filtro de rate limit registrado antes del filtro JWT.
- [ ] API key de Brevo fuera del código (variable de entorno o gestor de secretos); dominio autenticado con DKIM.
- [ ] CORS configurado con el origen exacto del frontend.
- [ ] CSRF desactivado (se usa JWT); confirmado que no se usan cookies de sesión.
- [ ] Enlace construido en el backend a partir de una URL en configuración; el frontend solo manda el email (previene reset poisoning).
- [ ] URL del frontend gestionada por variable de entorno por cada entorno (local, staging, producción).
- [ ] Job periódico que borre tokens expirados de la tabla.
- [ ] Correo de reset con un solo propósito, expiración visible y salida por si no fue el usuario.

## 14. Resumen de las barreras de seguridad

- **Enumeración de usuarios:** respuesta y tiempo idénticos, existan o no el correo.
- **Rate limiting:** frena el abuso por volumen (bombardeo, fuerza bruta) en la puerta.
- **CORS:** permite que React y Spring Boot (dos orígenes) se comuniquen sin que el navegador los bloquee.
- **CSRF:** evita que un sitio ajeno use la sesión a espaldas del usuario; desactivado aquí por usar JWT.
- **Token seguro:** aleatorio, hasheado, de un solo uso y con expiración corta.
- **Confirmación atómica:** transacción + restricción de unicidad contra condiciones de carrera.
- **Contención post-cambio:** invalidación de tokens, cierre de sesiones y correo de aviso.
