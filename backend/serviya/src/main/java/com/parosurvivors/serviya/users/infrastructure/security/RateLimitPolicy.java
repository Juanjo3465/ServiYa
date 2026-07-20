package com.parosurvivors.serviya.users.infrastructure.security;

import java.time.Duration;

/**
 * Cuotas de los endpoints publicos de autenticacion (RNF de abuso). Cada politica es una ventana
 * independiente: una peticion puede consumir de varias a la vez (p. ej. login gasta cuota de IP y de
 * correo), y agotar cualquiera de ellas corta la peticion.
 *
 * <p>Los limites por IP frenan al atacante que ataca muchas cuentas desde un sitio; los limites por
 * CORREO frenan al que ataca UNA cuenta repartiendo el trafico entre muchas IPs (botnet, proxies
 * rotatorios), que es justo lo que a un limite por IP se le escapa.</p>
 *
 * <p>Se recargan por INTERVALO y no de forma continua: la cuota se repone entera al cerrarse la
 * ventana. Es lo que espera alguien que lee "5 intentos cada 15 minutos", y evita que un atacante
 * paciente mantenga un goteo constante de intentos.</p>
 */
public enum RateLimitPolicy {

    /** Fuerza bruta de credenciales desde un mismo origen. */
    LOGIN_BY_IP("login-ip", 10, Duration.ofMinutes(15)),

    /** Fuerza bruta contra UNA cuenta concreta repartida entre varias IPs. */
    LOGIN_BY_EMAIL("login-email", 5, Duration.ofMinutes(15)),

    /** Alta masiva de cuentas (y el correo de bienvenida que arrastra cada una). */
    REGISTER_BY_IP("register-ip", 5, Duration.ofMinutes(60)),

    /** Bombardeo de correos de recuperacion y sondeo de enumeracion por volumen. */
    PASSWORD_RESET_BY_IP("reset-ip", 5, Duration.ofMinutes(15)),

    /**
     * Bombardeo de la bandeja de UNA victima desde muchas IPs. Es el mas estricto: mas alla de tres
     * enlaces por hora no hay uso legitimo, y cada envio consume cuota de Brevo.
     */
    PASSWORD_RESET_BY_EMAIL("reset-email", 3, Duration.ofMinutes(60)),

    /** Fuerza bruta sobre el token de un solo uso. */
    PASSWORD_RESET_CONFIRM_BY_IP("reset-confirm-ip", 10, Duration.ofMinutes(15)),

    /**
     * Sondeo de tokens por la via barata. Mas holgado que el confirm porque la vista del frontend lo
     * llama sola al montarse y un usuario puede recargar la pagina varias veces.
     */
    PASSWORD_RESET_VALIDATE_BY_IP("reset-validate-ip", 20, Duration.ofMinutes(15));

    private final String keyPrefix;
    private final long capacity;
    private final Duration window;

    RateLimitPolicy(String keyPrefix, long capacity, Duration window) {
        this.keyPrefix = keyPrefix;
        this.capacity = capacity;
        this.window = window;
    }

    public long capacity() {
        return capacity;
    }

    public Duration window() {
        return window;
    }

    /** Clave del balde: politica + sujeto. El prefijo evita que una IP y un correo iguales colisionen. */
    public String bucketKey(String subject) {
        return keyPrefix + ":" + subject;
    }
}
