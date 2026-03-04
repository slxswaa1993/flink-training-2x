# Stage 1: Dependencies
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

# Stage 2: Build static export
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# Stage 3: Nginx serves static files + proxies API/WS to backend
FROM nginx:1.27-alpine

COPY --from=builder /app/out /usr/share/nginx/html

# nginx.conf is in the Docker context (infra/docker/dashboard-frontend/)
# but we COPY it from the build context root which is services/dashboard-frontend/
# The actual config is copied via the build command's -f flag
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
