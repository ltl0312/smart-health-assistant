# ============================================================
# 智能健康助手 - 前端 Docker 镜像 (Multi-stage Build)
# ============================================================

# ---- Stage 1: 构建 ----
FROM node:20-alpine AS builder

WORKDIR /app

# 配置 npm 国内镜像源
RUN npm config set registry https://registry.npmmirror.com

# 利用 Docker 缓存层
COPY package.json ./
RUN npm install --legacy-peer-deps

# 复制源码并构建
COPY . .
RUN npm run build

# ---- Stage 2: Nginx 运行 ----
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
