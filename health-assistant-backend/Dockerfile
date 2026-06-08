# ============================================================
# 智能健康助手 - 后端 Docker 镜像
# ============================================================

FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

COPY settings-docker.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser
COPY --from=builder /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
