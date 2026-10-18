# syntax=docker/dockerfile:1

# --- build: static bundle; Keycloak settings are compiled in (they are read by the browser)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_KEYCLOAK_REALM=bidanddrive
ARG VITE_KEYCLOAK_CLIENT_ID=auction-frontend
RUN npm run build

# --- runtime: nginx serves the SPA and proxies /api to the backend
FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
