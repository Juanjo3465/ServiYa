package com.parosurvivors.serviya.contract;

import com.parosurvivors.serviya.users.infrastructure.adapters.input.JwksController;
import com.parosurvivors.serviya.users.infrastructure.security.JwtKeys;
import com.parosurvivors.serviya.users.infrastructure.security.JwtTestSupport;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

/**
 * Clase base de los tests de Spring Cloud Contract (T6.18 / contract tests M24). El plugin SCC genera un
 * test por contrato (src/test/resources/contracts/) que extiende esta clase y ejecuta la petición con
 * RestAssuredMockMvc contra el controlador real; si el PRODUCTOR (este monolito) deja de honrar el
 * contrato, el build falla.
 *
 * <p>Se usa {@code standaloneSetup} con los controladores concretos (sin levantar el contexto de Spring
 * ni BD) para que la verificación sea rápida. Las claves ES256 de prueba salen de {@link JwtTestSupport}.
 */
public abstract class ContractVerifierBase {

    @BeforeEach
    void setup() {
        JwksController jwksController = new JwksController(new JwtKeys(JwtTestSupport.properties()));
        RestAssuredMockMvc.standaloneSetup(jwksController);
    }
}
