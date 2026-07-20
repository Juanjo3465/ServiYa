package com.parosurvivors.serviya.users.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parosurvivors.serviya.shared.dto.ErrorResponse;
import com.parosurvivors.serviya.shared.exceptions.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Aplica la cuota POR IP a los endpoints publicos de {@code /api/v1/auth/**} antes de que la peticion
 * llegue a ningun sitio.
 *
 * <p>Va delante del filtro JWT a proposito: frena al atacante en la puerta, sin gastar ciclos validando
 * tokens ni consultando la base de datos en peticiones que van a rechazarse igualmente. Como estos
 * endpoints son publicos y no hay usuario autenticado del que tirar, la IP es el unico sujeto disponible
 * aqui; la cuota por CORREO se aplica mas adelante, en el controlador, donde el cuerpo ya esta parseado
 * (leerlo dentro del filtro consumiria el InputStream y rompeeria la lectura posterior).</p>
 *
 * <p>No es un {@code @Component}: lo instancia {@code SecurityConfig}, igual que
 * {@link JwtAuthenticationFilter}, para que no se auto-registre fuera de la cadena de Spring Security.</p>
 */
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    /** Ruta -> politica aplicable. Lo que no este aqui no lleva cuota por IP. */
    private static final Map<String, RateLimitPolicy> POLICIES_BY_PATH = Map.of(
            "/api/v1/auth/login", RateLimitPolicy.LOGIN_BY_IP,
            "/api/v1/auth/register", RateLimitPolicy.REGISTER_BY_IP,
            "/api/v1/auth/password-reset", RateLimitPolicy.PASSWORD_RESET_BY_IP,
            "/api/v1/auth/password-reset/validate", RateLimitPolicy.PASSWORD_RESET_VALIDATE_BY_IP,
            "/api/v1/auth/password-reset/confirm", RateLimitPolicy.PASSWORD_RESET_CONFIRM_BY_IP);

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    /** Solo se ejecuta en /api/v1/auth/**; el resto de la API ni siquiera lo atraviesa. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitPolicy policy = POLICIES_BY_PATH.get(request.getRequestURI());
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            rateLimiterService.consumeOrThrow(policy, clientIp(request));
        } catch (RateLimitExceededException ex) {
            // Estamos ANTES del DispatcherServlet: el @RestControllerAdvice no ve esta excepcion,
            // asi que la respuesta se escribe aqui con el mismo formato (igual que RestAccessErrorHandler).
            writeTooManyRequests(request, response, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * IP del cliente. Tras un proxy o balanceador, {@code getRemoteAddr()} devolveria la del proxy y todo
     * el trafico compartiria un unico balde, asi que se prefiere la primera entrada de
     * {@code X-Forwarded-For} (la del cliente original; las siguientes son los proxies encadenados).
     *
     * <p>OJO: esa cabecera la puede falsificar el cliente si el backend queda expuesto directamente. Solo
     * es de fiar cuando hay delante un proxy que la reescribe. Con la cabecera falsificable, un atacante
     * podria darse un balde nuevo en cada peticion — por eso las cuotas por CORREO del controlador, que no
     * dependen de ningun dato manipulable, son la defensa que de verdad sostiene el flujo de reset.</p>
     */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                      RateLimitExceededException ex) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
