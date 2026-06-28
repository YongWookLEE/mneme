-- Phase 11 step 3 보정
-- audit_events.ip는 INET이었으나 JDBC 드라이버가 null 파라미터를 varchar로 보내 PostgreSQL이
-- "column ip is of type inet but expression is of type character varying"으로 거부한다.
-- 운영상 IP는 단순 문자열로 저장·열람하면 충분하므로 TEXT로 완화한다.

ALTER TABLE audit_events ALTER COLUMN ip TYPE text USING ip::text;
