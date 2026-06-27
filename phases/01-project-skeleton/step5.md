# Step 5: compose-health-verification

> Task 5 of phase 01. `docker compose up`으로 전체 스택을 띄우고 backend Actuator / frontend dev 서버 / Caddy 라우팅 / Postgres 연결성 4개를 모두 헬스 통과시킨다. 새 파일은 만들지 않는다. 검증 결과를 step의 `summary`에 남기는 것이 산출물.

## 읽어야 할 파일

- `phases/01-project-skeleton/step1.md` ~ `step4.md`
- `docs/ARCHITECTURE.md` "서비스 시작 순서"
- `deploy/docker-compose.yml`, `deploy/docker-compose.override.yml`, `deploy/Caddyfile`, `deploy/.env.example`

## 작업

### 5.1 `.env` 준비

```bash
cd /Users/lyw/Documents/self/workspace/mneme
[ -f deploy/.env ] || cp deploy/.env.example deploy/.env

# 비밀 자리에 더미값 채우기 (없으면)
grep -q '^MNEME_DB_PASSWORD=devpass$' deploy/.env || \
  sed -i.bak 's/^MNEME_DB_PASSWORD=.*/MNEME_DB_PASSWORD=devpass/' deploy/.env

# OpenAI/Google 키는 phase 01에서 미사용이라 빈 문자열 유지
rm -f deploy/.env.bak
```

확인:

```bash
grep -E '^MNEME_DB_PASSWORD' deploy/.env
# 기대: MNEME_DB_PASSWORD=devpass
```

### 5.2 빌드 + 기동

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.override.yml \
  --env-file deploy/.env \
  up -d --build
```

빌드는 첫 회 3~6분(JVM + node deps). 두 번째부터는 캐시 사용.

기대: 모든 서비스가 background로 기동되고 명령은 exit 0.

### 5.3 상태 확인

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml ps
```

기대 출력 (요지):

```
NAME                  SERVICE         STATUS              PORTS
mneme-postgres-1      postgres        Up X (healthy)      0.0.0.0:5432->5432/tcp
mneme-backend-1       backend         Up X (healthy)      0.0.0.0:8080->8080/tcp
mneme-frontend-dev-1  frontend-dev    Up X                0.0.0.0:5173->5173/tcp
mneme-caddy-1         caddy           Up X                0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
```

`backend`가 `Up (health: starting)`이면 30~60초 더 대기 후 재확인. `Up (unhealthy)`이면 다음 단계로 가지 말고 로그 분석:

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml logs backend --tail=200
```

흔한 원인:
- Postgres healthcheck 실패 → `MNEME_DB_PASSWORD` 빈 값 또는 typo
- Spring `ApplicationContext` 실패 → step 1의 `application.yml` autoconfigure exclude 누락
- 메모리 부족 → `JAVA_OPTS=-Xmx512m`로 임시 완화

### 5.4 헬스 검증 (4종)

```bash
# 1) 백엔드 Actuator (직접)
curl -sf http://localhost:8080/actuator/health | tee /tmp/backend-health.json
# 기대 본문: {"status":"UP"}

# 2) Vite dev 서버
curl -sf -o /tmp/frontend.html http://localhost:5173/
grep -q "Mneme" /tmp/frontend.html && echo "[OK] frontend Mneme 텍스트 확인"

# 3) Caddy 통과 백엔드
curl -sf http://localhost/actuator/health | grep -q '"status":"UP"' && echo "[OK] caddy → backend"

# 4) Caddy 통과 프론트
curl -sf http://localhost/ | grep -q "Mneme" && echo "[OK] caddy → frontend"

# 5) Postgres 연결성
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml exec -T postgres \
  pg_isready -U mneme -d mneme
# 기대 출력: /var/run/postgresql:5432 - accepting connections
```

5종 모두 통과해야 step 완료. 하나라도 실패 시 그 항목의 로그를 step의 `summary` 또는 `error_message`에 기록.

### 5.5 정리

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml down
```

볼륨은 보존(`-v` 안 붙임). 다음 phase에서 Postgres 데이터가 필요할 수 있음.

### 5.6 검증 노트 (커밋 메시지에 포함)

검증 결과를 한 줄 요약으로 남긴다. 코드 변경이 없으므로 빈 커밋:

```bash
git commit --allow-empty -m "chore(deploy): verify compose stack health

- postgres pg_isready: OK
- backend /actuator/health UP: OK
- frontend :5173 Mneme 텍스트: OK
- caddy :80 → backend/frontend 라우팅: OK
- 빌드 시간 ~Xm Ys (1회차)

Refs: phase 01 step 5"
```

빈 커밋이 싫으면 이 step은 커밋 없이 index.json `summary`에만 결과 남겨도 무방.

## Acceptance Criteria

5.4의 5종 검증 명령이 전부 통과. 각 명령의 출력이 위 기대와 일치.

요약 명령 (한 번에):

```bash
set -e
curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'
curl -sf http://localhost:5173/ | grep -q "Mneme"
curl -sf http://localhost/actuator/health | grep -q '"status":"UP"'
curl -sf http://localhost/ | grep -q "Mneme"
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml exec -T postgres pg_isready -U mneme -d mneme
echo "ALL OK"
```

마지막 라인이 `ALL OK`여야 함.

## 검증 절차

1. Acceptance Criteria 묶음 명령 통과.
2. `docker compose down` 후 `docker volume ls | grep mneme_postgres_data`로 볼륨 보존 확인.
3. 성공 시 `phases/01-project-skeleton/index.json`의 step 5 갱신:
   ```json
   { "step": 5, "name": "compose-health-verification", "status": "completed", "summary": "postgres/backend/frontend-dev/caddy 모두 헬스 통과, caddy 라우팅 검증 OK" }
   ```
4. 실패 시:
   - `status: error` + `error_message`에 어떤 검증이 어떻게 실패했는지 (curl exit code, backend 로그 마지막 30줄 요약)
   - 사용자 개입이 필요하면(예: 포트 80 점유) `status: blocked` + `blocked_reason`

## 금지사항

- 검증 실패를 우회하려고 healthcheck를 비활성화하지 마라. **이유: 헬스 통과가 phase 01의 본 목적**.
- 검증 통과를 위해 잠시 `application.yml`에 `autoconfigure.exclude`를 더 늘리지 마라. **이유: phase 02에서 DataSource를 다시 켜야 함 — 의도하지 않은 우회는 다음 phase 망가뜨림**.
- 검증 후 `docker compose down -v`로 볼륨 삭제하지 마라. **이유: 비파괴 기본. 사용자 명시 승인 없이 볼륨 삭제 금지**.
- 5173/8080/5432/80 포트가 점유되어 있을 때 다른 서비스를 멈추는 결정을 임의로 내리지 마라. **이유: 사용자 환경 영향**. blocked 처리 후 사용자 안내.
