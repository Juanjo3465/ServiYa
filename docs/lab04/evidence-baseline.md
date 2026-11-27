# Lab 04 — Baseline evidence (before the change)

Captured **before** applying the Lab 04 changes, with the pre-change edge (nginx
listening on port 80 only) and the application running from `main` (no TLS anywhere).

Environment: local Docker Compose. The monolith container in that environment was
crash-looping because the local `.env` was missing, so the API-level response could
not be produced; the original exposure is established by the two observations below.

## 1. The edge serves plain HTTP

```console
$ curl -s -o /dev/null -w 'GET / -> %{http_code} over %{scheme}\n' http://localhost:8081/
GET / -> 200 over http

$ curl -sI http://localhost:8081/ | head -3
HTTP/1.1 200 OK
Server: nginx/1.31.8
Date: Fri, 27 Nov 2026 03:47:34 GMT
```

No TLS listener, no redirect, no `Strict-Transport-Security` header.

## 2. The refresh cookie was not marked `Secure`

The attribute is driven by `JWT_REFRESH_COOKIE_SECURE`
(`backend/serviya/src/main/resources/application.yaml:184`), whose pre-change default
was `false` because the edge was HTTP-only (`docker-compose.yml`, old comment:
*"en dev http se queda false para que la cookie viaje"*). The backend process was
running with the variable unset, so the application default applied:

```console
$ docker exec serviya-backend printenv JWT_REFRESH_COOKIE_SECURE
# (no output -> application default: false)
```

Therefore every login/register response emitted the refresh cookie **without**
`Secure`, i.e. a cleartext-capable cookie. After the change, the same login emits
`Secure; HttpOnly; SameSite=Strict` over HTTPS (see
[`evidence-after.md`](evidence-after.md) §5).

## Baseline verdict

| Aspect | Baseline |
|---|---|
| Transport | plain HTTP on `:8081` |
| Protocol offered by the edge | HTTP/1.1 only |
| HSTS | absent |
| Refresh cookie | no `Secure` attribute (flag `false`) |
| Credentials (password, access tokens) | sent/received in cleartext on the LAN |
