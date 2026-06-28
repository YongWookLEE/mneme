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
    proxy: (() => {
      const target = process.env.VITE_DEV_API_TARGET ?? "http://localhost:8080";
      return {
        "/api": target,
        "/actuator": target,
        "/mcp": target,
        "/sse": target,
        "/oauth": target,
        "/oauth2": target,
        "/login": target,
      };
    })(),
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
