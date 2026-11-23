package com.parosurvivors.serviya.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.shared.audit.SecurityAuditServicePort;
import com.parosurvivors.serviya.shared.ratelimit.RateLimiter;
import com.parosurvivors.serviya.users.application.dto.result.AuthResult;
import com.parosurvivors.serviya.users.application.ports.input.SessionRevocationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.TokenSessionServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserAuthenticationServicePort;
import com.parosurvivors.serviya.users.domain.RoleName;
import com.parosurvivors.serviya.users.infrastructure.adapters.input.AuthController;
import com.parosurvivors.serviya.users.infrastructure.adapters.input.JwksController;
import com.parosurvivors.serviya.users.infrastructure.mappers.UserWebMapper;
import com.parosurvivors.serviya.users.infrastructure.security.JwtKeys;
import com.parosurvivors.serviya.users.infrastructure.security.JwtTestSupport;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;

/**
 * Clase base de los tests de Spring Cloud Contract (T6.18–T6.21 / contract tests M24). El plugin SCC
 * genera un test por contrato (src/test/resources/contracts/) que extiende esta clase y ejecuta la
 * petición con RestAssuredMockMvc contra los controladores reales; si el PRODUCTOR (este monolito, hoy
 * emisor del JWKS y de los tokens) deja de honrar un contrato, el build falla.
 *
 * <p>Se usa {@code standaloneSetup} (sin contexto de Spring ni BD) para que la verificación sea rápida:
 * <ul>
 *   <li>{@link JwksController} real con las claves ES256 de prueba ({@link JwtTestSupport}) — contrato del JWKS.</li>
 *   <li>{@link AuthController} real con el caso de uso de login <b>stubbeado</b> (Mockito) + el
 *       {@link UserWebMapper} real — contrato del borde del login (forma de la respuesta: access token en el
 *       cuerpo + refresh en cookie HttpOnly). No se verifica la emisión/firma real del token aquí; eso lo
 *       cubren los tests de emisión (JwtServiceTest) y el contrato del JWKS.</li>
 * </ul>
 */
public abstract class ContractVerifierBase {

    /** JWT ES256 de ejemplo (3 segmentos base64url). El contrato del login solo fija la FORMA del token. */
    private static final String SAMPLE_JWT =
            "eyJhbGciOiJFUzI1NiIsImtpZCI6InRlc3Qta2V5LTEiLCJ0eXAiOiJhdCtqd3QifQ"
            + ".eyJzdWIiOiIxIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNlcnZpeWEubG9jYWwiLCJhdWQiOiJtYXJrZXRwbGFjZS1hcGkifQ"
            + ".MEUCIQDexampleContractShapeSignatureValueAAAAAAAAAAAAAAAAAAAAAA";

    @BeforeEach
    void setup() {
        JwksController jwksController = new JwksController(new JwtKeys(JwtTestSupport.properties()));

        UserAuthenticationServicePort authService = mock(UserAuthenticationServicePort.class);
        when(authService.login(any())).thenReturn(new AuthResult(
                SAMPLE_JWT,
                1L,
                List.of(RoleName.CLIENT),
                LocalDateTime.now().plusMinutes(15),
                "refresh-token-raw-value-for-contract",
                LocalDateTime.now().plusDays(30)));

        UserWebMapper webMapper = Mappers.getMapper(UserWebMapper.class);

        AuthController authController = new AuthController(
                authService,
                mock(TokenSessionServicePort.class),
                mock(SessionRevocationServicePort.class),
                mock(SecurityAuditServicePort.class),
                webMapper,
                mock(RateLimiter.class),
                JwtTestSupport.properties());

        RestAssuredMockMvc.standaloneSetup(jwksController, authController);
    }
}
