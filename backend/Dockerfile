# syntax=docker/dockerfile:1.7
# =====================================================================
# SATU Dockerfile untuk SEMUA service Spring Boot.
#
# Service dipilih lewat build arg MODULE (lihat docker-compose.yml):
#     docker build -f backend/Dockerfile --build-arg MODULE=user-service ./backend
#
# Kenapa tidak satu Dockerfile per service? Karena 9 file yang hampir identik
# berarti 9 tempat untuk lupa diperbarui. Satu file = satu sumber kebenaran.
# =====================================================================

# ---------------------------------------------------------------------
# Tahap 1 - build
# ---------------------------------------------------------------------
FROM maven:3.9.16-eclipse-temurin-21 AS build

ARG MODULE
WORKDIR /workspace

# 1a) Salin POM lebih dulu. Docker meng-cache layer ini, jadi selama dependency
#     tidak berubah, `mvn dependency:go-offline` tidak dijalankan ulang.
COPY pom.xml ./
COPY platform-common/pom.xml platform-common/
COPY discovery-service/pom.xml discovery-service/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY product-service/pom.xml product-service/
COPY cart-service/pom.xml cart-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl "${MODULE}" -am -DskipTests dependency:go-offline || true

# 1b) Baru salin source dan build.
#     `-am` (also-make) otomatis membangun platform-common yang dibutuhkan.
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl "${MODULE}" -am -DskipTests package \
 && cp "${MODULE}"/target/*.jar /workspace/app.jar

# ---------------------------------------------------------------------
# Tahap 2 - runtime
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Jangan pernah jalan sebagai root di dalam container.
RUN addgroup -S shopflow && adduser -S shopflow -G shopflow

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R shopflow:shopflow /app

USER shopflow

# -XX:MaxRAMPercentage -> JVM menghormati limit memori container (bukan RAM host).
# Ini penyebab paling umum container Java di-OOM-kill di Kubernetes.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Shell form supaya ${SERVER_PORT} diekspansi saat runtime.
#
# start-period yang panjang (120s) bukan pemborosan: JVM butuh 20-40 detik untuk
# benar-benar siap di mesin normal, dan jauh lebih lama saat banyak service start
# bersamaan. Kalau start-period terlalu pendek, container akan ditandai "unhealthy"
# padahal aplikasinya sehat - dan di Kubernetes itu berarti pod dibunuh berulang kali,
# lalu masuk CrashLoopBackOff.
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=10 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
