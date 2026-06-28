# step 2 — token-quota-guard

`UsageQuotaGuard`:
- `requireEmbedBudget(userId, estimatedTokens)`: `usage_daily.embed_tokens + estimated > MNEME_TOKEN_LIMIT_EMBED_PER_DAY`이면 차단.
- `requireChatBudget(userId, estimatedTokens)`: 동일 패턴.
- 차단은 `OpenAiException.RateLimited`로 throw → 호출자가 swallow(메모리 저장은 진행, 임베딩만 skip).

EmbeddingService / ChatService 호출 직전에 guard 호출. 예상 토큰은 `text.length / 4` 휴리스틱(영문 평균).
