FROM node:22 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
# Lab 04 (Secure Channel): openssl is only needed to generate the local dev
# certificate at container start. See 40-dev-tls.sh.
RUN apk add --no-cache openssl
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Hook ejecutado por el entrypoint oficial de nginx: genera el certificado dev
# en /etc/nginx/certs si todavía no existe (volumen frontend_certs).
COPY 40-dev-tls.sh /docker-entrypoint.d/40-dev-tls.sh
RUN chmod +x /docker-entrypoint.d/40-dev-tls.sh
EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
