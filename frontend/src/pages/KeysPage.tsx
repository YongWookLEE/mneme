import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type JSX } from "react";
import {
  fetchKeys,
  issueKey,
  revokeKey,
  rotateKey,
  type IssuedKeyDto,
  type KeyDto,
} from "../api/keys";
import Sidebar from "../components/Sidebar";

/**
 * `/keys` 페이지 — API 키 발급/폐기/회전 + MCP 연결 명령 빌더.
 *
 * 발급된 평문 키는 응답 시점에 1회만 노출되므로 화면에서도 한 번만 보여주고, 사용자가 복사한 뒤
 * 폐기하도록 권장한다. MCP 연결 가이드는 Claude.ai / ChatGPT / Codex 각 명령을 복사 가능한 형태로 노출.
 *
 * @since phase 11 step 3
 */
export default function KeysPage(): JSX.Element {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["keys"],
    queryFn: fetchKeys,
  });

  const [name, setName] = useState("");
  const [issued, setIssued] = useState<IssuedKeyDto | null>(null);

  const issueMutation = useMutation({
    mutationFn: async (newName: string) => issueKey(newName),
    onSuccess: (k) => {
      setIssued(k);
      setName("");
      queryClient.invalidateQueries({ queryKey: ["keys"] });
    },
  });

  const revokeMutation = useMutation({
    mutationFn: async (extId: string) => revokeKey(extId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["keys"] }),
  });

  const rotateMutation = useMutation({
    mutationFn: async (extId: string) => rotateKey(extId),
    onSuccess: (k) => {
      setIssued(k);
      queryClient.invalidateQueries({ queryKey: ["keys"] });
    },
  });

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">API 키</h1>
        <p className="mb-4 text-xs text-ink-400">
          `mn_…` Bearer 키. 발급 직후 화면에서 한 번만 보이고, 이후에는 prefix 8자만 남는다.
        </p>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (name.trim()) issueMutation.mutate(name.trim());
          }}
          className="mb-6 flex gap-2"
        >
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="키 이름 (예: claude-desktop)"
            className="flex-1 rounded border border-ink-600 bg-ink-800 px-3 py-1.5 text-sm outline-none focus:border-ink-400"
          />
          <button
            type="submit"
            disabled={issueMutation.isPending || !name.trim()}
            className="rounded bg-ink-100 px-3 py-1.5 text-sm font-medium text-ink-900 hover:bg-white disabled:opacity-50"
          >
            발급
          </button>
        </form>

        {issued && (
          <div className="mb-6 space-y-3 rounded border border-yellow-700 bg-yellow-900/20 p-4 text-sm">
            <div className="font-medium text-yellow-200">
              새 키 — 평문은 지금만 표시됩니다
            </div>
            <pre className="overflow-auto rounded border border-ink-700 bg-ink-900 p-2 font-mono text-xs text-ink-100">
              {issued.plaintext}
            </pre>
            <McpConnectBuilder bearer={issued.plaintext} />
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => {
                  void navigator.clipboard.writeText(issued.plaintext);
                }}
                className="rounded border border-ink-600 px-3 py-1.5 text-xs hover:bg-ink-700"
              >
                평문 복사
              </button>
              <button
                type="button"
                onClick={() => setIssued(null)}
                className="rounded border border-ink-600 px-3 py-1.5 text-xs hover:bg-ink-700"
              >
                숨기기
              </button>
            </div>
          </div>
        )}

        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && data.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            등록된 키가 없습니다.
          </div>
        )}
        <ul className="space-y-2">
          {data?.map((k: KeyDto) => (
            <li
              key={k.extId}
              className="flex items-center justify-between gap-3 rounded border border-ink-700 bg-ink-800 p-3"
            >
              <div className="min-w-0">
                <div className="truncate font-medium text-ink-100">{k.name}</div>
                <div className="font-mono text-xs text-ink-400">
                  {k.prefix}… · {new Date(k.createdAt).toLocaleDateString("ko-KR")}
                  {k.lastUsedAt
                    ? ` · 마지막 사용 ${new Date(k.lastUsedAt).toLocaleString("ko-KR")}`
                    : " · 미사용"}
                </div>
              </div>
              <div className="flex shrink-0 gap-2">
                <button
                  type="button"
                  onClick={() => rotateMutation.mutate(k.extId)}
                  disabled={rotateMutation.isPending}
                  className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
                >
                  회전
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (window.confirm(`키 "${k.name}"을(를) 폐기합니다.`)) {
                      revokeMutation.mutate(k.extId);
                    }
                  }}
                  disabled={revokeMutation.isPending}
                  className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
                >
                  폐기
                </button>
              </div>
            </li>
          ))}
        </ul>
      </main>
    </>
  );
}

interface BuilderProps {
  bearer: string;
}

/** Claude / ChatGPT / Codex용 MCP 연결 명령 스니펫. */
function McpConnectBuilder({ bearer }: BuilderProps): JSX.Element {
  const base = window.location.origin.replace(":5173", ":8080");
  const claudeJson = JSON.stringify(
    {
      mcpServers: {
        mneme: {
          transport: "sse",
          url: `${base}/sse`,
          headers: { Authorization: `Bearer ${bearer}` },
        },
      },
    },
    null,
    2,
  );
  const codexCli = `codex mcp add mneme --transport sse --url ${base}/sse --header "Authorization: Bearer ${bearer}"`;
  return (
    <div className="space-y-2 text-xs text-ink-200">
      <div className="font-medium text-ink-100">MCP 연결 명령</div>
      <details className="rounded border border-ink-700 bg-ink-900 p-2">
        <summary className="cursor-pointer">Claude Desktop / Claude.ai</summary>
        <pre className="mt-2 overflow-auto whitespace-pre-wrap break-words font-mono">{claudeJson}</pre>
      </details>
      <details className="rounded border border-ink-700 bg-ink-900 p-2">
        <summary className="cursor-pointer">Codex CLI</summary>
        <pre className="mt-2 overflow-auto whitespace-pre-wrap break-words font-mono">{codexCli}</pre>
      </details>
    </div>
  );
}
