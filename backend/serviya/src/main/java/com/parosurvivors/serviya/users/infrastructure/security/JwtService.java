package com.parosurvivors.serviya.users.infrastructure.security;

import com.parosurvivors.serviya.users.application.dto.result.IssuedToken;
import com.parosurvivors.serviya.users.application.ports.output.TokenProviderPort;
import com.parosurvivors.serviya.users.domain.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link TokenProviderPort} emitiendo access tokens <b>ES256</b>
 * (jjwt; Fase 1 de {@code guia-implementacion-auth.md}). Reemplaza la firma HS256 con secreto compartido:
 * ahora SOLO el emisor firma (con la clave privada de {@link JwtKeys}) y los verificadores usan la pública
 * (publicada en el JWKS, {@link com.parosurvivors.serviya.users.infrastructure.adapters.input.JwksController}).
 *
 * <p>El token lleva los claims del contrato (§4.1): header {@code kid}+{@code typ}, y
 * {@code iss}/{@code aud}/{@code sub}/{@code roles}/{@code jti}/{@code iat}/{@code exp}. El {@code sub} es el
 * userId; {@code roles} la lista de roles.
 *
 * <p>{@link #resolve(String)} se mantiene <b>temporalmente</b> (T1.6) para el {@link JwtAuthenticationFilter}
 * del propio monolito, verificando ahora con la clave pública ES256. Se eliminará cuando el monolito pase a
 * resource server estándar (Fase 4).
 */
@Component
public class JwtService implements TokenProviderPort {

    private final JwtKeys keys;
    private final JwtProperties properties;

    public JwtService(JwtKeys keys, JwtProperties properties) {
        this.keys = keys;
        this.properties = properties;
    }

    @Override
    public IssuedToken issue(Long userId, List<RoleName> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.getAccessTtl());
        List<String> roleNames = roles == null ? List.of() : roles.stream().map(Enum::name).toList();

        String token = Jwts.builder()
                .header().keyId(keys.keyId()).type(properties.getTyp()).and()
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(String.valueOf(userId))
                .claim("roles", roleNames)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(keys.privateKey(), Jwts.SIG.ES256)
                .compact();

        return new IssuedToken(token, LocalDateTime.ofInstant(expiration, ZoneId.systemDefault()));
    }

    /**
     * Valida firma ES256 (clave pública), emisor y expiración, y construye un {@link Authentication} con el
     * userId como principal y los roles como authorities ({@code ROLE_<NAME>}). Vacío si el token es inválido,
     * expiró o no es de este emisor.
     */
    @SuppressWarnings("unchecked")
    public Optional<Authentication> resolve(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keys.publicKey())
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.valueOf(claims.getSubject());
            List<String> roles = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            return Optional.of(new UsernamePasswordAuthenticationToken(userId, null, authorities));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
