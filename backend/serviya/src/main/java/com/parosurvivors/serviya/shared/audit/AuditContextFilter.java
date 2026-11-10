package com.parosurvivors.serviya.shared.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Captura IP, user agent y {@code X-Request-Id} de cada petición en {@link SecurityAuditContext} para que la
 * auditoría pueda enriquecer sus eventos sin acoplar las capas de negocio al {@code HttpServletRequest}.
 * La IP real llega por {@code X-Forwarded-For} detrás del gateway ({@code forward-headers-strategy=native}),
 * así que {@code getRemoteAddr()} ya la resuelve. Limpia el contexto al salir (evita fugas entre peticiones
 * reutilizando el hilo del pool).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            SecurityAuditContext.set(request.getRemoteAddr(),
                    request.getHeader("User-Agent"), request.getHeader("X-Request-Id"));
            filterChain.doFilter(request, response);
        } finally {
            SecurityAuditContext.clear();
        }
    }
}
