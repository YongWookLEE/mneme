# step 2 — hybrid-score-service

`SearchService.search(userId, query, filters, limit)`:
1. EmbeddingService로 query 임베딩 계산(Caffeine 캐시 활용).
2. 단일 네이티브 SQL:

```sql
SELECT id, title, content, summary, folder_id, version, created_at, updated_at,
       (:alpha * (1 - (embedding <=> :qvec))) +
       (:beta  * ts_rank(tsv, plainto_tsquery('simple', :q))) +
       (:gamma * similarity(content, :q)) AS score
FROM memories
WHERE user_id = :uid AND archived_at IS NULL
  AND (:folder_id IS NULL OR folder_id = :folder_id)
  AND (:from IS NULL OR created_at >= :from)
  AND (:to IS NULL OR created_at <= :to)
ORDER BY score DESC
LIMIT :limit
```

태그 필터는 EXISTS subquery로 추가. 가중치는 `mneme.search.*` 설정(α=0.6, β=0.3, γ=0.1 기본).
