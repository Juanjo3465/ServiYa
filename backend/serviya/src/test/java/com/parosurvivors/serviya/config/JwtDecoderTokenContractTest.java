package com.parosurvivors.serviya.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parosurvivors.serviya.users.infrastructure.security.JwtKeys;
import com.parosurvivors.serviya.users.infrastructure.security.JwtProperties;
import com.parosurvivors.serviya.users.infrastructure.security.JwtTestSupport;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Contrato del token en el RESOURCE SERVER del monolito (T6.19). Verifica, contra el {@code jwtDecoder}
 * real de {@link SecurityConfig}, que se ACEPTA un access token ES256 válido y se RECHAZAN los que violan
 * el contrato 4.1: algoritmo distinto de ES256, {@code aud} incorrecto, {@code iss} incorrecto, expirado
 * y con firma inválida. Es la contraparte "consumidor" del contrato del JWKS/token (T6.19/T6.20).
 *
 * <p>Nota de alineación: la misma batería debe pasar en los otros resource servers (notif, gateway, chat),
 * que verifican con la MISMA configuración ES256/JWKS (ESTADO M42, checklist transversal).
 */
class JwtDecoderTokenContractTest {

    private final JwtProperties props = JwtTestSupport.properties();
    private final JwtKeys keys = new JwtKeys(props);
    private final JwtDecoder decoder = new SecurityConfig(keys, props, null).jwtDecoder();

    /** Claims válidos (iss/aud/typ/kid/sub/iat/exp correctos); cada test firma o altera lo que corresponda. */
    private JwtBuilder validClaims() {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(JwtTestSupport.KEY_ID).type(props.getTyp()).and()
                .issuer(JwtTestSupport.ISSUER)
                .audience().add(JwtTestSupport.AUDIENCE).and()
                .subject("1")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)));
    }

    @Test
    void acceptsValidEs256Token() {
        String token = validClaims().signWith(keys.privateKey(), Jwts.SIG.ES256).compact();

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getAudience()).contains(JwtTestSupport.AUDIENCE);
    }

    @Test
    void rejectsNonEs256Algorithm() {
        SecretKey hmac = Jwts.SIG.HS256.key().build();
        String token = validClaims().signWith(hmac, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongAudience() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .header().keyId(JwtTestSupport.KEY_ID).type(props.getTyp()).and()
                .issuer(JwtTestSupport.ISSUER)
                .audience().add("otra-api").and()
                .subject("1").issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(900)))
                .signWith(keys.privateKey(), Jwts.SIG.ES256).compact();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongIssuer() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .header().keyId(JwtTestSupport.KEY_ID).type(props.getTyp()).and()
                .issuer("https://evil.example")
                .audience().add(JwtTestSupport.AUDIENCE).and()
                .subject("1").issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(900)))
                .signWith(keys.privateKey(), Jwts.SIG.ES256).compact();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant past = Instant.now().minusSeconds(3600);
        String token = Jwts.builder()
                .header().keyId(JwtTestSupport.KEY_ID).type(props.getTyp()).and()
                .issuer(JwtTestSupport.ISSUER)
                .audience().add(JwtTestSupport.AUDIENCE).and()
                .subject("1").issuedAt(Date.from(past)).expiration(Date.from(past.plusSeconds(60)))
                .signWith(keys.privateKey(), Jwts.SIG.ES256).compact();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        // Firmado con OTRA clave EC P-256: estructura válida pero la firma no verifica con la clave pública del emisor.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        ECPrivateKey otherKey = (ECPrivateKey) kpg.generateKeyPair().getPrivate();
        String token = validClaims().signWith(otherKey, Jwts.SIG.ES256).compact();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
