# step 3 — pii-log-masking

Logback `PatternLayoutEncoder`에 커스텀 `Converter` 등록:
- `mn_[A-Za-z0-9]+` → `mn_********`
- `sk-[A-Za-z0-9_-]+` → `sk-********`
- 이메일 → `***@***.<tld>`

`%maskedMsg` 패턴 토큰. `logback-spring.xml` 작성.

테스트: 직접 logger.info("key=mn_LIVETEST...") 호출 후 캡처 라이터로 출력 확인.
