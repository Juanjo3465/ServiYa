# infra/redis/acl/users.coord.acl.tpl — plantilla VERSIONADA (el users.coord.acl generado NO se versiona)
# Render:  ./render-acl.sh users.coord.acl.tpl users.coord.acl   (con las contraseñas exportadas en el entorno)
# Se guardan HASHES SHA-256 (#<hash>), nunca la contraseña en claro.
user default off
user admin on #${ADMIN_HASH} ~* &* +@all
user healthcheck on #${HEALTHCHECK_HASH} +ping
user monitor on #${MONITOR_HASH} ~* +@connection +ping +info +client|list +slowlog +latency +config|get +memory|usage +scan +type +ttl +pttl +strlen
# notifications-service: SOLO comprueba que redis-coord responde (modo degradado D-07/1a). Sin acceso a claves.
user svc-notifications-coord on #${SVC_NOTIFICATIONS_COORD_HASH} resetkeys resetchannels +@connection +ping
# ~auth:min_iat:* lo escribe el monolito (emisor de identidad) en Fase 2B; el gateway lo LEE con su propio
# usuario ACL (Fase 3). El SET con TTL viaja como `SET key val PX <ms>` → cubierto por +set (no hace falta +psetex).
user svc-serviya-coord on #${SVC_SERVIYA_COORD_HASH} ~lock:* ~fence:* ~rl:* ~auth:min_iat:* +@connection +ping +get +set +del +incr +pexpire +pttl +hmget +hset +time +zadd +zcard +zrange +zremrangebyscore +eval +evalsha +script|load +script|exists
