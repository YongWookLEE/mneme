# P04 Step 5: audit-event-base

> Task 5 of phase 04. AuditEvent JPA 엔티티 + 리포지토리 + `AuditPublisher` 컴포넌트. 본 step에서는 키 이벤트(`key.created`, `key.revoked`, `key.rotated`, `key.renamed`)만 활용. 다른 이벤트(memory.*, oauth.*)는 해당 phase에서 추가.

## 산출물

- `com.mneme.observability.AuditEvent` 엔티티 (audit_events 테이블)
- `com.mneme.observability.AuditEventRepository`
- `com.mneme.observability.AuditPublisher` (@Component) — 이벤트 INSERT
- `ApiKeyService`에 AuditPublisher 주입, 이슈/폐기/회전 시점에 record

## Acceptance

- 빌드 + 스모크 + ktlint
