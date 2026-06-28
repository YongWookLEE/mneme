import type { JSX } from "react";
import Sidebar from "../components/Sidebar";

/**
 * 클라이언트별 MCP 연결 가이드.
 *
 * Bearer 키가 필요한 부분은 자리표시 `<YOUR_BEARER>`로 두고, /keys 페이지에서 발급한 평문을 직접
 * 붙여 넣도록 안내한다. 각 클라이언트가 받는 인증 인자 형태가 달라 명령이 모두 다르다.
 *
 * - Claude Code(CLI): `--header "Authorization: Bearer …"` 직접 부착
 * - Codex CLI: `--bearer-token-env-var <ENV>` — 환경변수 이름을 받는다. 키 자체는 셸에서 export.
 * - ChatGPT: 웹 UI에서 직접 입력
 *
 * 스크린샷은 phase 15 client-validation에서 실제 캡처본을 채워 넣는다.
 *
 * @since phase 12
 */
export default function ConnectGuidePage(): JSX.Element {
  const baseGuess =
    typeof window !== "undefined" ? window.location.origin.replace(":5173", ":8080") : "http://localhost:8080";

  const claudeApiKey = `claude mcp add --transport sse mneme ${baseGuess}/sse \\\n  --header "Authorization: Bearer <YOUR_BEARER>" \\\n  --scope local`;
  const claudeOAuth = `claude mcp add --transport sse mneme ${baseGuess}/sse`;

  const codexApiKey = `# 1. 키를 셸 환경변수로 export\nexport MNEME_API_KEY="<YOUR_BEARER>"\n\n# 2. Codex에 MCP 등록\ncodex mcp add mneme \\\n  --url ${baseGuess}/sse \\\n  --bearer-token-env-var MNEME_API_KEY`;
  const codexOAuth = `codex mcp add mneme --url ${baseGuess}/sse\ncodex mcp login mneme   # 브라우저에서 OAuth 동의\ncodex mcp list`;

  const chatgpt = `# ChatGPT Developer mode\nSettings → Connectors → Developer mode → Create\n  Name: Mneme\n  MCP server URL: ${baseGuess}/sse\n  Authentication: OAuth   ← 권장 (또는 API key + Bearer 토큰)`;

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">MCP 연결 가이드</h1>
        <p className="mb-6 text-xs text-ink-400">
          먼저 <a href="/keys" className="underline">/keys</a>에서 키를 발급하세요. 아래 스니펫의
          <code className="mx-1 rounded bg-ink-700 px-1">&lt;YOUR_BEARER&gt;</code>를 평문 키로 치환합니다.
          OAuth는 별도 입력 없이 브라우저 동의 흐름으로 연결됩니다(phase 10 mcp-oauth-dcr).
          스크린샷은 실제 연결 검증(phase 15) 시 추가됩니다.
        </p>

        <ClientCard
          title="Claude Code (CLI) — OAuth"
          subtitle="권장. 별도 토큰 없이 브라우저 동의."
          snippet={claudeOAuth}
        />
        <ClientCard
          title="Claude Code (CLI) — API 키"
          subtitle="--header로 Bearer를 직접 부착"
          snippet={claudeApiKey}
        />
        <ClientCard
          title="Codex CLI — OAuth"
          subtitle="권장. add 후 login으로 브라우저 동의."
          snippet={codexOAuth}
        />
        <ClientCard
          title="Codex CLI — API 키"
          subtitle="--bearer-token-env-var는 토큰 자체가 아닌 환경변수 이름을 받는다."
          snippet={codexApiKey}
        />
        <ClientCard
          title="ChatGPT Developer mode"
          subtitle="설정 → Connectors → Developer mode → Create"
          snippet={chatgpt}
        />
      </main>
    </>
  );
}

interface ClientCardProps {
  title: string;
  subtitle: string;
  snippet: string;
}

/** 클라이언트 가이드 카드. 스니펫 복사 버튼 포함. */
function ClientCard({ title, subtitle, snippet }: ClientCardProps): JSX.Element {
  return (
    <section className="mb-4 rounded border border-ink-700 bg-ink-800 p-4">
      <header className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-medium text-ink-100">{title}</h2>
          <div className="text-xs text-ink-400">{subtitle}</div>
        </div>
        <button
          type="button"
          onClick={() => {
            void navigator.clipboard.writeText(snippet);
          }}
          className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
        >
          복사
        </button>
      </header>
      <pre className="overflow-auto whitespace-pre-wrap break-words rounded border border-ink-700 bg-ink-900 p-3 font-mono text-xs text-ink-100">
        {snippet}
      </pre>
    </section>
  );
}
