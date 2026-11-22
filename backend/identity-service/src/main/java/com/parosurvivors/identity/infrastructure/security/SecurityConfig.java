package com.parosurvivors.identity.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad PLACEHOLDER del Slice 0 (andamiaje): sin endpoints propios todavía, permite todo para que el
 * servicio arranque y exponga el actuator. Se sustituye en el Slice 1+ cuando se muevan los endpoints de
 * auth/cuenta: identity pasará a ser emisor + resource server (verifica su propio access token ES256 con la
 * clave pública) con la misma matriz de autorización que traía el monolito.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
