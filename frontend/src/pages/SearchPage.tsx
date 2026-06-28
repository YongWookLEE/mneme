import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import type { JSX } from "react";
import Sidebar from "../components/Sidebar";
import { searchMemories, type SearchHitDto } from "../api/search";

/**
 * 검색 결과 페이지. `?q=` 쿼리스트링을 읽어 백엔드 `/api/search` 호출.
 *
 * @since phase 11 step 3
 */
export default function SearchPage(): JSX.Element {
  const [params] = useSearchParams();
  const q = params.get("q") ?? "";
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["search", q],
    queryFn: () => searchMemories({ q }),
    enabled: q.length > 0,
  });

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">검색: {q}</h1>
        <p className="mb-4 text-xs text-ink-400">
          벡터 + 전문검색 + 트라이그램 결합. 점수는 가중 합.
        </p>
        {q.length === 0 && (
          <div className="text-sm text-ink-300">상단 검색바에 질의를 입력하세요.</div>
        )}
        {isLoading && <div className="text-sm text-ink-300">검색 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && data.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            결과 없음.
          </div>
        )}
        <ul className="space-y-2">
          {data?.map((hit: SearchHitDto) => (
            <li
              key={hit.extId}
              className="rounded border border-ink-700 bg-ink-800 p-4 hover:border-ink-500"
            >
              <Link to={`/memory/${hit.extId}`} className="block">
                <div className="flex items-start justify-between gap-3">
                  <div className="mb-1 truncate text-base font-medium text-ink-100">{hit.title}</div>
                  <span className="shrink-0 text-xs text-ink-400">{hit.score.toFixed(3)}</span>
                </div>
                {hit.summary && (
                  <div className="line-clamp-2 text-sm text-ink-300">{hit.summary}</div>
                )}
              </Link>
            </li>
          ))}
        </ul>
      </main>
    </>
  );
}
