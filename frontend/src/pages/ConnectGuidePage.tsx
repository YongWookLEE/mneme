import type { JSX } from "react";
import Sidebar from "../components/Sidebar";

/**
 * 클라이언트별 MCP 연결 가이드.
 *
 * Bearer 키가 필요한 부분은 자리표시 `<YOUR_BEARER>`로 두고, /keys 페이지에서 발급한 평문을 직접
 * 붙여 넣도록 안내한다. 스크린샷은 phase 15 client-validation에서 실제 캡처본을 채워 넣는다.
 *
 * @since phase 12
 */
export default function ConnectGuidePage(): JSX.Element {
  const baseGuess = typeof window !== "undefined" ? window.location.origin.replace(":5173", ":8080") : "http://localhost:8080";
  const claude = JSON.stringify(
    {
      mcpServers: {
        mneme: {
          transport: "sse",
          url: `${baseGuess}/sse`,
          headers: { Authorization: "Bearer <YOUR_BEARER>" },
        },
      },
    },
    null,
    2,
  );
  const codex = `codex mcp add mneme \\\n  --transport sse \\\n  --url ${baseGuess}/sse \\\n  --header "Authorization: Bearer <YOUR_BEARER>"`;
  const chatgpt = `# ChatGPT Developer mode\n# Settings → Connectors → Add custom MCP\nName: Mneme\nTransport: SSE\nURL: ${baseGuess}/sse\nAuthorization: Bearer <YOUR_BEARER>`;

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">MCP 연결 가이드</h1>
        <p className="mb-6 text-xs text-ink-400">
          먼저 <a href="/keys" className="underline">/keys</a>에서 키를 발급하세요. 아래 스니펫의
          <code className="mx-1 rounded bg-ink-700 px-1">&lt;YOUR_BEARER&gt;</code>를 평문 키로 치환합니다.
          스크린샷은 실제 연결 검증(phase 15) 시 추가됩니다.
        </p>

        <ClientCard
          title="Claude Desktop"
          subtitle="~/Library/Application Support/Claude/claude_desktop_config.json"
          snippet={claude}
        />
        <ClientCard
          title="Codex CLI"
          subtitle="터미널에서 한 줄 명령"
          snippet={codex}
        />
        <ClientCard
          title="ChatGPT Developer mode"
          subtitle="설정 → Connectors → Add custom MCP"
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
      <div className="mb-3 rounded border border-ink-700 bg-ink-900 p-3 text-xs text-ink-300">
        <strong>스크린샷 자리</strong> — phase 15 client-validation에서 실제 캡처 추가
      </div>
      <pre className="overflow-auto whitespace-pre-wrap break-words rounded border border-ink-700 bg-ink-900 p-3 font-mono text-xs text-ink-100">
        {snippet}
      </pre>
    </section>
  );
}
