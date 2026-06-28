import { useState, type JSX, type ReactNode } from "react";
import { getBearer, setBearer } from "../lib/auth";

interface Props {
  children: ReactNode;
}

/**
 * Bearer 토큰이 없으면 입력 모달, 있으면 자식 컴포넌트 렌더.
 *
 * 입력 받은 토큰은 localStorage `mneme.bearer`에 저장된다.
 * 운영에서는 phase 03 세션 쿠키 로그인과 병행 예정(phase 12 통합 가이드 페이지).
 *
 * @since phase 11
 */
export default function AuthGate({ children }: Props): JSX.Element {
  const [bearer, setLocal] = useState<string | null>(() => getBearer());
  const [draft, setDraft] = useState("");

  if (bearer) return <>{children}</>;

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink-900 p-6 text-ink-100">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (draft.trim()) {
            setBearer(draft);
            setLocal(draft.trim());
          }
        }}
        className="w-full max-w-md space-y-4 rounded border border-ink-600 bg-ink-800 p-6"
      >
        <h1 className="text-xl font-medium tracking-tight">Mneme 대시보드</h1>
        <p className="text-sm text-ink-300">
          접속하려면 API 키(<code className="text-ink-100">mn_…</code>) 또는 MCP access token을 입력하세요.
        </p>
        <input
          type="password"
          autoComplete="off"
          autoFocus
          placeholder="mn_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          className="w-full rounded border border-ink-600 bg-ink-900 px-3 py-2 font-mono text-sm outline-none focus:border-ink-400"
        />
        <button
          type="submit"
          className="w-full rounded bg-ink-100 px-3 py-2 text-sm font-medium text-ink-900 transition hover:bg-white disabled:opacity-50"
          disabled={!draft.trim()}
        >
          입장
        </button>
      </form>
    </div>
  );
}
