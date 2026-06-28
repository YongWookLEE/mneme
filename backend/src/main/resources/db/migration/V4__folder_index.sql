-- Phase 21 folder-index
-- 폴더마다 LLM이 합성한 한 줄 요약 + 본문(마크다운)을 보관한다. 폴더 1개당 한 줄(folder_id PK).
-- soft delete는 폴더 자체에 따라간다(폴더 삭제 시 CASCADE).

CREATE TABLE folder_indexes (
    folder_id     UUID PRIMARY KEY REFERENCES folders(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary       TEXT NOT NULL,
    body          TEXT NOT NULL,
    memory_count  INT NOT NULL DEFAULT 0,
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX folder_indexes_user_idx ON folder_indexes(user_id);
