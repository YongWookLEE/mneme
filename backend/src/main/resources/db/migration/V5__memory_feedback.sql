-- Phase 23 review-feedback
-- 사용자가 메모리 단위로 분류·요약·태그·인덱스 결과에 남기는 피드백.
-- value: 'up' 또는 'down'. target은 평가 대상이 무엇이었는지 분류.

CREATE TABLE memory_feedback (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    memory_id   UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    target      TEXT NOT NULL,
    value       TEXT NOT NULL CHECK (value IN ('up', 'down')),
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX memory_feedback_user_idx ON memory_feedback(user_id, created_at DESC);
CREATE INDEX memory_feedback_memory_idx ON memory_feedback(memory_id);
