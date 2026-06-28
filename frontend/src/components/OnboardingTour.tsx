import { useState, type JSX } from "react";
import { Link } from "react-router-dom";

const STORAGE_KEY = "mneme.onboarded";

interface Step {
  title: string;
  body: JSX.Element;
}

/**
 * 첫 로그인 4단계 투어. localStorage `mneme.onboarded` 플래그로 한 번만 노출.
 *
 * @since phase 12
 */
export default function OnboardingTour(): JSX.Element | null {
  const [open, setOpen] = useState(() => {
    if (typeof window === "undefined") return false;
    return window.localStorage.getItem(STORAGE_KEY) !== "1";
  });
  const [idx, setIdx] = useState(0);

  if (!open) return null;

  const steps: Step[] = [
    {
      title: "환영합니다",
      body: (
        <p>
          Mneme는 여러 AI 클라이언트가 공유하는 영구 메모리 저장소입니다. 폴더 트리에 메모리를
          정리하고, 의미 기반 검색으로 다시 찾고, MCP로 어디서든 같은 기억을 호출하세요. 자세한
          사용법은 상단의 <Link to="/help" className="underline">도움말</Link>을 참고하세요.
        </p>
      ),
    },
    {
      title: "1. API 키 발급",
      body: (
        <p>
          <Link to="/keys" className="underline">키 페이지</Link>에서 새 키를 만드세요. 평문 키는
          발급 직후 1회만 표시됩니다.
        </p>
      ),
    },
    {
      title: "2. MCP 클라이언트 연결",
      body: (
        <p>
          <Link to="/connect" className="underline">연결 가이드</Link>에서 Claude Desktop · ChatGPT
          Developer · Codex CLI용 설정을 복사해 붙여 넣으세요.
        </p>
      ),
    },
    {
      title: "3. 메모리 작성",
      body: (
        <p>
          MCP로 <code className="rounded bg-ink-700 px-1">mn_write</code>를 호출하거나, 대시보드 검색바에서
          기억을 빠르게 찾을 수 있습니다. <code className="rounded bg-ink-700 px-1">[[메모리 제목]]</code>으로
          본문 안에 링크를 걸면 자동으로 backlink 인덱스가 갱신됩니다.
        </p>
      ),
    },
  ];

  const step = steps[idx];
  const last = idx === steps.length - 1;
  function dismiss(): void {
    if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, "1");
    setOpen(false);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-6">
      <div className="w-full max-w-md space-y-4 rounded border border-ink-600 bg-ink-800 p-6 text-ink-100">
        <div className="text-xs text-ink-400">
          {idx + 1} / {steps.length}
        </div>
        <h2 className="text-lg font-medium tracking-tight">{step.title}</h2>
        <div className="text-sm leading-relaxed text-ink-200">{step.body}</div>
        <div className="flex justify-between gap-2 pt-2">
          <button
            type="button"
            onClick={dismiss}
            className="rounded border border-ink-600 px-3 py-1.5 text-xs hover:bg-ink-700"
          >
            건너뛰기
          </button>
          <div className="flex gap-2">
            {idx > 0 && (
              <button
                type="button"
                onClick={() => setIdx((i) => i - 1)}
                className="rounded border border-ink-600 px-3 py-1.5 text-sm hover:bg-ink-700"
              >
                이전
              </button>
            )}
            <button
              type="button"
              onClick={() => (last ? dismiss() : setIdx((i) => i + 1))}
              className="rounded bg-ink-100 px-3 py-1.5 text-sm font-medium text-ink-900 hover:bg-white"
            >
              {last ? "시작" : "다음"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
