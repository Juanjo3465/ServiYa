package contracts.auth

import org.springframework.cloud.contract.spec.Contract

/**
 * Contrato del borde del login (T6.19 / contrato 4.1–4.2 de la guía de auth).
 *
 * POST /api/v1/auth/login devuelve el access token (JWT Bearer) en el CUERPO y fija el refresh token en
 * una cookie HttpOnly (nunca en el cuerpo). Este es el contrato que consume el frontend (api.js: lee
 * `token`, envía la cookie en /auth/refresh). Si el productor cambiara la forma (p. ej. mover el refresh
 * al cuerpo, o quitar `token`/`tokenType`), rompería al consumidor.
 *
 * Nota: aquí se fija la FORMA, no la emisión real del token (firma ES256), que cubre el contrato del JWKS
 * (shouldPublishEs256Jwks) + los tests de emisión (JwtServiceTest).
 */
Contract.make {
    description("POST /auth/login devuelve el access token (JWT Bearer) en el cuerpo y fija el refresh en una cookie HttpOnly.")
    request {
        method 'POST'
        url '/api/v1/auth/login'
        headers {
            contentType(applicationJson())
        }
        body(
            email: 'user@example.com',
            password: 'Secret123!'
        )
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
            header('Set-Cookie', $(consumer('serviya_refresh=rt; Path=/api/v1/auth; HttpOnly; SameSite=Strict'),
                    producer(regex('serviya_refresh=.+'))))
        }
        body(
            token: $(consumer('eyJ.sample.jwt'),
                    producer(regex('[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+'))),
            tokenType: 'Bearer'
        )
    }
}
