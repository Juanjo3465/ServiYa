# Lab 04 — After evidence (Secure Channel implemented)

Captured on `feat/lab04-secure-channel`, full stack running via
`docker compose -p serviya-lab04 up -d --build` (current `main` + Lab 04 changes),
self-signed dev certificate generated at container start into the `frontend_certs`
volume.

## 1. The certificate is generated outside the image

```console
$ docker logs serviya-frontend 2>&1 | grep -E "dev-tls|subject|notAfter"
/docker-entrypoint.sh: Launching /docker-entrypoint.d/40-dev-tls.sh
[dev-tls] generating self-signed dev certificate (30 days) in /etc/nginx/certs
[dev-tls] certificate ready:
subject=O=ServiYa Lab 04, CN=localhost
notAfter=Dec 27 04:16:48 2026 GMT
```

The key is written to `/etc/nginx/certs/dev.key` inside the `frontend_certs` Docker
volume (mode 600): it never enters an image layer and is never committed.

## 2. Plain HTTP is rejected (denied path)

```console
$ curl -sI http://localhost:8081/ | head -2
HTTP/1.1 301 Moved Permanently
Server: nginx/1.31.10

$ curl -sI http://localhost:8081/ | grep -i '^location'
Location: https://localhost:8443/
```

The intended route through HTTP is therefore not usable: it can only precede a new
HTTPS request.

## 3. TLS negotiation (legitimate path)

Trust setup: the dev certificate is exported from the container and used as a CA
file; `curl -k` is deliberately **not** used.

```console
$ docker compose -p serviya-lab04 cp frontend:/etc/nginx/certs/dev.crt ./docs/lab04/dev.crt

$ curl -s -o /dev/null -w 'GET / -> %{http_code} over %{scheme} HTTP/%{http_version} verify=%{ssl_verify_result}\n' \
    --cacert ./docs/lab04/dev.crt https://localhost:8443/
GET / -> 200 over https HTTP/2 verify=0

$ openssl s_client -connect localhost:8443 -servername localhost </dev/null 2>/dev/null \
    | grep -E "Protocol|Cipher is"
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Protocol: TLSv1.3

$ curl -sI --cacert ./docs/lab04/dev.crt https://localhost:8443/ | grep -i strict-transport
strict-transport-security: max-age=300
```

`verify=0` means the chain and hostname were validated against the exported
certificate (SAN `DNS:localhost`). This certificate is for the local test
environment only.

## 4. Legitimate authenticated flow through TLS

```console
$ curl -s -o /dev/null -w '%{http_code}\n' --cacert ./docs/lab04/dev.crt \
    https://localhost:8443/api/v1/categories
200

$ # register (fresh throwaway account; token redacted)
$ curl -s -o /dev/null -w '%{http_code}\n' --cacert ./docs/lab04/dev.crt \
    -X POST https://localhost:8443/api/v1/auth/register \
    -H 'Content-Type: application/json' -d '{...}'
201

$ # authenticated request with the access token
$ curl -s -o /dev/null -w '%{http_code}\n' --cacert ./docs/lab04/dev.crt \
    -H "Authorization: Bearer <redacted>" https://localhost:8443/api/v1/users/me/profile
200

$ # authenticated notifications endpoint (gateway -> notifications-service)
$ curl -s -o /dev/null -w '%{http_code}\n' --cacert ./docs/lab04/dev.crt \
    -H "Authorization: Bearer <redacted>" https://localhost:8443/api/v1/notifications
200
```

## 5. Refresh cookie carries `Secure`

With `JWT_REFRESH_COOKIE_SECURE=true` (now the base-compose default):

```console
$ curl -s -D - -o /dev/null --cacert ./docs/lab04/dev.crt \
    -X POST https://localhost:8443/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"<throwaway account>","password":"<redacted>"}' | grep -i '^set-cookie'
set-cookie: serviya_refresh=<redacted>; Path=/api/v1/auth; Max-Age=2591999; \
            Expires=Sun, 27 Dec 2026 04:20:37 GMT; Secure; HttpOnly; SameSite=Strict
```

And the refresh flow works with that cookie (proves the flag does not break sessions
when the edge is HTTPS):

```console
$ curl -s -o /dev/null -w '%{http_code}\n' --cacert ./docs/lab04/dev.crt \
    -X POST -H "Cookie: serviya_refresh=<redacted>" https://localhost:8443/api/v1/auth/refresh
200
```

## Summary

| Element | Expected | Observed |
|---|---|---|
| Plain HTTP at the edge | rejected | `301` → `https://localhost:8443/` |
| Negotiated protocol | TLS 1.2/1.3 | `TLSv1.3`, `TLS_AES_256_GCM_SHA384` |
| Certificate | validated, not `-k` | `verify=0` with `--cacert` |
| HSTS | present | `max-age=300` |
| Legitimate request | 200 | 200 over HTTPS/2 (public + authenticated endpoints) |
| Refresh cookie | `Secure` | `Secure; HttpOnly; SameSite=Strict` (login 200, refresh 200) |
| Private key in git/image | never | generated at runtime in a volume |

### Environment note

The stack used for this capture also logs an unrelated pre-existing issue: the
monolith's outbox relay fails with `NOT_FOUND - no exchange 'serviya.users'` because
that exchange is not declared in this local RabbitMQ. It does not affect the Secure
Channel scenario (auth, catalog, profile and notifications endpoints all return 2xx).
