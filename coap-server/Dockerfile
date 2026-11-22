# Bygges fra coap-server/:  docker build -t forest-fire-coap .
FROM node:22-alpine
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY server.ts ./
EXPOSE 5683/udp
CMD ["npx", "tsx", "server.ts"]
