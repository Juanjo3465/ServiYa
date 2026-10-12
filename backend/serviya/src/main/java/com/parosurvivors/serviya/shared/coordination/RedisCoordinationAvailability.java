package com.parosurvivors.serviya.shared.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Comprueba la disponibilidad de {@code redis-coord} con un {@code PING}, cacheando el resultado unos
 * segundos para no añadir una ida y vuelta a Redis en cada consulta (p. ej. por cada envío de correo).
 * Ante cualquier error de conexión devuelve {@code false} (se asume caído). Reusa la misma fábrica de
 * conexión que el health indicator, así que respeta los timeouts cortos configurados.
 */
@Component
public class RedisCoordinationAvailability implements CoordinationAvailability {

    private static final Logger log = LoggerFactory.getLogger(RedisCoordinationAvailability.class);

    private final LettuceConnectionFactory factory;
    private final long cacheMillis;

    private volatile long lastCheckAt = 0L;
    private volatile boolean lastResult = true;

    public RedisCoordinationAvailability(
            @Qualifier("coordRedisConnectionFactory") LettuceConnectionFactory coordRedisConnectionFactory,
            @Value("${app.ratelimit.coordination-probe-cache-ms:5000}") long cacheMillis) {
        this.factory = coordRedisConnectionFactory;
        this.cacheMillis = cacheMillis;
    }

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastCheckAt < cacheMillis) {
            return lastResult;
        }
        boolean available;
        try (RedisConnection conn = factory.getConnection()) {
            available = "PONG".equalsIgnoreCase(conn.ping());
        } catch (RuntimeException ex) {
            log.debug("Sondeo de redis-coord falló; se considera no disponible", ex);
            available = false;
        }
        lastResult = available;
        lastCheckAt = now;
        return available;
    }
}
