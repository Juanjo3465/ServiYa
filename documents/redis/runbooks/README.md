# Runbooks de Redis — ServiYa

Procedimientos operativos. **Se escriben en la Fase 7** (endurecimiento para producción):

- `redis-caido.md` — qué hacer si una instancia no responde.
- `memoria-llena.md` — OOM / expulsiones inesperadas.
- `rotar-credenciales.md` — rotación de contraseñas ACL (dos activas a la vez).
- `restaurar-backup.md` — restauración del RDB de `redis-coord`.
- `actualizar-version.md` — actualización de la imagen (incluye avisos de seguridad del motor Lua).
