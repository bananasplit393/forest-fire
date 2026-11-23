# Bygges fra frontend/:  docker build -t forest-fire-frontend .
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# API'ets adresse set fra browseren
ARG VITE_API_URL=http://localhost:8081
ENV VITE_API_URL=$VITE_API_URL
# bærbarens IP, vises i footeren så appen kan forbinde
ARG VITE_HOST_IP=
ENV VITE_HOST_IP=$VITE_HOST_IP
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
