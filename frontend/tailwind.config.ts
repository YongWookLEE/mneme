import type { Config } from "tailwindcss";
import typography from "@tailwindcss/typography";

/**
 * Tailwind 설정.
 *
 * Phase 01은 default 팔레트만 사용. Mneme 디자인 토큰(#0E0E10 / 모노크롬)은
 * phase 11(dashboard-ui)에서 shadcn/ui 도입과 함께 theme.extend.colors에 추가한다.
 */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          "900": "#0a0a0b",
          "800": "#111113",
          "700": "#1a1a1d",
          "600": "#26262a",
          "500": "#3a3a40",
          "400": "#5a5a62",
          "300": "#7e7e87",
          "200": "#a8a8af",
          "100": "#d4d4d7",
          "50": "#f5f5f6",
        },
      },
    },
  },
  plugins: [typography],
} satisfies Config;
