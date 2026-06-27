# P05 Step 4: isolation-regression-base

> `IsolationRegressionTest` 클래스 베이스 — 모든 REST 엔드포인트에 대해 사용자 A → B 데이터 접근 → 404 검증의 골격. Testcontainers 호환 이슈로 본 step에서는 단위 테스트 가능한 서비스 계층 격리 검증만 작성하고, 풀 REST 회귀는 phase 08(security-controls)에서 도커-자바 업그레이드 후 도입.

## 산출물

- `IsolationRegressionTest.kt` (skeleton, 단위 테스트로 시작 — 서비스 레이어에서 다른 userId로 접근 시 404)
- 본격 REST 엔드포인트 격리 회귀는 phase 08 예약
- 신규 엔드포인트 추가 시 본 클래스에 케이스 추가 의무화는 phase 08에서 강제(코드 리뷰 체크리스트)

## Acceptance

- 빌드 + 스모크 + ktlint
- IsolationRegressionTest 단위 케이스(folder/memory/tag service)가 다른 userId 접근 시 NOT_FOUND(ResponseStatusException) 검증
