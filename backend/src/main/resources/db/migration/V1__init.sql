-- Mneme V1 init schema (phase 02 step 2)
-- forward-only. 잘못된 마이그레이션은 새 V 버전으로 보정.
-- 모든 UUID는 애플리케이션에서 UUID v7 생성 후 INSERT (DB default 사용 안 함).

-- ─── 확장 ────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── 사용자 ──────────────────────────────────────────────────────
CREATE TABLE users (
    id          UUID PRIMARY KEY,
    google_sub  TEXT UNIQUE NOT NULL,
    email       TEXT NOT NULL,
    locale      TEXT NOT NULL DEFAULT 'ko',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

-- ─── API 키 ──────────────────────────────────────────────────────
CREATE TABLE api_keys (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    key_hash      BYTEA NOT NULL,
    prefix        TEXT NOT NULL,
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX api_keys_prefix_idx ON api_keys(prefix) WHERE revoked_at IS NULL;
CREATE INDEX api_keys_user_idx ON api_keys(user_id);

-- ─── OAuth 클라이언트 (DCR) ─────────────────────────────────────
CREATE TABLE oauth_clients (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id           TEXT UNIQUE NOT NULL,
    client_secret_hash  BYTEA,
    redirect_uris       TEXT[] NOT NULL,
    client_name         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX oauth_clients_user_idx ON oauth_clients(user_id);

-- ─── OAuth 토큰 ──────────────────────────────────────────────────
CREATE TABLE oauth_tokens (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id           TEXT NOT NULL,
    access_hash         BYTEA NOT NULL,
    refresh_hash        BYTEA,
    scope               TEXT,
    expires_at          TIMESTAMPTZ NOT NULL,
    refresh_expires_at  TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX oauth_tokens_user_idx ON oauth_tokens(user_id);
CREATE INDEX oauth_tokens_access_idx ON oauth_tokens(access_hash) WHERE revoked_at IS NULL;

-- ─── 세션 (대시보드) ─────────────────────────────────────────────
CREATE TABLE sessions (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    csrf_token  TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX sessions_user_idx ON sessions(user_id);

-- ─── 폴더 ────────────────────────────────────────────────────────
CREATE TABLE folders (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id   UUID REFERENCES folders(id) ON DELETE RESTRICT,
    path        TEXT NOT NULL,
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, path)
);
CREATE INDEX folders_user_path_idx ON folders(user_id, path);
CREATE INDEX folders_user_parent_idx ON folders(user_id, parent_id);

-- ─── 메모리 ──────────────────────────────────────────────────────
CREATE TABLE memories (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id      UUID NOT NULL REFERENCES folders(id) ON DELETE RESTRICT,
    title          TEXT NOT NULL,
    content        TEXT NOT NULL,
    summary        TEXT,
    embedding      VECTOR(1536),
    tsv            TSVECTOR,
    source_uri     TEXT,
    byte_size      INT NOT NULL,
    model_version  TEXT NOT NULL DEFAULT 'text-embedding-3-small@1',
    archived_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX memories_user_archived_idx ON memories(user_id, archived_at);
CREATE INDEX memories_user_folder_idx ON memories(user_id, folder_id);
CREATE INDEX memories_embedding_idx ON memories USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX memories_tsv_idx ON memories USING gin(tsv);
CREATE INDEX memories_content_trgm_idx ON memories USING gin(content gin_trgm_ops);

CREATE TRIGGER memories_tsv_update BEFORE INSERT OR UPDATE ON memories
    FOR EACH ROW EXECUTE FUNCTION tsvector_update_trigger(tsv, 'pg_catalog.simple', title, content);

-- ─── 태그 ────────────────────────────────────────────────────────
CREATE TABLE tags (
    id       UUID PRIMARY KEY,
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name     TEXT NOT NULL,
    UNIQUE (user_id, name)
);
CREATE INDEX tags_user_idx ON tags(user_id);

CREATE TABLE memory_tags (
    memory_id  UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, tag_id)
);

-- ─── 메모리 링크 (본문 [[wiki-link]] 파싱 결과, MVP 핵심) ────────
-- 본문이 진실. 본문 저장 시 트랜잭션 안에서 동기 갱신.
CREATE TABLE memory_links (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_id     UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    target_id     UUID REFERENCES memories(id) ON DELETE SET NULL,   -- NULL = 깨진 링크
    target_label  TEXT NOT NULL,                                     -- 본문에 적힌 [[...]] 원문
    kind          TEXT NOT NULL DEFAULT 'wiki',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX memory_links_user_source_idx ON memory_links(user_id, source_id);
CREATE INDEX memory_links_user_target_idx ON memory_links(user_id, target_id);
CREATE INDEX memory_links_broken_idx ON memory_links(user_id) WHERE target_id IS NULL;

-- ─── 메모리 버전 (수정 이력) ─────────────────────────────────────
CREATE TABLE memory_versions (
    id         UUID PRIMARY KEY,
    memory_id  UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    edited_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX memory_versions_memory_idx ON memory_versions(memory_id, edited_at DESC);

-- ─── 감사 로그 ───────────────────────────────────────────────────
CREATE TABLE audit_events (
    id           UUID PRIMARY KEY,
    user_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_kind   TEXT NOT NULL,
    action       TEXT NOT NULL,
    target_kind  TEXT,
    target_id    TEXT,
    ip           INET,
    user_agent   TEXT,
    metadata     JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_events_user_idx ON audit_events(user_id, created_at DESC);

-- ─── 사용량 집계 ─────────────────────────────────────────────────
CREATE TABLE usage_daily (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date            DATE NOT NULL,
    embed_tokens    INT NOT NULL DEFAULT 0,
    llm_in_tokens   INT NOT NULL DEFAULT 0,
    llm_out_tokens  INT NOT NULL DEFAULT 0,
    request_count   INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, date)
);
