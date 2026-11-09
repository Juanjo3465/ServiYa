package com.parosurvivors.gateway.ratelimit;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Techo global {@code api-default} portado al gateway (T3.9): mismo algoritmo (token bucket Lua atómico
 * sobre {@code redis-coord}), misma clave {@code rl:v1:{<tipo>:<id>}:api-default} y misma política que el
 * monolito (capacidad 200, 5/s, modo <b>OPEN</b>), para no tener dos sistemas de rate limiting (D-04). El
 * {@code GlobalRateLimitFilter} del monolito se retira en la Fase 4 (T4.9).
 *
 * <p>Reactivo (regla #7) y tras el mismo {@link CircuitBreaker} {@code redis-coord} que el chequeo de
 * {@code min_iat}: si la instancia cae, ambos cortan juntos. Modo <b>OPEN</b> → ante error o breaker
 * abierto se <b>deja pasar</b> (el techo global no debe tumbar la API), con métrica de alerta.</p>
 */
@Component
public class ReactiveRedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ReactiveRedisRateLimiter.class);
    private static final String POLICY = "api-default";
    private static final String METRIC = "gateway.ratelimit.decision";

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> TOKEN_BUCKET =
            RedisScript.of(new ClassPathResource("redis-scripts/ratelimit_token_bucket.lua"), List.class);

    private final ReactiveStringRedisTemplate redis;
    private final CircuitBreaker breaker;
    private final MeterRegistry meters;
    private final boolean enabled;
    private final long capacity;
    private final double refillPerSecond;

    public ReactiveRedisRateLimiter(
            ReactiveStringRedisTemplate redis,
            CircuitBreaker redisCoordCircuitBreaker,
            MeterRegistry meters,
            @Value("${serviya.ratelimit.enabled:true}") boolean enabled,
            @Value("${serviya.ratelimit.api-default.capacity:200}") long capacity,
            @Value("${serviya.ratelimit.api-default.refill-per-second:5}") double refillPerSecond) {
        this.redis = redis;
        this.breaker = redisCoordCircuitBreaker;
        this.meters = meters;
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    public static String key(String subjectType, String subjectId) {
        return "rl:v1:{" + subjectType + ":" + subjectId + "}:" + POLICY;
    }

    @SuppressWarnings("unchecked")
    public Mono<RateLimitDecision> tryConsume(String subjectType, String subjectId) {
        if (!enabled) {
            return Mono.just(RateLimitDecision.allow());
        }
        String key = key(subjectType, subjectId);
        // args como List (no varargs): overload inequívoco y fácil de stubear en tests.
        List<String> args = List.of(Long.toString(capacity), Double.toString(refillPerSecond), "1");
        return Mono.defer(() -> redis.execute(TOKEN_BUCKET, List.of(key), args).next())
                .transformDeferred(CircuitBreakerOperator.of(breaker))
                .map(this::toDecision)
                .onErrorResume(ex -> {
                    // api-default es OPEN: error de Redis o breaker abierto ⇒ se deja pasar, con alerta.
                    count(ex instanceof CallNotPermittedException ? "breaker-open" : "error");
                    log.warn("Rate limit sin redis-coord ({}); fail-open (OPEN)", ex.getClass().getSimpleName());
                    return Mono.just(RateLimitDecision.allow());
                });
    }

    private RateLimitDecision toDecision(List<?> result) {
        boolean allowed = !result.isEmpty() && asLong(result.get(0)) == 1L;
        long retryMs = result.size() > 2 ? asLong(result.get(2)) : 0L;
        count(allowed ? "allowed" : "rejected");
        if (allowed) {
            return RateLimitDecision.allow();
        }
        long retrySeconds = Math.max(1L, (long) Math.ceil(retryMs / 1000.0));
        return RateLimitDecision.rejected(retrySeconds);
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private void count(String result) {
        meters.counter(METRIC, "policy", POLICY, "result", result).increment();
    }
}
