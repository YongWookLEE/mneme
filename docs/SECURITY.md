# Security Policy

## 지원 버전

Mneme는 단일 main 브랜치만 유지한다. 모든 보안 수정은 main에서 발생하며, 셀프호스팅 사용자는 정기적으로 최신 main을 pull해야 한다. 별도 LTS 분기 없음.

## 보안 모델 요약

상세는 [`docs/ARCHITECTURE.md` 보안 섹션](ARCHITECTURE.md#보안)과 [`docs/ADR.md` ADR-009/010](ADR.md) 참고.

### 위협 모델

| 위협 | 대응 |
|---|---|
| 사용자 데이터 교차 노출 | 모든 리포지토리 메서드 user_id 강제, 격리 회귀 테스트 자동 검증, 권한 위반 404 응답 |
| API 키 도용 | sha256 해시 저장, 1회 노출, 폐기 즉시 401, 키 도용 의심 패턴 감지 |
| OpenAI 비용 폭주 | 사용자별 분당/일별 호출 + 일일 LLM 토큰 한도, 초과 시 차단 + 이메일 알림 |
| 프롬프트 인젝션 | 시스템 프롬프트 코드 상수, 사용자 입력 인용 블록, 응답 우회 패턴 감지 |
| 민감정보 로그 유출 | Authorization/Cookie/메모리 본문/임베딩 자동 마스킹, 별도 감사 로그 |
| OAuth 리다이렉트 부정 | redirect_uris 화이트리스트 엄격 매칭, 와일드카드 금지 |
| 외부 호출 중 트랜잭션 점유 | 컨트롤러 `@Transactional` 금지, 외부 호출은 트랜잭션 밖 |
| SQL/프롬프트 인젝션 | prepared statement 강제, 사용자 입력 직접 보간 금지 |
| 세션 탈취 | 쿠키 Secure/HttpOnly/SameSite=Lax + CSRF 토큰 헤더 검증 |
| 클릭재킹 | X-Frame-Options: DENY + CSP frame-ancestors 'none' |
| MIME confusion | X-Content-Type-Options: nosniff |
| HTTPS 다운그레이드 | HTTPS 강제, HSTS preload (운영 도메인 확정 후) |

### 비밀 관리

- `.env` 권한 `600`, git 제외
- 운영은 호스팅 사업자 secrets manager 또는 Docker secrets 권장
- DB 비밀번호, OpenAI 키, OAuth client secret 평문 로그 금지

### 데이터 처리

- **저장 위치**: 사용자 메모리는 본인 인스턴스 Postgres에만. 외부 전송은 OpenAI API 호출 시 분류·요약·임베딩 입력으로만 사용
- **보존**: archived 메모리는 영구 보존(soft delete). 계정 삭제 시 30일 유예 후 완전 제거
- **백업**: 일 1회 외부 객체 스토리지(설정 시), 23개 보관
- **export**: 사용자가 언제든 zip + manifest로 다운로드
- **계정 삭제**: 사용자 요청 시 `users.deleted_at` 마킹 → 30일 후 cascade delete

## 취약점 신고

보안 취약점을 발견하셨다면 **공개 이슈 대신** 다음 주소로 비공개 보고 부탁드립니다:

- 이메일: `<관리자 이메일>` (`MNEME_ADMIN_EMAILS` 환경변수의 첫 주소)

다음 정보를 포함해주시면 좋습니다:
- 영향받는 컴포넌트 / 엔드포인트
- 재현 절차
- 가능한 영향 범위 (데이터 노출, RCE, DoS 등)
- 제안하는 완화 방안 (있다면)

응답은 48시간 이내 1차 확인, 7일 이내 진단 회신을 목표로 합니다. 비상업 1인 유지보수 프로젝트라 즉시 대응이 어려운 경우가 있을 수 있습니다.

CVE 발급이 필요한 경우 신고자와 협의 후 진행합니다.

## 셀프호스팅 사용자가 직접 확인할 것

- [ ] `.env`의 모든 비밀이 강한 무작위 값
- [ ] HTTPS 활성화 (Caddy 기본 동작, 도메인 필요)
- [ ] `MNEME_FRONTEND_ORIGIN`이 자신의 도메인으로만 설정됨
- [ ] Google OAuth Authorized redirect URI가 실제 도메인과 정확히 일치
- [ ] OpenAI 플랫폼에 월 사용량 한도 설정
- [ ] DB 컨테이너 포트 외부 비노출 (compose 기본은 internal network만)
- [ ] 백업 잡 활성화 (`MNEME_BACKUP_S3_*` 설정)
- [ ] 정기적으로 `git pull && docker compose pull && docker compose up -d`
- [ ] 의존성 보안 알림 구독 (Dependabot, OpenAI/Google 공지)

## 알려진 한계

- in-memory rate limit은 서버 재시작 시 카운터 리셋 → 한도 우회 가능성. 다중 인스턴스 운영 시 Redis 전환 필요(현재 단일 인스턴스 가정)
- 메모리 본문은 기본적으로 평문 저장. 컬럼 단위 at-rest 암호화는 후속 phase(34)
- 첨부 파일 미지원 → 외부 이미지 URL은 사용자 책임
- 1인 유지보수라 보안 패치 적용 지연 가능. 셀프호스팅 사용자는 자기 인스턴스 업그레이드 책임

## 참고 표준

- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) Level 1 지향
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749) / [RFC 7591 DCR](https://datatracker.ietf.org/doc/html/rfc7591)
- [PostgreSQL 보안](https://www.postgresql.org/docs/16/auth-pg-hba-conf.html)
- [MDN 보안 헤더](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers#security)
