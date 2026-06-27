# P04 Step 1: api-key-generator

> Task 1 of phase 04. `com.mneme.auth.ApiKeyGenerator`: 32B SecureRandom → base62 인코딩 → `mn_` prefix 결합 → 평문 키. sha256 해시 + 앞 8자 식별자(prefix 컬럼)도 함께 산출. 단위 테스트로 round-trip(생성→해시 검증) 확인.

## 산출물

- `com.mneme.auth.ApiKeyGenerator` (Spring `@Component`)
- 평문 키 형식: `mn_<43자 base62>` (32B → ~43자)
- `verify(plaintext, storedHashHex): Boolean` 헬퍼

## Acceptance

- 빌드 + 테스트 + ktlint
