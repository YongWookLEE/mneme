#!/usr/bin/env sh
# Mneme 복원 스크립트 — 백업 객체를 받아서 새 DB에 적용한다.
#
# 사용:
#   ./restore.sh <backup-object-name>
#   예) ./restore.sh mneme-20260628T030000Z.sql.gz
#
# CRITICAL: 이 스크립트는 대상 DB의 기존 데이터를 보존하지 않는다. 별도 DB 또는
# 새 환경에서 실행하라. 운영 DB에 직접 부어 넣지 마라.

set -eu

if [ "$#" -lt 1 ]; then
  echo "사용: $0 <backup-object-name>"
  exit 2
fi
OBJECT="$1"

: "${MNEME_DB_HOST:=postgres}"
: "${MNEME_DB_USER:=mneme}"
: "${MNEME_DB_NAME:=mneme}"
: "${MNEME_BACKUP_S3_PREFIX:=mneme-backup}"
: "${AWS_DEFAULT_REGION:=auto}"

for v in MNEME_DB_PASSWORD MNEME_BACKUP_S3_BUCKET AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY; do
  eval val=\${$v:-}
  if [ -z "$val" ]; then
    echo "[restore] FATAL: $v 미지정"; exit 1
  fi
done

TMP_DIR="$(mktemp -d)"
LOCAL="$TMP_DIR/$OBJECT"
S3_SRC="s3://$MNEME_BACKUP_S3_BUCKET/$MNEME_BACKUP_S3_PREFIX/$OBJECT"

echo "[restore] 다운로드 ← $S3_SRC"
if [ -n "${AWS_ENDPOINT_URL:-}" ]; then
  aws --endpoint-url "$AWS_ENDPOINT_URL" s3 cp "$S3_SRC" "$LOCAL" --no-progress
else
  aws s3 cp "$S3_SRC" "$LOCAL" --no-progress
fi

echo "[restore] psql 적용 (host=$MNEME_DB_HOST db=$MNEME_DB_NAME)"
gunzip -c "$LOCAL" | PGPASSWORD="$MNEME_DB_PASSWORD" psql \
  -h "$MNEME_DB_HOST" -U "$MNEME_DB_USER" -d "$MNEME_DB_NAME"

rm -f "$LOCAL"
rmdir "$TMP_DIR"
echo "[restore] 완료"
