# Step 1 — Shell layout + API client + 모노크롬 테마

## 범위

- 의존성 추가: `react-router-dom`, `@tanstack/react-query`
- Tailwind config에 Mneme 모노크롬 토큰(neutral 50~900 + ink/surface/border/muted/hover) 추가. 보라/푸른기 금지.
- `src/lib/auth.ts`: localStorage 기반 API 키 저장(`mneme.bearer`). axios/fetch 헤더 자동 부착.
- `src/api/client.ts`: `fetch` wrapper + 401시 키 다시 묻기.
- `src/api/{folders,memories}.ts`: GET 엔드포인트.
- 레이아웃: 좌 폴더 트리(읽기 전용) / 중 메모리 리스트(읽기 전용) / 우 상세 placeholder. 최상단 헤더.
- 라우트: `/` `/keys`(빈 페이지) `/archive`(빈 페이지) `/memory/:extId`(상세). React Router.
- 인증 모달: Bearer 토큰 미설정 시 입력 받음(`mn_...` 또는 OAuth access).

## Acceptance

- `npm run lint`, `npm run typecheck`, `npm run build` 통과.
- 백엔드 띄운 상태에서 `npm run dev` → 5173에서 폴더 트리 + 메모리 리스트 노출.
