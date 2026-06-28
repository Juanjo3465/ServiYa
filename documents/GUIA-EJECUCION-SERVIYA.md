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

> **En desarrollo el backend tarda en estar listo — espera antes de probar.** A
> diferencia de producción (que arranca un `.jar` ya compilado en segundos), en
> desarrollo el backend corre `mvn spring-boot:run` sobre el código montado: tiene que
> resolver dependencias (el primer arranque baja todo Maven a `maven_repo`, varios
> minutos), **compilar** y recién entonces levantar Spring Boot. Hasta que no veas
> `Started ServiyaApplication in X seconds` en el log, el puerto `8080` **no acepta
> conexiones** y Postman/el navegador darán "no response" (no es un bug). Espera a esa
> línea siguiendo el log:
>
> ```bash
> docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f backend
> ```
>
> (sales del visor con `Ctrl+C`, sin detener el contenedor).

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
| Ver **todos**, incluidos los caídos (con su exit code) | `docker compose ps -a` |
| Ver por qué se cayó un contenedor | `docker inspect -f '{{.State.Status}} exit={{.State.ExitCode}}' serviya-mysql` |
| Ver los logs de todos los servicios | `docker compose logs -f` |
| Ver los logs de un servicio | `docker compose logs -f backend` |
| Ver las últimas 100 líneas | `docker compose logs --tail 100 backend` |

> El flag `-f` (follow) sigue mostrando los logs en vivo. Sal con `Ctrl+C` (esto solo cierra el visor de logs, **no** detiene los contenedores).

> **Si un contenedor aparece como `Exited (1)`** y no como `Up`, algo falló al
> arrancar. Mira el final de su log (`docker compose logs --tail 40 <servicio>`) para
> ver el error exacto. En el caso de `mysql`, un script de init que falla (p. ej. por
> una foreign key) aborta toda la inicialización y deja la BD a medias (ver la sección
> "Base de datos").

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

## Añadir o actualizar dependencias (backend y frontend)

Cuando cambias las dependencias hay que **relanzar el contenedor afectado** para que se
apliquen. Cómo hacerlo depende del servicio y del modo (dev/prod), porque cada uno resuelve
sus dependencias de forma distinta.

### Backend (`pom.xml`)

Tras añadir/actualizar una dependencia en `backend/serviya/pom.xml`:

**En desarrollo** — el `pom.xml` está montado y el volumen `maven_repo` persiste, así que
basta con **reiniciar el backend**: al rearrancar, `mvn spring-boot:run` vuelve a resolver
el `pom.xml` y descarga la dependencia nueva a `maven_repo`. No hace falta `--build`:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml restart backend
```

> La primera vez que arranca con la dependencia nueva la **descarga** (puede tardar).
> Espera a ver `Started ServiyaApplication in X seconds` en el log antes de probar
> (`logs -f backend`).

**En producción** — la dependencia se compila dentro del `.jar` al construir la imagen,
así que **hay que reconstruir** la imagen del backend:

```bash
docker compose up -d --build backend
```

### Frontend (`package.json`) — gotcha del volumen anónimo

Cuando **añades o actualizas una dependencia del frontend** (p. ej. `react-router-dom`) y estás en **desarrollo**, reconstruir con `--build` **no basta**. Hay que recrear los volúmenes anónimos con el flag `-V` (`--renew-anon-volumes`):

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build -V
```

**Por qué.** En `docker-compose.dev.yml` el frontend monta dos volúmenes:

```yaml
volumes:
  - ./frontend/serviya:/app   # tu código del host (para HMR)
  - /app/node_modules         # volumen anónimo que preserva el node_modules del contenedor
```

El bind mount taparía el `node_modules` que instala la imagen, así que el volumen anónimo `/app/node_modules` lo protege. El problema: ese volumen anónimo **se crea una sola vez y persiste entre reconstrucciones**. Aunque reconstruyas la imagen con la dependencia nueva, Docker **reutiliza el volumen anónimo viejo y lo monta encima**, ocultando el paquete recién instalado. El síntoma típico es Vite mostrando `Failed to resolve import "<paquete>"` aunque el paquete esté en `package.json`.

El flag `-V` descarta el volumen anónimo viejo y lo repuebla desde la imagen recién construida. **No** toca `mysql_data` (es un volumen con nombre), así que la base de datos se conserva.

> Alternativa equivalente: `docker compose ... down` (sin `-v`, para no borrar la DB) y volver a levantar con `--build`. Pero `-V` es más directo y no requiere bajar todo el stack.

**En producción** el frontend se compila (`npm run build`) y lo sirve Nginx, así que no hay
volumen anónimo de por medio: basta con **reconstruir** la imagen del frontend:

```bash
docker compose up -d --build frontend
```

---

## Base de datos

| Acción | Comando |
|---|---|
| Entrar a la consola de MySQL | `docker compose exec mysql mysql -u root -p` |
| Lanzar una consulta rápida | `docker compose exec mysql mysql -u root -p<password> marketplace_services -e "SELECT COUNT(*) FROM services;"` |
| Ejecutar un script manualmente | `docker compose exec -T mysql mysql -u root -p<password> marketplace_services < mysql/script.sql` |
| Re-ejecutar los scripts de inicio | `docker compose down -v` y luego volver a levantar |

> La base de datos se llama `marketplace_services` (variable `DB_NAME` en `.env`) y la
> contraseña de root es `MYSQL_ROOT_PASSWORD` (también en `.env`).

### Aplicar cambios en los scripts SQL tras un `git pull`

> **Léelo si acabas de bajar cambios del repo y la BD no refleja los datos nuevos.** Los
> scripts de `mysql/` corren **solo cuando el volumen de datos está vacío** (ver la
> siguiente sección). Si ya levantaste el proyecto antes, tu volumen tiene los datos
> viejos y **un `git pull` o un `--build` NO los actualiza** — aunque renombremos,
> añadamos o editemos un `*.sql`, MySQL los ignora porque el volumen ya existe. Para que
> los cambios se apliquen hay que **borrar el volumen de MySQL y volver a levantar** para
> que la init corra desde cero.

**En desarrollo** (recomendado: borra solo el volumen de MySQL y conserva la caché de Maven):

```bash
# 1) Baja los cambios
git pull

# 2) Borra el contenedor y el volumen de datos de MySQL (NO toca maven_repo)
docker compose -f docker-compose.yml -f docker-compose.dev.yml rm -sf mysql
docker volume rm serviya_mysql_data

# 3) Vuelve a levantar: los scripts de mysql/ se ejecutan de nuevo desde cero
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

**En producción** (no hay `maven_repo`, así que el `down -v` es la vía directa):

```bash
git pull
docker compose down -v          # elimina los contenedores y TODOS los volúmenes (incl. la BD)
docker compose up -d --build    # reconstruye y re-ejecuta los scripts de mysql/
```

> Verifica con `docker volume ls` que el volumen se llame `serviya_mysql_data`
> (`<proyecto>_<volumen>`). Si los seeds fallan al re-ejecutarse, el contenedor `mysql`
> queda `Exited (1)` y la BD a medias — mira el log (`logs --tail 40 mysql`) y revisa el
> "Gotcha" más abajo. Detalle ampliado en **"Resetear SOLO la base de datos"**.

### Cómo funcionan los scripts de la carpeta `mysql/`

- Se ejecutan **automáticamente solo la primera vez** que se crea el volumen de datos
  (cuando está vacío). En arranques posteriores **NO** se vuelven a ejecutar, aunque
  edites o añadas archivos. Para reaplicarlos hay que **borrar el volumen** (ver abajo).
- Corren en **orden alfabético/numérico** por el prefijo (`01_`, `02_`, …). Ese orden
  **es** el orden de dependencias de las foreign keys, así que importa:

  | Orden | Script | Puebla | Depende de |
  |---|---|---|---|
  | `01` | `create_database` | esquema completo (tablas) | — |
  | `02` | `populate_users` | `users` + `user_profiles` | — |
  | `03` | `populate_categories` | `categories` | — |
  | `04` | `populate_services` | `services` (oferente 1) | users, categories |
  | `05` | `populate_addresses` | `addresses` (clientes) | users |
  | `06` | `populate_offerer_profiles` | perfil + disponibilidad oferente 1 | users |
  | `07` | `populate_service_availabilities` | disponibilidad por servicio | services |
  | `08` | `populate_service_requests` | `service_requests` (8 filas) | services, users, addresses |
  | `09` | `populate_service_feedback` | `service_feedback` (solo COMPLETED) | service_requests |

- **Regla de oro al añadir/editar un seed:** una tabla solo se puede poblar **después**
  de las tablas a las que apunta por FK. Si necesitas insertar antes de que exista la
  tabla referenciada, NO uses `SET FOREIGN_KEY_CHECKS=0` (genera datos huérfanos):
  mejor renumera el archivo para que corra en el orden correcto.
- **`address_line`, `document_number` y `phone_number` son `VARBINARY`** (PII cifrada
  AES-256-GCM en runtime). Aun así se pueden **sembrar en texto plano**: el
  `PiiAttributeConverter` del backend, al leer, detecta que no es ciphertext válido y
  devuelve el valor tal cual. Por eso los seeds insertan estos campos en claro.

> **Gotcha — un script de init que falla aborta TODO.** Si cualquier `*.sql` de `mysql/`
> da error (típicamente una FK que no se cumple), el contenedor `mysql` termina con
> `Exited (1)` y deja el volumen **a medio inicializar**. En el siguiente arranque el
> volumen ya no está vacío, así que los scripts **no** se vuelven a ejecutar y la BD
> queda incompleta para siempre. La salida: arreglar el script y **volver a borrar el
> volumen** (`down -v` o el reset rápido de abajo).

### Resetear SOLO la base de datos (sin re-descargar Maven)

`docker compose down -v` borra **todos** los volúmenes, incluido `maven_repo` (la caché
de dependencias del backend). Eso obliga al backend a re-descargar todo Maven en el
siguiente arranque (varios minutos). Si solo quieres **reaplicar los seeds**, borra
únicamente el volumen de MySQL:

```bash
# 1) Elimina el contenedor de mysql y su volumen de datos (conserva maven_repo)
docker compose -f docker-compose.yml -f docker-compose.dev.yml rm -sf mysql
docker volume rm serviya_mysql_data

# 2) Vuelve a levantar: la init de mysql corre de nuevo desde cero
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

> El nombre del volumen es `serviya_mysql_data` (`<proyecto>_<volumen>`). Confírmalo con
> `docker volume ls`. Como el backend no se recrea, su `maven_repo` se conserva y el
> arranque es mucho más rápido.

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

# Aplicar cambios en los scripts SQL tras un `git pull` (reset de la BD, conserva Maven)
git pull
docker compose -f docker-compose.yml -f docker-compose.dev.yml rm -sf mysql
docker volume rm serviya_mysql_data
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# Aplicar una dependencia NUEVA del backend (pom.xml) en DESARROLLO
docker compose -f docker-compose.yml -f docker-compose.dev.yml restart backend

# Aplicar una dependencia NUEVA del frontend (package.json) en DESARROLLO (flag -V)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build -V

# En PRODUCCIÓN, una dependencia nueva (backend o frontend) se aplica reconstruyendo
docker compose up -d --build backend   # o: ... --build frontend
```
