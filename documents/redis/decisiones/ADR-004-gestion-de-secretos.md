# ADR-004: Gestión de secretos de Redis

- **Estado:** Aceptado (local); gestor de producción **pendiente**
- **Fecha:** 2026-10-10
- **Decisión relacionada:** D-14

## Contexto

Las contraseñas de los usuarios ACL de Redis (y las conexiones de la app) **no pueden** ir al repositorio
ni horneadas en la imagen. Hay dos horizontes: desarrollo local y producción.

## Opciones consideradas

1. **Local:** archivo `.env` no versionado (ya es la convención del repo) vs. gestor de secretos pesado.
2. **Producción:** Vault / AWS Secrets Manager / Azure Key Vault / GCP Secret Manager.

## Decisión

**Local:** `.env` **no versionado**. Se versiona solo `infra/redis/.env.example` (nombres, sin valores)
y la plantilla `users.acl.tpl`; el `users.acl` **generado** (con **hashes SHA-256**, nunca claves en
claro) **no se versiona**. **Producción:** gestor de secretos **PENDIENTE** — se decide en la Fase 7.

## Consecuencias

Simple y reproducible en local; sin secretos en el repo ni en la imagen. Quedan **pendientes** para prod
la rotación (las ACL permiten dos contraseñas activas a la vez) y la integración con un gestor.
**Revisar** en la Fase 7 (endurecimiento) junto con TLS (D-15).
