package com.parosurvivors.serviya.shared.exception.handlers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parosurvivors.serviya.shared.exceptions.ForbiddenException;
import com.parosurvivors.serviya.shared.exceptions.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato del formato de error 4.4 en el monolito (T6.21): toda excepción de aplicación se traduce al
 * cuerpo compartido {@code {status, error, code, message, correlationId}} con los códigos del catálogo
 * (401 → {@code UNAUTHORIZED}, 403 → {@code FORBIDDEN}, ...) y el {@code correlationId} propagado por el
 * gateway vía {@code X-Request-Id}. El frontend decide por el {@code code} (refresca ante 401, no ante 403).
 *
 * <p>Alineación transversal (ESTADO M42): el gateway ya verifica el mismo cuerpo/códigos en
 * {@code ApiGatewayRoutingTest} (TOKEN_MISSING/EXPIRED/INVALID, FORBIDDEN, RATE_LIMITED); notif usa el mismo
 * {@code ErrorResponse}. El chat DIVERGE (namespace {@code CHAT_*}) → pendiente de unificar (M42).
 */
class ErrorResponseContractTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/contract-error/forbidden")
        String forbidden() {
            throw new ForbiddenException("sin permiso sobre el recurso");
        }

        @GetMapping("/contract-error/unauthorized")
        String unauthorized() {
            throw new UnauthorizedException("credenciales invalidas");
        }
    }

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void forbiddenExceptionReturns403WithContract44Body() throws Exception {
        mvc.perform(get("/contract-error/forbidden").header("X-Request-Id", "corr-123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.correlationId").value("corr-123"));
    }

    @Test
    void unauthorizedExceptionReturns401WithContract44Body() throws Exception {
        mvc.perform(get("/contract-error/unauthorized")
                        .header("X-Request-Id", "corr-456")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").value("corr-456"));
    }
}
