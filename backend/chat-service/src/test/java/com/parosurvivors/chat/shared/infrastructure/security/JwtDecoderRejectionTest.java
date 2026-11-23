package com.parosurvivors.chat.shared.infrastructure.security;

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

/**
 * Contrato del token en el RESOURCE SERVER del chat a nivel DECODER (T6.19). Complementa a
 * {@link JwtTokenValidationTest} (que cubre iss/aud/exp a nivel validador) ejerciendo el {@code jwtDecoder}
 * real de {@link JwtDecoderConfig} vía un {@link MockWebServer} que publica el JWKS: acepta un ES256 válido
 * y rechaza alg≠ES256, {@code aud}/{@code iss} incorrectos, expirado y firma inválida (ESTADO M42, misma
 * batería que monolito, notif y gateway).
 */
class JwtDecoderRejectionTest {

    private static final JwtProperties PROPS = new JwtProperties("https://auth.serviya", "marketplace-api");
    private static final String ISSUER = "https://auth.serviya";
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

        decoder = new JwtDecoderConfig()
                .jwtDecoder(jwksServer.url("/.well-known/jwks.json").toString(), PROPS);
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
        // Firmado con OTRA clave EC (mismo kid): el JWKS servido es el de 'jwks' ⇒ la firma no verifica.
        String token = new TestJwks().validToken(ISSUER, AUDIENCE, "1", List.of("CLIENT"));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
