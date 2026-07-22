# syntax=docker/dockerfile:1.7

FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=/api/v1
ARG VITE_WS_URL=/ws
ARG VITE_APP_NAME="NEXUS Smart Cyber Esports"
ARG VITE_ENABLE_PWA=true
ARG VITE_ENABLE_VOICE=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_APP_NAME=$VITE_APP_NAME
ENV VITE_ENABLE_PWA=$VITE_ENABLE_PWA
ENV VITE_ENABLE_VOICE=$VITE_ENABLE_VOICE

RUN npm run build

FROM nginx:1.27-alpine AS runtime

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://localhost/healthz | grep -q ok || exit 1
