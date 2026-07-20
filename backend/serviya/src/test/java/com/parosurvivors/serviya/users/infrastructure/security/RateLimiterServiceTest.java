package com.parosurvivors.serviya.users.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parosurvivors.serviya.shared.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Comportamiento del contador de cuotas: agota, aisla por sujeto y por politica, e informa cuanto
 * esperar. Ejercita Bucket4j de verdad (no hay mocks), asi que tambien verifica que la configuracion
 * de los baldes es la que creemos.
 */
class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RateLimiterService();
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    private void consume(RateLimitPolicy policy, String subject, int times) {
        for (int i = 0; i < times; i++) {
            service.consumeOrThrow(policy, subject);
        }
    }

    @Test
    void allowsExactlyTheConfiguredCapacityAndThenBlocks() {
        RateLimitPolicy policy = RateLimitPolicy.PASSWORD_RESET_BY_EMAIL;

        // La cuota completa debe pasar sin rechistar...
        assertThatCode(() -> consume(policy, "victima@example.com", (int) policy.capacity()))
                .doesNotThrowAnyException();

        // ...y la siguiente peticion ya no.
        assertThatThrownBy(() -> service.consumeOrThrow(policy, "victima@example.com"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void tellsTheClientHowLongToWait() {
        RateLimitPolicy policy = RateLimitPolicy.PASSWORD_RESET_BY_EMAIL;
        consume(policy, "victima@example.com", (int) policy.capacity());

        RateLimitExceededException ex = catchRateLimit(policy, "victima@example.com");

        // Nunca 0: devolver 0 invitaria a reintentar de inmediato y volver a fallar.
        assertThat(ex.getRetryAfterSeconds()).isPositive();
        assertThat(ex.getRetryAfterSeconds()).isLessThanOrEqualTo(policy.window().toSeconds());
    }

    @Test
    void quotasAreIndependentPerSubject() {
        RateLimitPolicy policy = RateLimitPolicy.PASSWORD_RESET_BY_EMAIL;
        consume(policy, "victima@example.com", (int) policy.capacity());

        // Bombardear a una victima no debe dejar sin servicio al resto de usuarios.
        assertThatCode(() -> service.consumeOrThrow(policy, "otro@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void quotasAreIndependentPerPolicyEvenForTheSameSubject() {
        // Mismo sujeto textual en dos politicas: los prefijos de clave evitan que compartan balde.
        consume(RateLimitPolicy.PASSWORD_RESET_BY_EMAIL, "colision",
                (int) RateLimitPolicy.PASSWORD_RESET_BY_EMAIL.capacity());

        assertThatCode(() -> service.consumeOrThrow(RateLimitPolicy.LOGIN_BY_EMAIL, "colision"))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNothingWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        RateLimitPolicy policy = RateLimitPolicy.PASSWORD_RESET_BY_EMAIL;

        assertThatCode(() -> consume(policy, "victima@example.com", (int) policy.capacity() * 5))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresAnAbsentSubjectInsteadOfSharingOneBucket() {
        // Un sujeto nulo/vacio (p.ej. correo ausente) NO debe caer en un balde comun: eso convertiria
        // el limite en una negacion de servicio para todos los que llegasen sin ese dato.
        assertThatCode(() -> {
            for (int i = 0; i < 50; i++) {
                service.consumeOrThrow(RateLimitPolicy.LOGIN_BY_EMAIL, null);
                service.consumeOrThrow(RateLimitPolicy.LOGIN_BY_EMAIL, "  ");
            }
        }).doesNotThrowAnyException();
    }

    private RateLimitExceededException catchRateLimit(RateLimitPolicy policy, String subject) {
        try {
            service.consumeOrThrow(policy, subject);
            throw new AssertionError("Se esperaba RateLimitExceededException");
        } catch (RateLimitExceededException e) {
            return e;
        }
    }
}
