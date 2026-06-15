# ============================================================
# 智能健康助手 — 统一 Dockerfile（全栈单体镜像）
# ============================================================
# 依赖清单：
#   运行时: JDK 17 (Eclipse Temurin), Nginx (Alpine)
#   构建时: Node 20, Maven 3.9
#   外部服务: MySQL 8.0, Redis 7, DeepSeek API
#
# 构建:  docker build -t smart-health-assistant .
# 运行:  docker run -p 80:80 --env-file .env smart-health-assistant
# 编排:  docker compose up -d
# ============================================================

# ============================
# Stage 1 — 构建前端 (Vue 3 + Vite)
# ============================
FROM node:20-alpine AS frontend-builder

# npm 国内镜像源（可根据网络环境删除此行）
RUN npm config set registry https://registry.npmmirror.com

WORKDIR /app/frontend

# 利用 Docker 层缓存：先装依赖，再复制源码
COPY health-assistant-frontend/package.json ./
RUN npm install --legacy-peer-deps

COPY health-assistant-frontend/ ./
RUN npm run build

# ============================
# Stage 2 — 构建后端 (Spring Boot 3.2 + Maven)
# ============================
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder

# 阿里云 Maven 镜像加速
COPY health-assistant-backend/settings-docker.xml /root/.m2/settings.xml

WORKDIR /app

# 先复制 pom.xml 下载依赖（层缓存）
COPY health-assistant-backend/pom.xml ./
RUN mvn dependency:go-offline -B -q || true

# 复制后端源码 + 初始化SQL
COPY health-assistant-backend/src ./src

# 将前端构建产物复制到 Spring Boot 静态资源目录
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

# 编译打包（跳过测试）
RUN mvn clean package -Dmaven.test.skip=true -B -q

# ============================
# Stage 3 — 运行时镜像 (JRE 17 + Nginx 反向代理)
# ============================
FROM eclipse-temurin:17-jre-alpine

# 安装 Nginx（反向代理 + 安全头 + Gzip）
RUN apk add --no-cache nginx curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    mkdir -p /run/nginx /app/uploads && \
    addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser && \
    chown -R appuser:appgroup /app /run/nginx /var/lib/nginx /var/log/nginx

WORKDIR /app

# ---- 环境变量（所有可配置项在此集中声明）----

# 服务端口
ENV SERVER_PORT=8080

# MySQL 数据库
ENV SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/smart_health_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true" \
    SPRING_DATASOURCE_USERNAME=root \
    MYSQL_PASSWORD="" \
    SPRING_DATASOURCE_PASSWORD=""

# Redis
ENV SPRING_DATA_REDIS_HOST=redis \
    SPRING_DATA_REDIS_PORT=6379

# JWT 鉴权
ENV JWT_SECRET="" \
    JWT_EXPIRATION=604800000

# DeepSeek AI
ENV DEEPSEEK_BASE_URL="https://api.deepseek.com" \
    DEEPSEEK_API_KEY="" \
    DEEPSEEK_MODEL="deepseek-v4-pro" \
    DEEPSEEK_TIMEOUT_MS=120000 \
    DEEPSEEK_TEMPERATURE=0.2 \
    DEEPSEEK_MAX_TOKENS=4096

# CORS 跨域
ENV APP_CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:80,http://localhost"

# 文件上传
ENV UPLOAD_AVATAR_MAX_SIZE=5242880 \
    UPLOAD_AVATAR_PATH="/app/uploads/avatars/"

# MyBatis 日志
ENV MYBATIS_LOG_IMPL="org.apache.ibatis.logging.stdout.StdOutImpl"

# ---- 复制构建产物 ----

# 前端静态文件
COPY --from=frontend-builder /app/frontend/dist /usr/share/nginx/html

# 后端 JAR
COPY --from=backend-builder /app/target/*.jar /app/app.jar

# Nginx 配置 & 启动脚本
COPY docker/nginx.conf /etc/nginx/http.d/default.conf
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 创建上传目录
RUN mkdir -p /app/uploads/avatars && chown -R appuser:appgroup /app/uploads

USER appuser

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost/ || exit 1

ENTRYPOINT ["/entrypoint.sh"]
