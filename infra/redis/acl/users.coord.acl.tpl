# infra/redis/acl/users.coord.acl.tpl — plantilla VERSIONADA (el users.coord.acl generado NO se versiona)
# Render:  ./render-acl.sh users.coord.acl.tpl users.coord.acl   (con las contraseñas exportadas en el entorno)
# Se guardan HASHES SHA-256 (#<hash>), nunca la contraseña en claro.
user default off
user admin on #${ADMIN_HASH} ~* &* +@all
user healthcheck on #${HEALTHCHECK_HASH} +ping
user monitor on #${MONITOR_HASH} ~* +@connection +ping +info +client|list +slowlog +latency +config|get +memory|usage +scan +type +ttl +pttl +strlen
user svc-serviya-coord on #${SVC_SERVIYA_COORD_HASH} ~lock:* ~fence:* ~rl:* +@connection +ping +get +set +del +incr +pexpire +pttl +hmget +hset +eval +evalsha +script|load +script|exists
