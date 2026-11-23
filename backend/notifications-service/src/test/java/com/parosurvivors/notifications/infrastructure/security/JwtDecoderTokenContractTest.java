package com.parosurvivors.notifications.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Contrato del token en el RESOURCE SERVER de notif (T6.19). Igual que el del monolito pero verificando vía
 * JWKS: un {@link MockWebServer} publica el JWKS de prueba y se ejerce el {@code jwtDecoder()} real de
 * {@link SecurityConfig}. Acepta un ES256 válido y rechaza alg≠ES256, {@code aud}/{@code iss} incorrectos,
 * expirado y firma inválida (ESTADO M42, checklist transversal: misma batería en monolito, gateway y chat).
 */
class JwtDecoderTokenContractTest {

    private static final String ISSUER = "https://auth.serviya.test";
    private static final String AUDIENCE = "marketplace-api";

    private MockWebServer jwksServer;
    private TestJwks jwks;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        jwks = new TestJwks();
        jwksServer = new MockWebServer();
        jwksServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setBody(jwks.jwksJson());
            }
        });
        jwksServer.start();

        SecurityConfig config = new SecurityConfig(null);
        ReflectionTestUtils.setField(config, "jwksUri", jwksServer.url("/.well-known/jwks.json").toString());
        ReflectionTestUtils.setField(config, "issuer", ISSUER);
        ReflectionTestUtils.setField(config, "audience", AUDIENCE);
        ReflectionTestUtils.setField(config, "typ", "at+jwt");
        decoder = config.jwtDecoder();
    }

    @AfterEach
    void tearDown() throws Exception {
        jwksServer.shutdown();
    }

    @Test
    void acceptsValidEs256Token() {
        Jwt jwt = decoder.decode(jwks.validToken(ISSUER, AUDIENCE, "1", List.of("CLIENT")));

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getAudience()).contains(AUDIENCE);
    }

    @Test
    void rejectsNonEs256Algorithm() {
        assertThatThrownBy(() -> decoder.decode(jwks.hs256Token(ISSUER, AUDIENCE, "1")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongAudience() {
        assertThatThrownBy(() -> decoder.decode(jwks.validToken(ISSUER, "otra-api", "1", List.of("CLIENT"))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongIssuer() {
        assertThatThrownBy(() -> decoder.decode(jwks.validToken("https://evil.example", AUDIENCE, "1", List.of("CLIENT"))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        assertThatThrownBy(() -> decoder.decode(jwks.expiredToken(ISSUER, AUDIENCE, "1", List.of("CLIENT"))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsInvalidSignature() {
        // Token firmado con OTRA clave EC (mismo kid): el JWKS servido es el de 'jwks' ⇒ la firma no verifica.
        String token = new TestJwks().validToken(ISSUER, AUDIENCE, "1", List.of("CLIENT"));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
