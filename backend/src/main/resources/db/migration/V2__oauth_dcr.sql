-- Phase 10 mcp-oauth-dcr
-- RFC 7591 동적 클라이언트 등록(DCR) 지원을 위한 보정 + Authorization Code 지원 컬럼.
-- oauth_clients.user_id NULL 허용: DCR로 등록되는 클라이언트는 발급 시점에 사용자 귀속이 없을 수 있다.

ALTER TABLE oauth_clients ALTER COLUMN user_id DROP NOT NULL;

-- Authorization 흐름의 인가 코드는 짧은 TTL(10분)이므로 인메모리(Caffeine)에 보관한다.
-- 별도 DB 테이블은 두지 않는다. 토큰만 oauth_tokens(V1)에 저장된다.

-- oauth_tokens는 V1 그대로 사용. scope에 "mcp"가 들어간다.
