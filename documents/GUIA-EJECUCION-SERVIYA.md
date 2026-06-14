# Guía de ejecución del proyecto Serviya

Guía de referencia con los comandos para levantar, manejar y mantener el proyecto. El proyecto está compuesto por tres servicios: **backend** (Spring Boot), **mysql** (base de datos) y **frontend** (React + Vite).

---

## Comandos principales

Estos son los dos comandos que usarás el 90% del tiempo. Ambos levantan **todos los servicios en segundo plano** (`-d`).

### Desarrollo

Con hot-reload en el backend (Spring Boot DevTools) y en el frontend (Vite HMR):

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

### Producción

Backend como `.jar` optimizado y frontend compilado y servido por Nginx:

```bash
docker compose up -d --build
```

> El flag `-d` (detached) deja los contenedores corriendo en segundo plano y te devuelve la terminal. El `--build` reconstruye las imágenes; puedes omitirlo si no cambiaste nada que requiera reconstrucción.

---

## Acceso a los servicios

| Servicio | Desarrollo | Producción |
|---|---|---|
| Backend (API) | http://localhost:8080 | http://localhost:8080 |
| Frontend | http://localhost:5173 | http://localhost:8081 |
| MySQL (cliente externo) | localhost:3307 | localhost:3307 |

> Dentro de la red de Docker, el backend se conecta a la base de datos usando el host `mysql` y el puerto interno `3306` (no `localhost` ni `3307`).

---

## Ver estado y logs

| Acción | Comando |
|---|---|
| Ver qué contenedores están corriendo | `docker compose ps` |
| Ver los logs de todos los servicios | `docker compose logs -f` |
| Ver los logs de un servicio | `docker compose logs -f backend` |
| Ver las últimas 100 líneas | `docker compose logs --tail 100 backend` |

> El flag `-f` (follow) sigue mostrando los logs en vivo. Sal con `Ctrl+C` (esto solo cierra el visor de logs, **no** detiene los contenedores).

---

## Detener y reiniciar

| Acción | Comando |
|---|---|
| Detener sin eliminar | `docker compose stop` |
| Volver a arrancar lo detenido | `docker compose start` |
| Reiniciar un servicio | `docker compose restart backend` |
| Detener y eliminar los contenedores | `docker compose down` |
| Detener y eliminar **incluyendo la base de datos** | `docker compose down -v` |

> **Cuidado con `down -v`**: el `-v` elimina los volúmenes, lo que **borra todos los datos de MySQL**. Úsalo solo cuando quieras empezar la base de datos desde cero.

Si levantaste en **desarrollo**, usa los mismos flags para detener:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml down
```

---

## Reconstruir tras cambios de código

En **desarrollo** normalmente no necesitas reconstruir: el hot-reload refleja los cambios solo. Reconstruyes cuando cambias dependencias (`pom.xml`, `package.json`) o algún Dockerfile.

| Acción | Comando |
|---|---|
| Reconstruir y relanzar todo | `docker compose up -d --build` |
| Reconstruir solo un servicio | `docker compose up -d --build backend` |
| Reconstruir desde cero (sin caché) | `docker compose build --no-cache backend` |

---

## Base de datos

| Acción | Comando |
|---|---|
| Entrar a la consola de MySQL | `docker compose exec mysql mysql -u root -p` |
| Ejecutar un script manualmente | `docker compose exec -T mysql mysql -u root -p<password> <nombre_bd> < mysql/script.sql` |
| Re-ejecutar los scripts de inicio | `docker compose down -v` y luego volver a levantar |

> Los scripts de la carpeta `mysql/` se ejecutan **automáticamente solo la primera vez** que se crea el contenedor (cuando el volumen de datos está vacío). Para que se vuelvan a ejecutar hay que borrar el volumen con `down -v`.

---

## Entrar a un contenedor

Útil para inspeccionar o depurar desde adentro.

| Servicio | Comando |
|---|---|
| Backend | `docker compose exec backend bash` |
| Frontend (Nginx / Alpine) | `docker compose exec frontend sh` |
| MySQL | `docker compose exec mysql bash` |

---

## Verificaciones útiles

| Acción | Comando |
|---|---|
| Validar la configuración de Nginx (frontend en producción) | `docker compose exec frontend nginx -t` |
| Ver el consumo de recursos de los contenedores | `docker stats` |
| Listar las imágenes construidas | `docker images` |

---

## Limpieza

| Acción | Comando |
|---|---|
| Eliminar contenedores del proyecto | `docker compose down` |
| Eliminar imágenes, contenedores y redes sin usar | `docker system prune` |
| Lo anterior **incluyendo volúmenes** | `docker system prune --volumes` |

> `docker system prune` afecta a **todo Docker en tu máquina**, no solo a este proyecto. Úsalo con criterio.

---

## Resumen rápido

```bash
# Levantar en DESARROLLO (segundo plano, con hot-reload)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# Levantar en PRODUCCIÓN (segundo plano)
docker compose up -d --build

# Ver logs en vivo
docker compose logs -f

# Detener todo
docker compose down
```
