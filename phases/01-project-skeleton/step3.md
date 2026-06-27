# Step 3: frontend-vite-react-skeleton

> Task 3 of phase 01. Vite + React 18 + TypeScript 5 + Tailwind 3 프론트엔드 부트스트랩. 라우팅·인증·shadcn은 다음 phase. 이 step의 끝에서는 `npm run dev` → 5173에서 "Mneme — coming soon" 화면이 보이고, Vitest 스모크 1개가 통과한다.

## 읽어야 할 파일

- `docs/ARCHITECTURE.md` (frontend 디렉터리 구조)
- `docs/ADR.md` ADR-007 (React + Vite + shadcn/ui)
- `docs/UI_GUIDE.md` (색상 토큰 #0B0D10, #7C9CFF — Tailwind 토큰화는 phase 11. 여기선 단순 default Tailwind)
- `CLAUDE.md` (프론트엔드 명령어 + 명령어 표)

## 작업

### 3.1 `frontend/package.json`

```json
{
  "name": "@mneme/frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .ts,.tsx --max-warnings 0",
    "lint:fix": "eslint . --ext .ts,.tsx --fix",
    "test": "vitest",
    "typecheck": "tsc -b --noEmit"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^6.5.0",
    "@testing-library/react": "^16.0.1",
    "@types/react": "^18.3.11",
    "@types/react-dom": "^18.3.0",
    "@typescript-eslint/eslint-plugin": "^8.8.1",
    "@typescript-eslint/parser": "^8.8.1",
    "@vitejs/plugin-react": "^4.3.2",
    "autoprefixer": "^10.4.20",
    "eslint": "^8.57.1",
    "eslint-plugin-react-hooks": "^4.6.2",
    "eslint-plugin-react-refresh": "^0.4.12",
    "jsdom": "^25.0.1",
    "postcss": "^8.4.47",
    "tailwindcss": "^3.4.13",
    "typescript": "^5.6.2",
    "vite": "^5.4.8",
    "vitest": "^2.1.2"
  }
}
```

### 3.2 `frontend/tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "useDefineForClassFields": true,
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "types": ["vitest/globals", "@testing-library/jest-dom"],
    "baseUrl": "./src",
    "paths": {
      "@/*": ["*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### 3.3 `frontend/tsconfig.node.json`

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "types": ["node"]
  },
  "include": ["vite.config.ts", "tailwind.config.ts", "postcss.config.js"]
}
```

### 3.4 `frontend/vite.config.ts`

```ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

/**
 * Vite 빌드/dev 서버 설정.
 *
 * - dev 서버는 5173에서 모든 인터페이스 바인딩(Docker 컨테이너 내부 노출 위해 host=true).
 * - `/api`, `/actuator`, `/mcp` 경로는 백엔드 8080으로 프록시(로컬 dev에서 CORS 회피).
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": "http://localhost:8080",
      "/actuator": "http://localhost:8080",
      "/mcp": "http://localhost:8080",
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: ["./src/__tests__/setup.ts"],
  },
});
```

### 3.5 `frontend/index.html`

```html
<!doctype html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Mneme</title>
  </head>
  <body class="bg-neutral-950 text-neutral-100">
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

### 3.6 `frontend/src/main.tsx`

```tsx
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles/globals.css";

/**
 * 프론트엔드 진입점.
 *
 * React 18 createRoot로 #root에 App을 마운트한다.
 * 라우터/QueryClient/i18n Provider는 phase 03(인증) 이후에 추가한다.
 */
const container = document.getElementById("root");
if (!container) {
  throw new Error("root container not found");
}

ReactDOM.createRoot(container).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

### 3.7 `frontend/src/App.tsx`

```tsx
/**
 * Mneme 루트 컴포넌트 (Phase 01 placeholder).
 *
 * 실제 페이지(Dashboard, Login, ApiKeys 등)는 phase 11에서 도입한다.
 * 지금은 골격이 살아있다는 신호만 보여준다.
 */
export default function App(): JSX.Element {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-semibold tracking-tight">Mneme</h1>
        <p className="mt-3 text-sm text-neutral-400">
          여러 AI 클라이언트가 공유하는 영구 기억층 — coming soon.
        </p>
      </div>
    </main>
  );
}
```

### 3.8 `frontend/src/styles/globals.css`

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
  color-scheme: dark;
}

html,
body,
#root {
  height: 100%;
}
```

### 3.9 `frontend/tailwind.config.ts`

```ts
import type { Config } from "tailwindcss";

/**
 * Tailwind 설정.
 *
 * Phase 01은 default 팔레트만 사용. Mneme 디자인 토큰(#0B0D10 / #7C9CFF)은
 * phase 11(dashboard-ui)에서 shadcn/ui 도입과 함께 theme.extend.colors에 추가한다.
 */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
} satisfies Config;
```

### 3.10 `frontend/postcss.config.js`

```js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

### 3.11 `frontend/.eslintrc.cjs`

```js
module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  parser: "@typescript-eslint/parser",
  parserOptions: {
    ecmaVersion: "latest",
    sourceType: "module",
    project: ["./tsconfig.json", "./tsconfig.node.json"],
    tsconfigRootDir: __dirname,
  },
  plugins: ["@typescript-eslint", "react-hooks", "react-refresh"],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react-hooks/recommended",
  ],
  rules: {
    "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
    "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
  },
  ignorePatterns: ["dist", "node_modules", "vite.config.ts", "tailwind.config.ts"],
};
```

### 3.12 테스트 셋업 + 스모크 테스트

`frontend/src/__tests__/setup.ts`:

```ts
import "@testing-library/jest-dom/vitest";
```

`frontend/src/__tests__/smoke.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "../App";

/**
 * 앱 루트 컴포넌트 스모크 테스트.
 *
 * Phase 01에서는 "Mneme" 텍스트가 표시되는 것이 곧 부팅 성공 신호.
 */
describe("App", () => {
  it("Mneme 타이틀이 렌더된다", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /Mneme/i })).toBeInTheDocument();
  });
});
```

### 3.13 의존성 설치 + 검증

```bash
npm --prefix frontend install
npm --prefix frontend run lint
npm --prefix frontend run test -- --run
npm --prefix frontend run typecheck
npm --prefix frontend run build
```

기대:

- `lint`: 경고/에러 0
- `test`: `1 passed`
- `typecheck`: 출력 없음 + exit 0
- `build`: `frontend/dist/` 생성, `index.html` + `assets/`

dev 서버 수동 확인 (선택):

```bash
npm --prefix frontend run dev &
sleep 3
curl -sf http://localhost:5173/ | grep -q "Mneme"
kill %1
```

### 3.14 frontend `.gitignore`

존재하지 않으면 `frontend/.gitignore` 생성:

```gitignore
node_modules/
dist/
.eslintcache
*.log
.vite/
```

(루트 `.gitignore`로 커버되어 있으면 별도 작성 생략.)

### 3.15 커밋

```bash
git add frontend/ .gitignore
git commit -m "chore(frontend): bootstrap vite react typescript tailwind skeleton

- React 18 + Vite 5 + TS 5 + Tailwind 3
- ESLint(typescript + react-hooks) max-warnings 0
- Vitest + Testing Library 스모크 1개
- /api, /actuator, /mcp 백엔드 프록시
- placeholder App 'Mneme — coming soon'

Refs: ADR-007"
```

## Acceptance Criteria

```bash
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run test -- --run
npm --prefix frontend run build
```

네 명령 모두 exit 0. `frontend/dist/index.html` 존재.

## 검증 절차

1. Acceptance Criteria 모두 통과.
2. App.tsx / main.tsx / vite.config.ts에 한국어 JSDoc/주석이 있는지 확인.
3. ESLint 경고 0개(`--max-warnings 0`이라 경고만 있어도 실패함을 확인).
4. `frontend/package.json`의 모든 deps 버전이 명시된 그대로인지 확인(추정 또는 latest 금지).
5. 성공 시 `phases/01-project-skeleton/index.json`의 step 3 갱신:
   ```json
   { "step": 3, "name": "frontend-vite-react-skeleton", "status": "completed", "summary": "vite 5 + react 18 + ts 5 + tailwind 3 + vitest 1 스모크 PASSED, build dist 생성" }
   ```

## 금지사항

- shadcn/ui CLI 초기화하지 마라. **이유: phase 11에서 디자인 토큰과 함께 도입**.
- react-router-dom, @tanstack/react-query, react-i18next, axios를 이 step에서 추가하지 마라. **이유: phase 03 이후**.
- Playwright 설치하지 마라. **이유: phase 15(client-validation)에서 E2E와 함께**.
- 컴포넌트에 emoji를 넣지 마라. **이유: 전역 규칙 — 사용자가 명시 요청 시에만**.
- `any` 타입 사용 금지(테스트 파일 포함). **이유: strict 보장**.
