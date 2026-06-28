#!/usr/bin/env sh
# Mneme 백업 스크립트 — pg_dump → gzip → S3 호환 객체 스토리지 업로드.
#
# 환경 변수:
#   MNEME_DB_HOST           기본 postgres
#   MNEME_DB_USER           기본 mneme
#   MNEME_DB_NAME           기본 mneme
#   MNEME_DB_PASSWORD       (필수)
#   MNEME_BACKUP_S3_BUCKET  버킷 이름 (필수)
#   MNEME_BACKUP_S3_PREFIX  기본 mneme-backup
#   AWS_ACCESS_KEY_ID       (필수 — B2의 keyId 또는 AWS access key)
#   AWS_SECRET_ACCESS_KEY   (필수)
#   AWS_DEFAULT_REGION      기본 auto
#   AWS_ENDPOINT_URL        예) https://s3.us-west-002.backblazeb2.com (B2). 비우면 AWS S3 기본
#   MNEME_BACKUP_RETAIN_DAYS 기본 30 (이보다 오래된 백업은 lifecycle policy로 관리 권장. 본 스크립트는 업로드만)
#
# Cron 예: 0 3 * * * /scripts/backup.sh >> /var/log/mneme-backup.log 2>&1

set -eu

: "${MNEME_DB_HOST:=postgres}"
: "${MNEME_DB_USER:=mneme}"
: "${MNEME_DB_NAME:=mneme}"
: "${MNEME_BACKUP_S3_PREFIX:=mneme-backup}"
: "${AWS_DEFAULT_REGION:=auto}"

if [ -z "${MNEME_DB_PASSWORD:-}" ]; then
  echo "[backup] FATAL: MNEME_DB_PASSWORD 환경변수 미지정"
  exit 1
fi
if [ -z "${MNEME_BACKUP_S3_BUCKET:-}" ]; then
  echo "[backup] FATAL: MNEME_BACKUP_S3_BUCKET 환경변수 미지정"
  exit 1
fi
if [ -z "${AWS_ACCESS_KEY_ID:-}" ] || [ -z "${AWS_SECRET_ACCESS_KEY:-}" ]; then
  echo "[backup] FATAL: AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 미지정"
  exit 1
fi

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TMP_DIR="$(mktemp -d)"
DUMP_FILE="$TMP_DIR/mneme-$STAMP.sql.gz"

echo "[backup] $STAMP pg_dump 시작 (host=$MNEME_DB_HOST db=$MNEME_DB_NAME)"
PGPASSWORD="$MNEME_DB_PASSWORD" pg_dump \
  -h "$MNEME_DB_HOST" -U "$MNEME_DB_USER" -d "$MNEME_DB_NAME" \
  --format=plain --no-owner --no-acl \
  | gzip -9 > "$DUMP_FILE"

SIZE=$(stat -c%s "$DUMP_FILE" 2>/dev/null || stat -f%z "$DUMP_FILE")
echo "[backup] dump 완료: $DUMP_FILE ($SIZE bytes)"

S3_TARGET="s3://$MNEME_BACKUP_S3_BUCKET/$MNEME_BACKUP_S3_PREFIX/$(basename "$DUMP_FILE")"
echo "[backup] 업로드 → $S3_TARGET"

if [ -n "${AWS_ENDPOINT_URL:-}" ]; then
  aws --endpoint-url "$AWS_ENDPOINT_URL" s3 cp "$DUMP_FILE" "$S3_TARGET" --no-progress
else
  aws s3 cp "$DUMP_FILE" "$S3_TARGET" --no-progress
fi

rm -f "$DUMP_FILE"
rmdir "$TMP_DIR"
echo "[backup] 완료: $S3_TARGET"
