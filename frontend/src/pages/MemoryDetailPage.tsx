import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import type { JSX } from "react";
import { fetchMemory } from "../api/memories";
import Sidebar from "../components/Sidebar";

/**
 * 메모리 단건 상세(읽기 전용 phase 11 step 1).
 *
 * 본문 마크다운 렌더링과 편집은 step 2에서 도입한다. 현재는 raw 본문을 monospace로 그대로 출력.
 *
 * @since phase 11
 */
export default function MemoryDetailPage(): JSX.Element {
  const { extId } = useParams<{ extId: string }>();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["memory", extId],
    queryFn: () => fetchMemory(extId!),
    enabled: Boolean(extId),
  });

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && (
          <article className="space-y-4">
            <header>
              <h1 className="text-xl font-medium tracking-tight">{data.title}</h1>
              <div className="mt-1 text-xs text-ink-400">
                {new Date(data.updatedAt).toLocaleString("ko-KR")} · v{data.version} · {data.byteSize}B
              </div>
              {data.summary && (
                <p className="mt-3 rounded border border-ink-700 bg-ink-800 p-3 text-sm text-ink-200">
                  {data.summary}
                </p>
              )}
            </header>
            <pre className="whitespace-pre-wrap break-words rounded border border-ink-700 bg-ink-800 p-4 font-mono text-sm text-ink-100">
              {data.content}
            </pre>
          </article>
        )}
      </main>
    </>
  );
}
