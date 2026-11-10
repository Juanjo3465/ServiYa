# ADR-0014: Ruta pública `/api/v1/chat/**`

- **Estado:** Aceptado
- **Fecha:** 2026-11-09

## Contexto
La guía usa `/api/chat/v1/**`. El gateway y todos los servicios de ServiYa versionan como `/api/v1/<recurso>`.

## Decisión
REST en `/api/v1/chat/...` y WebSocket en `/api/v1/chat/ws`. El gateway enruta `/api/v1/chat` y `/api/v1/chat/**`
a `chat-service` antes del comodín del monolito, y reenvía el upgrade WebSocket con su query string (lo verifica
`ApiGatewayWebSocketTest`).

## Consecuencias
Las rutas de la guía (§8) cambian solo de prefijo. Una v2 incompatible sería `/api/v2/chat/...`.
