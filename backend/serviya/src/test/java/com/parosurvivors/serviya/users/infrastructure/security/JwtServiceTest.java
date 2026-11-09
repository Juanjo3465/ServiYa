package com.parosurvivors.serviya.users.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.parosurvivors.serviya.users.application.dto.result.IssuedToken;
import com.parosurvivors.serviya.users.domain.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

/**
 * El access token (RF-001/RF-002) se firma con ES256 (clave privada) y se verifica con la pública, y lleva
 * los claims del contrato §4.1 (Fase 1, guia-implementacion-auth.md). El fail-fast de las claves vive en
 * {@link JwtKeysTest}.
 */
class JwtServiceTest {

    private JwtKeys keys;
    private JwtService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = JwtTestSupport.properties();
        keys = new JwtKeys(props);
        service = new JwtService(keys, props);
    }

    @Test
    void issuesAndResolvesTokenRoundTrip() {
        IssuedToken issued = service.issue(42L, List.of(RoleName.CLIENT, RoleName.OFFERER));

        Optional<Authentication> resolved = service.resolve(issued.token());
        assertThat(resolved).isPresent();
        assertThat(resolved.get().getPrincipal()).isEqualTo(42L);
        assertThat(resolved.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENT", "ROLE_OFFERER");
    }

    @Test
    @SuppressWarnings("unchecked")
    void issuedTokenCarriesContractClaims() {
        IssuedToken issued = service.issue(42L, List.of(RoleName.CLIENT, RoleName.OFFERER));

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(keys.publicKey())
                .build()
                .parseSignedClaims(issued.token());
        JwsHeader header = jws.getHeader();
        Claims claims = jws.getPayload();

        assertThat(header.getAlgorithm()).isEqualTo("ES256");
        assertThat(header.getKeyId()).isEqualTo(JwtTestSupport.KEY_ID);
        assertThat(header.getType()).isEqualTo("at+jwt");
        assertThat(claims.getIssuer()).isEqualTo(JwtTestSupport.ISSUER);
        assertThat(claims.getAudience()).contains(JwtTestSupport.AUDIENCE);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getId()).isNotBlank(); // jti
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat((List<String>) claims.get("roles", List.class)).containsExactly("CLIENT", "OFFERER");
    }

    @Test
    void rejectsInvalidToken() {
        assertThat(service.resolve("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        IssuedToken issued = service.issue(42L, List.of(RoleName.CLIENT));

        JwtProperties otherIssuer = JwtTestSupport.properties();
        otherIssuer.setIssuer("https://auth.evil.test");
        JwtService resolverExpectingAnotherIssuer = new JwtService(keys, otherIssuer);

        assertThat(resolverExpectingAnotherIssuer.resolve(issued.token())).isEmpty();
    }
}
