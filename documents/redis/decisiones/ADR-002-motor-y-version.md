# ADR-002: Motor y versión de Redis

- **Estado:** Aceptado (motor); patch exacto **pendiente** de fijar al implementar
- **Fecha:** 2026-10-10
- **Decisión relacionada:** D-02

## Contexto

Hay que elegir motor y una **versión fija** (nada de `latest`). El stack actual es Spring Boot 3.5.5 /
Java 21, que gestiona `spring-boot-starter-data-redis` vía su BOM (cliente **Lettuce 6.5.x**). A futuro
es posible un servicio gestionado en la nube.

## Opciones consideradas

1. **Redis** (FOSS **AGPLv3** desde la v8) — pros: ecosistema y documentación de referencia; imagen
   oficial. Contras: licencia AGPL (irrelevante para uso self-hosted/interno).
2. **Valkey** (fork de la Linux Foundation, drop-in de Redis 7.2) — pros: BSD, adoptado por algunos
   gestionados. Contras: ecosistema algo más nuevo.

## Decisión

**Redis**, línea **`8.2-alpine`**. El **patch exacto** se fija al implementar (Fase 1), verificando la
última versión parcheada. Compatible con Lettuce 6.5.x (BOM de Spring Boot 3.5.5) y Java 21 (RESP2/RESP3).

## Consecuencias

Ecosistema y documentación maduros. Como el proyecto usa solo **comandos estándar** y scripts Lua
propios (sin módulos propietarios), **Valkey queda como alternativa drop-in** si un gestionado lo
favorece. **Revisar** al decidir topología/gestionado (D-16/D-17, Fase 8).
