package com.parosurvivors.chat.realtime.infrastructure;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Métricas de dominio de tiempo real del chat (M41(a) / T6.2, observabilidad). Publica como gauge Prometheus
 * {@code chat.ws.connected_users}: el número de usuarios con al menos una sesión WebSocket activa en ESTA
 * instancia (lo expone {@link ConnectionRegistry#connectedUsers()}). Es un indicador clave de carga del chat
 * en tiempo real; junto con {@code chat.outbox.events.published} (throughput de eventos/mensajes al bus) da
 * la foto de dominio del servicio.
 */
@Configuration
class ChatRealtimeMetrics {

    ChatRealtimeMetrics(MeterRegistry registry, ConnectionRegistry connections) {
        Gauge.builder("chat.ws.connected_users", connections, ConnectionRegistry::connectedUsers)
                .description("Usuarios con al menos una sesión WebSocket activa en esta instancia")
                .register(registry);
    }
}
