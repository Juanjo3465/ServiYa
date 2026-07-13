package com.parosurvivors.serviya.config;

import com.parosurvivors.serviya.shared.exception.handlers.RestAccessErrorHandler;
import com.parosurvivors.serviya.users.infrastructure.security.JwtAuthenticationFilter;
import com.parosurvivors.serviya.users.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final RestAccessErrorHandler restAccessErrorHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // permite al frontend (Vite :5173) llamar a la API
            .csrf(csrf -> csrf.disable()) // REST stateless API: no se usan cookies de sesion
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Autenticacion publica (login/registro/recuperacion) — RF-001/RF-002
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Documentacion OpenAPI/Swagger
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Perfil publico del oferente — RF-027: accesible tambien por visitantes (sin JWT).
                .requestMatchers(HttpMethod.GET, "/api/v1/offerers/*", "/api/v1/offerers/*/**").permitAll()
                // Perfil y cuenta del usuario autenticado — RF-005/006/008/010/011.
                // La identidad SIEMPRE sale del JWT (CurrentUser.id()), nunca del body/path: exige login.
                .requestMatchers("/api/v1/users/me/**").authenticated()
                // Modulo admin (modulo 9): gestion de usuarios/roles, detalle admin y feedback -> solo ADMIN.
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Acciones de moderacion sobre un reporte (warn/ban/revert-feedback/close/mark-not-provided) -> solo ADMIN.
                .requestMatchers("/api/v1/reports/*/actions/**").hasRole("ADMIN")
                // El resto se mantiene abierto por ahora (modulos aun no implementados)
                .anyRequest().permitAll()
            )
            // Sin token valido -> 401; token valido sin el rol -> 403. Cuerpo ErrorResponse uniforme
            // (mismo formato que GlobalExceptionHandler) via RestAccessErrorHandler. API stateless, sin login page.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAccessErrorHandler)
                .accessDeniedHandler(restAccessErrorHandler))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS para el frontend en desarrollo (Vite). Permite login/registro/perfil desde el navegador. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8081",
                "http://127.0.0.1:8081",
                "http://192.168.1.2:8081",
                "https://nonfervently-expiational-amani.ngrok-free.dev"
            ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
