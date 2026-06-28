# step 4 — memory-integration

## 목적

`MemoryService.create/update` 시 본문 임베딩 + 요약을 트랜잭션 밖에서 계산해 트랜잭션 안에서 저장. `usage_daily` 일일 토큰 집계.

## 범위

- `MemoryWriteFacade` (`@Component`, non-`@Transactional`): 외부 호출 → `MemoryService` 호출. 컨트롤러는 facade를 부른다.
- 임베딩 컬럼은 JPA에 직접 매핑 안 되어 있으므로 (phase 02 보류 사항) `EntityManager` 네이티브 쿼리(`UPDATE memories SET embedding = ?::vector WHERE id = ? AND user_id = ?`)로 별도 갱신.
- `UsageDailyRepository.incrementEmbedTokens(userId, date, tokens)`: `INSERT … ON CONFLICT DO UPDATE` upsert.
- 요약/태그 제안은 실패 시 무시(MVP). 본문 256KB 상한과 동일.

## Acceptance

- `POST /api/memories` → memories.embedding이 `NOT NULL` (확인 SQL).
- `usage_daily(date=today)` row 증가.
