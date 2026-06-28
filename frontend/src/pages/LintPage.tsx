import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import type { JSX } from "react";
import { fetchLint, type LintIssue } from "../api/lint";
import Sidebar from "../components/Sidebar";

const KIND_LABEL: Record<string, string> = {
  broken: "깨진 링크",
  orphan: "외톨이",
  stub: "미완성",
  "dup-title": "제목 중복",
};

/**
 * `/lint` — 본인 메모리 lint 결과 검토. 카테고리별 카운트 + 이슈 리스트(메모리로 점프).
 *
 * @since phase 22
 */
export default function LintPage(): JSX.Element {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["lint"],
    queryFn: fetchLint,
  });

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">검토 (lint)</h1>
        <p className="mb-4 text-xs text-ink-400">
          깨진 링크 · 외톨이 · 미완성 · 제목 중복을 감지합니다. 항목을 누르면 해당 메모리로 이동합니다.
        </p>

        {isLoading && <div className="text-sm text-ink-300">분석 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && (
          <>
            <div className="mb-4 grid grid-cols-4 gap-2 text-sm">
              {Object.entries(data.counts).map(([kind, count]) => (
                <div key={kind} className="rounded border border-ink-700 bg-ink-800 p-3">
                  <div className="text-xs text-ink-400">{KIND_LABEL[kind] ?? kind}</div>
                  <div className="text-lg font-medium text-ink-100">{count}</div>
                </div>
              ))}
            </div>
            {data.issues.length === 0 && (
              <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
                감지된 이슈가 없습니다. 👍
              </div>
            )}
            <ul className="space-y-1">
              {data.issues.map((it: LintIssue, i: number) => (
                <li key={i} className="rounded border border-ink-700 bg-ink-800 p-3 text-sm hover:border-ink-500">
                  <Link to={`/memory/${it.memoryExtId}`} className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <div className="truncate font-medium text-ink-100">{it.memoryTitle}</div>
                      <div className="text-xs text-ink-400">{it.detail}</div>
                    </div>
                    <span className="shrink-0 rounded border border-ink-600 px-2 py-0.5 text-xs text-ink-300">
                      {KIND_LABEL[it.kind] ?? it.kind}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </>
        )}
      </main>
    </>
  );
}
