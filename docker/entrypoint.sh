#!/bin/sh
# ============================================================
# 智能健康助手 — 容器启动脚本
# ============================================================
# 同时启动 Nginx (80) 和 Spring Boot (8080)
# Nginx 在前台运行，Java 在后台运行
# 任一进程退出都会导致容器停止
# ============================================================

set -e

echo "=== 智能健康助手 容器启动 ==="

# 将 Docker 环境变量传递给 Spring Boot
# MYSQL_PASSWORD → SPRING_DATASOURCE_PASSWORD（如果未单独设置）
if [ -n "${MYSQL_PASSWORD}" ] && [ -z "${SPRING_DATASOURCE_PASSWORD}" ]; then
    export SPRING_DATASOURCE_PASSWORD="${MYSQL_PASSWORD}"
fi

echo "[1/2] 启动 Spring Boot 后端 (端口 8080)..."
java -XX:+UseG1GC \
     -XX:MaxRAMPercentage=75.0 \
     -XX:+ExitOnOutOfMemoryError \
     -Duser.timezone=Asia/Shanghai \
     -jar /app/app.jar &
JAVA_PID=$!

# 等待后端就绪（轮询端口 8080，任意 HTTP 响应即表示就绪）
echo "[2/2] 等待后端就绪..."
for i in $(seq 1 60); do
    STATUS=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/ 2>/dev/null || echo "000")
    if [ "$STATUS" != "000" ]; then
        echo "后端就绪 (耗时 ${i}s, HTTP $STATUS)"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "警告: 后端 60 秒内未就绪，继续启动 Nginx"
    fi
    sleep 1
done

echo "启动 Nginx (端口 80)..."
# Nginx 前台运行；当它退出时容器停止
nginx -g "daemon off;" &
NGINX_PID=$!

# 信号处理：将 TERM/INT 转发给两个子进程
cleanup() {
    echo "收到关闭信号，正在停止服务..."
    kill -TERM $NGINX_PID 2>/dev/null || true
    kill -TERM $JAVA_PID 2>/dev/null || true
    wait
    echo "所有服务已停止。"
}

trap cleanup TERM INT

# 监控子进程；任一退出则全部退出
wait -n $NGINX_PID $JAVA_PID
EXIT_CODE=$?
echo "进程退出 (code=$EXIT_CODE)，容器即将停止。"
cleanup
exit $EXIT_CODE
