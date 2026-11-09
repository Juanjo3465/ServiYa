package com.parosurvivors.serviya.users.infrastructure.security;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la emisión de tokens de acceso ES256 (Fase 1 del plan de centralización de auth,
 * {@code documents/microservicios/guia-implementacion-auth.md}). Externalizada a {@code serviya.security.jwt.*}
 * para ajustarla sin recompilar y para que los secretos (claves) se inyecten por entorno.
 *
 * <pre>
 * serviya:
 *   security:
 *     jwt:
 *       private-key: ${JWT_PRIVATE_KEY}   # clave EC P-256 privada (PKCS#8, base64 DER o PEM). SOLO el emisor.
 *       public-key:  ${JWT_PUBLIC_KEY}    # clave EC P-256 pública (X.509 SPKI, base64 DER o PEM). Se publica en el JWKS.
 *       key-id:      ${JWT_KEY_ID}        # 'kid' del header; selecciona la clave en el JWKS (rotación).
 *       issuer:      ${JWT_ISSUER}        # 'iss': URL lógica estable del emisor (sobrevive a la extracción de identidad).
 *       audience:    ${JWT_AUDIENCE}      # 'aud': destinatario de la API (marketplace-api).
 *       access-ttl:  ${JWT_ACCESS_TTL}    # vida del access token (p. ej. 15m). El refresh llega en la Fase 2.
 *       typ:         at+jwt               # 'typ' del header (RFC 9068): evita confundir otros JWT con un access token.
 * </pre>
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "serviya.security.jwt")
public class JwtProperties {

    /** Clave EC P-256 privada (PKCS#8), en base64 del DER o PEM con armadura. Solo la tiene el emisor. */
    private String privateKey;

    /** Clave EC P-256 pública (X.509 SubjectPublicKeyInfo), en base64 del DER o PEM. Se publica en el JWKS. */
    private String publicKey;

    /** Identificador de la clave ({@code kid}); permite publicar/rotar varias claves en el JWKS. */
    private String keyId;

    /** Emisor ({@code iss}): URL lógica estable, no la URL interna del servicio. */
    private String issuer;

    /** Audiencia ({@code aud}): identificador de la API destinataria. */
    private String audience;

    /** Vida del access token (formato de duración de Spring, p. ej. {@code 15m}). */
    private Duration accessTtl = Duration.ofMinutes(15);

    /** Tipo del token en el header JOSE ({@code typ}); por defecto {@code at+jwt} (RFC 9068). */
    private String typ = "at+jwt";
}
