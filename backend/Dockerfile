# syntax=docker/dockerfile:1.7

# ── 1단계: gradle 빌드 ─────────────────────────────────────────
FROM gradle:8.7-jdk21 AS builder
WORKDIR /workspace

# 의존성 캐시를 위해 gradle 파일만 먼저 복사
COPY settings.gradle.kts build.gradle.kts gradle.properties /workspace/
COPY gradle /workspace/gradle
COPY backend/build.gradle.kts /workspace/backend/

# 의존성 사전 해결
RUN gradle :backend:dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY backend/src /workspace/backend/src
RUN gradle :backend:bootJar --no-daemon -x test

# ── 2단계: runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S mneme && adduser -S mneme -G mneme
USER mneme
WORKDIR /app

COPY --from=builder /workspace/backend/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx1g"

# Spring Boot Actuator 헬스를 docker healthcheck에 노출 (curl 없이 wget 사용)
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget --quiet --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
