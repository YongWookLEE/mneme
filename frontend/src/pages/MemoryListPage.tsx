import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import type { JSX } from "react";
import { fetchMemories, type MemoryDto } from "../api/memories";
import Sidebar from "../components/Sidebar";

/**
 * 메모리 리스트 페이지.
 *
 * 좌 사이드(폴더) + 중 리스트. URL `/folder/:folderExtId`이면 해당 폴더의 메모리만, 아니면 전체 활성 메모리.
 *
 * @since phase 11
 */
export default function MemoryListPage(): JSX.Element {
  const { folderExtId } = useParams<{ folderExtId?: string }>();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["memories"],
    queryFn: fetchMemories,
  });

  const filtered = data?.filter((m: MemoryDto) =>
    folderExtId ? m.folderExtId === folderExtId : true,
  );

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-4 text-lg font-medium tracking-tight">
          {folderExtId ? "폴더 메모리" : "모든 메모리"}
        </h1>
        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {filtered && filtered.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            메모리가 없습니다. MCP `mn_write` 또는 REST `POST /api/memories`로 작성하세요.
          </div>
        )}
        <ul className="space-y-2">
          {filtered?.map((m: MemoryDto) => (
            <li
              key={m.extId}
              className="rounded border border-ink-700 bg-ink-800 p-4 hover:border-ink-500"
            >
              <Link to={`/memory/${m.extId}`} className="block">
                <div className="mb-1 truncate text-base font-medium text-ink-100">{m.title}</div>
                {m.summary && (
                  <div className="line-clamp-2 text-sm text-ink-300">{m.summary}</div>
                )}
                <div className="mt-2 flex gap-3 text-xs text-ink-400">
                  <span>{new Date(m.updatedAt).toLocaleString("ko-KR")}</span>
                  <span>{m.byteSize}B</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </main>
    </>
  );
}
