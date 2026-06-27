import type { Config } from "tailwindcss";

/**
 * Tailwind 설정.
 *
 * Phase 01은 default 팔레트만 사용. Mneme 디자인 토큰(#0E0E10 / 모노크롬)은
 * phase 11(dashboard-ui)에서 shadcn/ui 도입과 함께 theme.extend.colors에 추가한다.
 */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
} satisfies Config;
