# Step 3: id-utility-uuid-v7-base32

> Task 3 of phase 02. `com.mneme.id` 모듈에 (1) UUID v7 생성기, (2) Crockford base32 인코더/디코더, (3) prefix 매핑(`mem_`/`fld_`/`tag_`/`key_`/`usr_`/`oac_`/`oat_`/`ses_`), (4) 외부 ID ↔ UUID 변환 유틸을 작성한다. 단위 테스트는 round-trip + 경계 케이스.

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` "외부 ID" 섹션
- `docs/ADR.md` ADR-012 (UUID v7 + base32)

## 작업

### 3.1 `com.mneme.id.UuidV7` (생성기)

- 표준 UUID v7 비트 레이아웃: `[unix_ms_48][version_4][rand_a_12][variant_2][rand_b_62]`
- `nowMicros()` 또는 `currentTimeMillis()` 기반. 동일 ms 내 단조 증가 보장(rand_a를 카운터로 사용)
- 스레드 안전(synchronized 또는 AtomicLong)

### 3.2 `com.mneme.id.Base32Crockford`

- Crockford alphabet: `0123456789ABCDEFGHJKMNPQRSTVWXYZ` (대문자 32자, I/L/O/U 제외)
- 인코딩은 소문자 출력. 디코딩은 대소문자 무시 + 혼동 문자(`O→0`, `I/L→1`) 자동 매핑
- 26자(= 130 비트) 출력, 첫 2비트는 0 padding

### 3.3 `com.mneme.id.PrefixedId`

- `value object`로 prefix + uuid 보관
- `format()` → `"<prefix>_<base32>"`
- `parse(extId: String, expectedPrefix: String)` → 검증 후 UUID 추출. prefix 불일치 시 `IllegalArgumentException`
- prefix 목록:
  - `mem_` Memory, `fld_` Folder, `tag_` Tag, `key_` ApiKey, `usr_` User, `oac_` OAuthClient, `oat_` OAuthToken, `ses_` Session, `lnk_` MemoryLink, `ver_` MemoryVersion, `aud_` AuditEvent

### 3.4 `com.mneme.id.IdFactory`

- Spring `@Component`. UUID v7 생성 + prefix 결합
- `newMemoryId()`, `newFolderId()` 등 메서드별 헬퍼

### 3.5 단위 테스트

`com.mneme.id.UuidV7Test`:
- 버전 비트 7 검증
- 단조 증가(같은 ms 내 1000개 생성 시 ascending)
- 멀티스레드 안전(2개 스레드 1000개씩 → 중복 없음)

`com.mneme.id.Base32CrockfordTest`:
- round-trip(랜덤 UUID 100개)
- 대소문자 무시 디코딩
- 혼동 문자 (`O→0`, `I/L→1`) 매핑
- 잘못된 길이 입력 시 예외

`com.mneme.id.PrefixedIdTest`:
- format/parse round-trip
- prefix 불일치 예외
- 잘못된 base32 예외

## Acceptance Criteria

```bash
./gradlew :backend:test :backend:ktlintCheck
```

새 테스트 클래스 3개 모두 PASSED. ktlint 통과.

## 검증 절차

1. Acceptance 명령 통과.
2. `com/mneme/id/` 아래에 4개 main 파일 + 3개 test 파일.
3. 모든 함수에 한국어 KDoc.
4. 성공 시 index.json step 3 completed.

## 금지사항

- `java.util.UUID.randomUUID()` 그대로 사용 금지. **이유: UUID v4는 시간순 정렬 불가**.
- 외부 라이브러리(uuid-creator 등) 추가 금지. **이유: 의존성 최소화. JDK 표준 + 직접 구현으로 충분**.
- prefix 문자열 하드코딩 분산 금지. **이유: 한 곳(`PrefixedId.Prefix`)에서만 정의**.
