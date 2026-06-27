# P04 Step 2: api-key-service

> Task 2 of phase 04. `ApiKeyService`: 발급/목록/폐기/이름수정/회전. `@Transactional`. 회전은 단순 형태(폐기 + 신규)로 시작. ADR-005의 14일 grace period는 phase 08(security-controls) 또는 별도 phase에서 도입.
>
> 감사 이벤트 발행은 step 5에서 인프라 구축 후 service에 주입. 본 step에서는 logback debug 로깅으로 표시만.

## 산출물

- `com.mneme.auth.ApiKeyService` (@Service @Transactional)
- 메서드: issue / listActive / revoke / rename / rotate
- 발급 결과 DTO: `IssuedKey(plaintext, key)` — plaintext는 1회 노출
