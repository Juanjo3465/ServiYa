package com.parosurvivors.serviya.users.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que, si hay un header {@code Authorization: Bearer <jwt>} valido, puebla el
 * {@link SecurityContextHolder} con el userId (principal) y sus roles. Si no hay token o es
 * invalido, no autentica: las reglas de {@code SecurityConfig} decidiran si el endpoint exige login.
 *
 * <p>No es un {@code @Component}: lo instancia {@code SecurityConfig} para evitar el auto-registro
 * de Spring Boot que lo aplicaria tambien fuera de la cadena de Spring Security.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.resolve(token)
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        }
        filterChain.doFilter(request, response);
    }
}
