# Step 4: docker-compose-stack

> Task 4 of phase 01. postgres+pgvector / backend / frontend-dev / caddy 네 서비스를 Docker Compose로 묶는다. 운영용 `docker-compose.yml`과 로컬 전용 `docker-compose.override.yml`을 분리해 ADR-008("로컬/운영 동일 파일, 차이는 override + .env")을 지킨다.

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` "서비스 시작 순서", "환경 변수 명세"
- `docs/ADR.md` ADR-008 (Docker Compose)
- `deploy/.env.example` (이미 존재 — 변수 누락 점검)
- `phases/01-project-skeleton/step1.md` (백엔드 application.yml의 env 참조)

## 작업

### 4.1 `backend/Dockerfile`

```dockerfile
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
```

### 4.2 `frontend/Dockerfile.dev`

```dockerfile
# syntax=docker/dockerfile:1.7
FROM node:20-alpine

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json* /app/
RUN npm install --no-audit --no-fund

COPY frontend /app

EXPOSE 5173

# vite는 컨테이너 내부 0.0.0.0 바인딩 필요
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

> 운영용 frontend 이미지는 phase 11 또는 phase 30에서 정적 빌드 + Caddy 정적 호스팅 형태로 별도 작성한다. 이 step은 로컬 dev 서버만 컨테이너화.

### 4.3 `deploy/docker-compose.yml` (운영 + 로컬 공통 기본)

```yaml
name: mneme

services:
  postgres:
    image: pgvector/pgvector:pg16
    restart: unless-stopped
    environment:
      POSTGRES_DB: mneme
      POSTGRES_USER: ${MNEME_DB_USER:-mneme}
      POSTGRES_PASSWORD: ${MNEME_DB_PASSWORD:?MNEME_DB_PASSWORD 환경변수가 필요합니다}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${MNEME_DB_USER:-mneme} -d mneme"]
      interval: 5s
      timeout: 3s
      retries: 20
      start_period: 5s
    networks:
      - mneme

  backend:
    build:
      context: ..
      dockerfile: backend/Dockerfile
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      MNEME_BASE_URL: ${MNEME_BASE_URL:-http://localhost:8080}
      MNEME_FRONTEND_ORIGIN: ${MNEME_FRONTEND_ORIGIN:-http://localhost:5173}
      MNEME_DB_URL: jdbc:postgresql://postgres:5432/mneme
      MNEME_DB_USER: ${MNEME_DB_USER:-mneme}
      MNEME_DB_PASSWORD: ${MNEME_DB_PASSWORD}
      MNEME_GOOGLE_OAUTH_CLIENT_ID: ${MNEME_GOOGLE_OAUTH_CLIENT_ID:-}
      MNEME_GOOGLE_OAUTH_CLIENT_SECRET: ${MNEME_GOOGLE_OAUTH_CLIENT_SECRET:-}
      MNEME_OPENAI_API_KEY: ${MNEME_OPENAI_API_KEY:-}
      MNEME_OPENAI_EMBED_MODEL: ${MNEME_OPENAI_EMBED_MODEL:-text-embedding-3-small}
      MNEME_OPENAI_LLM_MODEL: ${MNEME_OPENAI_LLM_MODEL:-gpt-4o-mini}
      JAVA_OPTS: ${JAVA_OPTS:--Xmx1g}
    ports:
      - "8080:8080"
    networks:
      - mneme

  caddy:
    image: caddy:2-alpine
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - mneme

volumes:
  postgres_data:
  caddy_data:
  caddy_config:

networks:
  mneme:
    driver: bridge
```

### 4.4 `deploy/docker-compose.override.yml` (로컬 dev 전용)

```yaml
services:
  postgres:
    ports:
      - "5432:5432"   # 로컬 디버깅용 외부 노출

  backend:
    environment:
      SPRING_PROFILES_ACTIVE: local

  frontend-dev:
    build:
      context: ..
      dockerfile: frontend/Dockerfile.dev
    restart: unless-stopped
    environment:
      VITE_DEV_API_TARGET: http://backend:8080
    ports:
      - "5173:5173"
    volumes:
      - ../frontend/src:/app/src:ro
      - ../frontend/index.html:/app/index.html:ro
      - ../frontend/vite.config.ts:/app/vite.config.ts:ro
      - ../frontend/tailwind.config.ts:/app/tailwind.config.ts:ro
      - ../frontend/postcss.config.js:/app/postcss.config.js:ro
    networks:
      - mneme
```

> override는 운영에서는 `--file deploy/docker-compose.yml`만 명시해 자동 적용을 회피한다. 로컬은 두 파일 모두 명시.

### 4.5 `deploy/Caddyfile`

```caddy
# 로컬 개발/단순 셀프호스팅 기본값.
# 운영 도메인 확정 후 phase 30(hosting-decision)에서 도메인 + 자동 HTTPS로 교체.

{
    auto_https off
}

:80 {
    # 백엔드로 라우팅할 경로 (REST + Actuator + MCP)
    @backend path /api/* /actuator/* /mcp/* /oauth/*
    handle @backend {
        reverse_proxy backend:8080
    }

    # 프론트엔드 dev 서버 (Vite HMR 포함)
    handle {
        reverse_proxy frontend-dev:5173
    }

    encode gzip

    log {
        output stdout
        format console
    }
}
```

> 운영 시 위 블록을 `mneme.example.com { ... }` 형태로 바꾸고 `auto_https off`를 제거하면 Caddy가 Let's Encrypt 자동 발급.

### 4.6 `deploy/.env.example` 점검

현재 파일에 다음 변수가 모두 있는지 확인 (없으면 추가). 이미 step0 보강에서 모두 들어있을 가능성이 큼:

- `MNEME_BASE_URL`, `MNEME_FRONTEND_ORIGIN`
- `MNEME_DB_URL`, `MNEME_DB_USER`, `MNEME_DB_PASSWORD`
- `MNEME_GOOGLE_OAUTH_CLIENT_ID`, `MNEME_GOOGLE_OAUTH_CLIENT_SECRET`
- `MNEME_OPENAI_API_KEY`, `MNEME_OPENAI_EMBED_MODEL`, `MNEME_OPENAI_LLM_MODEL`
- `SPRING_PROFILES_ACTIVE`, `JAVA_OPTS`

비교 명령:

```bash
grep -E '^(MNEME_|SPRING_PROFILES_ACTIVE|JAVA_OPTS)' deploy/.env.example | sort
```

누락 발견 시 같은 파일 끝에 추가하고 한 줄 한국어 코멘트.

### 4.7 빌드 검증 (실행은 step 5)

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml --env-file deploy/.env config -q
```

기대: 출력 없음 + exit 0 (compose 파일 문법 검증).

`.env`가 아직 없으면 잠시 더미 생성:

```bash
cp deploy/.env.example deploy/.env
sed -i.bak 's/^MNEME_DB_PASSWORD=.*/MNEME_DB_PASSWORD=devpass/' deploy/.env
rm deploy/.env.bak
```

위 명령 후 다시 `compose config -q`. 통과해야 함. 검증 후 `.env`는 step 5에서 다시 사용하므로 그대로 둔다. 단, `.gitignore`에 등록된 상태인지 확인.

### 4.8 커밋

```bash
git add backend/Dockerfile frontend/Dockerfile.dev deploy/
git commit -m "chore(deploy): add docker compose stack with postgres pgvector and caddy

- pgvector/pgvector:pg16 + healthcheck pg_isready
- backend multi-stage Dockerfile(gradle 8.7-jdk21 → temurin:21-jre-alpine, non-root)
- frontend-dev 컨테이너 vite host=0.0.0.0
- Caddyfile :80 → /api,/actuator,/mcp,/oauth → backend, 나머지 → frontend-dev
- override.yml 에 frontend-dev + postgres 5432 노출 (로컬 전용)
- 운영은 docker-compose.yml 단독 적용

Refs: ADR-008, ARCHITECTURE 시작 순서"
```

## Acceptance Criteria

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml --env-file deploy/.env config -q
docker compose -f deploy/docker-compose.yml --env-file deploy/.env config -q
```

두 명령 모두 exit 0, 출력 없음.

추가로 `Caddyfile` 문법 검증 (Docker 이용):

```bash
docker run --rm -v "$PWD/deploy/Caddyfile":/etc/caddy/Caddyfile caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile
```

기대 출력 마지막 줄에 `Valid configuration`.

## 검증 절차

1. Acceptance Criteria 두 명령 + Caddyfile 검증 모두 통과.
2. `deploy/.env` 가 git에 추적되지 않는지 확인: `git status --ignored | grep deploy/.env` (ignored 섹션에 위치).
3. `docker-compose.yml`의 환경변수 표기가 ARCHITECTURE 매트릭스와 정확히 일치 (`MNEME_*` prefix).
4. 운영용 `docker-compose.yml`에 frontend-dev가 없는지 확인 (override에만).
5. 성공 시 `phases/01-project-skeleton/index.json`의 step 4 갱신:
   ```json
   { "step": 4, "name": "docker-compose-stack", "status": "completed", "summary": "postgres pgvector/backend/frontend-dev/caddy compose 4서비스 + healthcheck + Caddyfile 검증" }
   ```

## 금지사항

- 컨테이너에서 root로 실행 금지. **이유: 컨테이너 탈출 위험. backend Dockerfile은 mneme 사용자**.
- `version: "3.8"` 같은 deprecated 필드 사용 금지. **이유: compose v2는 version 키 무시 — 명시할 필요 없음. `name:` 키 사용**.
- 운영용 `docker-compose.yml`에 `:latest` 태그 금지. **이유: 재현성. `pgvector/pgvector:pg16` `caddy:2-alpine` `node:20-alpine` `eclipse-temurin:21-jre-alpine` 처럼 메이저 버전 고정**.
- Caddyfile에 운영 도메인 하드코딩하지 마라. **이유: phase 30에서 도메인 확정**.
- `.env` 파일을 커밋하지 마라. **이유: 비밀 누출**.
- backend 이미지에 OpenAI 키를 ENV로 굽지 마라. **이유: 이미지 레이어에 평문 키 잔류 → 유출**.
