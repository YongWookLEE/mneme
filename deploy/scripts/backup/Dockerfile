FROM postgres:16-alpine

# pg_dump가 들어 있는 postgres 이미지 위에 awscli + cron 추가.
RUN apk add --no-cache python3 py3-pip dcron tzdata bash \
    && pip3 install --no-cache-dir --break-system-packages awscli

ENV TZ=UTC

COPY backup.sh /scripts/backup.sh
COPY restore.sh /scripts/restore.sh
COPY crontab /etc/crontabs/root

RUN chmod +x /scripts/backup.sh /scripts/restore.sh \
    && mkdir -p /var/log \
    && touch /var/log/mneme-backup.log

# crond를 foreground로 띄우면서 로그를 tail로 보여 준다.
CMD ["sh", "-c", "crond -f -d 8 -L /var/log/mneme-backup.log & exec tail -F /var/log/mneme-backup.log"]
