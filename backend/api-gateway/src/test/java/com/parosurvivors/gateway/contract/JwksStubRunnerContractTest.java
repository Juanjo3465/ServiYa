package com.parosurvivors.gateway.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cloud.contract.stubrunner.junit.StubRunnerExtension;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T6.20 — Contrato del JWKS, lado CONSUMIDOR con stub-runner.
 *
 * Descarga y corre el STUB del productor (el emisor del JWKS, hoy el monolito:
 * {@code com.parosurvivors:serviya:0.0.1-SNAPSHOT:stubs}) y verifica que el gateway puede consumir
 * {@code /.well-known/jwks.json} con la forma del contrato (ES256, EC/P-256, kid/x/y, SIN el parámetro
 * privado 'd'). Si el productor rompiera la forma del JWKS, su stub cambiaría y estas aserciones —la
 * expectativa del consumidor— fallarían, rompiendo el build del gateway (acople productor↔consumidor).
 *
 * Modo LOCAL: resuelve el stub jar del repositorio Maven local (requiere `mvn install` del productor).
 * El material de clave del stub es un placeholder del contrato (no permite sign→verify real); el valor de
 * este test es el acople de FORMA/build, complementario al rechazo de token real de los tests de T6.19.
 *
 * <p><b>@Tag("stubrunner"):</b> el build normal del gateway lo EXCLUYE ({@code excludedGroups=stubrunner}
 * en el pom), porque requiere el stub jar del productor en el repo Maven local. El job dedicado de CI
 * ({@code contract-stub-runner}) instala primero ese stub y luego lo corre con {@code -Dgroups=stubrunner}.</p>
 */
@Tag("stubrunner")
class JwksStubRunnerContractTest {

    @RegisterExtension
    static final StubRunnerExtension stubRunner = new StubRunnerExtension()
            .downloadStub("com.parosurvivors", "serviya", "0.0.1-SNAPSHOT", "stubs")
            .stubsMode(StubRunnerProperties.StubsMode.LOCAL);

    @Test
    void elGatewayConsumeElContratoDelJwksDelEmisor() throws Exception {
        String base = stubRunner.findStubUrl("serviya").toString();

        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(base + "/.well-known/jwks.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.headers().firstValue("Cache-Control").orElse("")).contains("max-age=300");

        JsonNode key = new ObjectMapper().readTree(res.body()).path("keys").path(0);
        assertThat(key.path("kty").asText()).isEqualTo("EC");
        assertThat(key.path("crv").asText()).isEqualTo("P-256");
        assertThat(key.path("alg").asText()).isEqualTo("ES256");
        assertThat(key.path("use").asText()).isEqualTo("sig");
        assertThat(key.path("kid").asText()).isNotBlank();
        assertThat(key.path("x").asText()).isNotBlank();
        assertThat(key.path("y").asText()).isNotBlank();
        // El JWKS NUNCA debe exponer el parámetro privado 'd'.
        assertThat(key.has("d")).isFalse();
    }
}
