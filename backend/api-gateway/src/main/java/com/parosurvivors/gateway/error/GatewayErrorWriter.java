package com.parosurvivors.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Escribe el cuerpo de error 4.4 en la respuesta reactiva. Centraliza el serializado JSON, el
 * {@code Content-Type}, el estado y el {@code correlationId} (de la cabecera {@code X-Correlation-Id} si
 * viene —la pondrá el filtro de T3.10— o uno nuevo) para que el entry point (401) y el access denied
 * handler (403) produzcan exactamente el mismo formato.
 */
public final class GatewayErrorWriter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final ObjectMapper objectMapper;

    public GatewayErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, GatewayErrorCode code,
                            String message, String wwwAuthenticate) {
        String correlationId = correlationId(exchange);
        GatewayErrorResponse body = new GatewayErrorResponse(
                status.value(), status.getReasonPhrase().toLowerCase().replace(' ', '_'),
                code.name(), message, correlationId);

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        if (wwwAuthenticate != null) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
        }
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            // Último recurso: nunca debería fallar serializando un record plano.
            bytes = ("{\"status\":" + status.value() + ",\"code\":\"" + code.name() + "\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static String correlationId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        return (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    }
}
