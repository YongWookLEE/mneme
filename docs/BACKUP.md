# Mneme 백업·복구

매일 자동 pg_dump → 객체 스토리지(B2/S3/MinIO 등 S3 호환) 업로드 + 필요 시 별도 환경에서 복원.

## 1. 객체 스토리지 준비

추천: [Backblaze B2](https://www.backblaze.com/cloud-storage) — 첫 10GB 무료, 이후 $0.006/GB/월. AWS S3·Cloudflare R2·MinIO 어느 쪽이든 S3 호환이면 동작합니다.

1. 새 버킷 생성 (예: `mneme-prod-backup`)
2. Application Key 발급 (Read/Write 필요)
3. 엔드포인트 URL 메모 (B2 기준 `https://s3.<region>.backblazeb2.com`)

## 2. 환경 변수 작성

`deploy/.env`에 추가:

```env
MNEME_BACKUP_S3_BUCKET=mneme-prod-backup
MNEME_BACKUP_S3_PREFIX=mneme-backup
AWS_ACCESS_KEY_ID=<발급받은 keyID>
AWS_SECRET_ACCESS_KEY=<발급받은 applicationKey>
AWS_DEFAULT_REGION=auto
AWS_ENDPOINT_URL=https://s3.us-west-002.backblazeb2.com
```

> AWS S3는 `AWS_ENDPOINT_URL`을 비워 두면 기본 엔드포인트를 씁니다.

## 3. 백업 컨테이너 띄우기

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.backup.yml \
  --env-file deploy/.env up -d backup
```

내부적으로 `crond`가 매일 **03:00 UTC**에 `scripts/backup.sh`를 실행합니다.

## 4. 수동 백업 / 로그

```bash
# 즉시 한 번 실행
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.backup.yml \
  exec backup /scripts/backup.sh

# 로그 추적
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.backup.yml \
  logs -f backup
```

## 5. 복원 (정기 리허설 권장)

운영 DB에 직접 복원하지 말고 **별도 인스턴스**로 받아 검증한 뒤 데이터 마이그레이션 결정.

```bash
# 1) 별도 디렉토리에서 docker compose 띄움 (postgres만 새 데이터 디렉토리로)
mkdir -p ~/mneme-restore && cd ~/mneme-restore
cp -r <원본>/deploy .
# deploy/.env에 다른 MNEME_DB_PASSWORD 사용, postgres_data 볼륨 이름이 분리되도록 name: 변경

docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d postgres

# 2) 복원 컨테이너 띄우고 객체 다운로드 → psql 적용
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.backup.yml \
  --env-file deploy/.env up -d backup
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.backup.yml \
  exec backup /scripts/restore.sh mneme-20260628T030000Z.sql.gz

# 3) 별도 백엔드 띄워서 검증
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d backend
curl -sf http://localhost:8080/actuator/health
```

복원이 끝나면 별도 환경을 내리고(`docker compose down -v`) 운영 환경에는 손대지 않습니다.

## 6. 보존 정책

스크립트 자체는 업로드만 합니다. **삭제·보존은 객체 스토리지 lifecycle policy로** 관리하세요.

- B2: Buckets → Lifecycle Settings → "Keep all versions for N days"
- AWS S3: Lifecycle rules → "Delete previous versions after 30 days"

## 7. 정기 리허설 체크리스트

운영 안정을 위해 분기에 한 번 다음을 수행:

- [ ] 최근 백업이 객체 스토리지에 존재하는지 확인 (콘솔 또는 `aws s3 ls`)
- [ ] 가장 최근 백업으로 복원 절차(§5) 실행 → 헬스 체크 통과 + 본인 메모리 일부 조회
- [ ] 백업 컨테이너 로그에 실패 줄(`FATAL`, `error`)이 없는지 확인
- [ ] 객체 스토리지 비용·사용량 점검

## 8. 트러블슈팅

| 증상 | 원인/해결 |
|---|---|
| `aws: not found` | 백업 컨테이너 빌드 실패. `docker compose build backup` 다시. |
| `Could not connect to ... endpoint` | `AWS_ENDPOINT_URL`이 잘못됐거나 네트워크 차단. B2는 region별 URL이 다름. |
| `Access Denied` | Application Key의 권한이 readOnly이거나 다른 버킷에 발급됨. 새로 만들 것. |
| `pg_dump: error: connection to server ... failed` | postgres 컨테이너가 아직 안 떴거나 `MNEME_DB_PASSWORD` 불일치. |
| 백업이 너무 큼 | `memory_versions` 테이블이 누적된 경우. 현재는 versioning이 없으므로 보통은 단순 누적. 데이터 크기에 비례. |
