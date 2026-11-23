package contracts.jwks

import org.springframework.cloud.contract.spec.Contract

/**
 * Contrato del JWKS (T6.20 / contrato 4.1 de la guía de auth).
 *
 * El emisor (hoy el monolito; en Fase 8A Slice 1 pasará a identity-service) publica en
 * GET /.well-known/jwks.json una clave pública ES256 con la forma que esperan los VERIFICADORES
 * (gateway, notif, chat): kty=EC, crv=P-256, alg=ES256, use=sig, con kid, x e y; NUNCA el parámetro
 * privado 'd'. Si el productor rompe esta forma, los consumidores dejarían de poder validar tokens.
 */
Contract.make {
    description("El JWKS publica una clave pública ES256 (kty=EC, crv=P-256, alg=ES256, use=sig, kid, x, y) con Cache-Control cacheable.")
    request {
        method 'GET'
        url '/.well-known/jwks.json'
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
            header('Cache-Control', $(consumer('max-age=300, public'), producer(regex('.*max-age=300.*'))))
        }
        body([
            keys: [[
                kty: 'EC',
                crv: 'P-256',
                alg: 'ES256',
                use: 'sig',
                kid: $(consumer('test-key-1'), producer(regex('.+'))),
                x  : $(consumer('MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE'), producer(regex('.+'))),
                y  : $(consumer('MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE'), producer(regex('.+')))
            ]]
        ])
    }
}
