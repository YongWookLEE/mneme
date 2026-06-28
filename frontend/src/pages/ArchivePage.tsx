import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import type { JSX } from "react";
import { apiGet } from "../api/client";
import { restoreMemory, type MemoryDto } from "../api/memories";
import Sidebar from "../components/Sidebar";

/**
 * 보관된 메모리 페이지. `/api/memories?archived=true`.
 *
 * 활성 동일 제목과 충돌하면 복구 실패 → 409.
 *
 * @since phase 11 step 3
 */
export default function ArchivePage(): JSX.Element {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["memories", "archived"],
    queryFn: () => apiGet<MemoryDto[]>("/memories?archived=true"),
  });

  const restoreMutation = useMutation({
    mutationFn: async (extId: string) => restoreMemory(extId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["memories", "archived"] });
      queryClient.invalidateQueries({ queryKey: ["memories"] });
    },
  });

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">아카이브</h1>
        <p className="mb-4 text-xs text-ink-400">
          보관된(soft delete) 메모리. 영구 삭제는 지원하지 않음. 동일 활성 제목과 충돌 시 복구 실패.
        </p>
        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && data.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            보관된 메모리가 없습니다.
          </div>
        )}
        <ul className="space-y-2">
          {data?.map((m: MemoryDto) => (
            <li
              key={m.extId}
              className="flex items-start justify-between gap-3 rounded border border-ink-700 bg-ink-800 p-4"
            >
              <Link to={`/memory/${m.extId}`} className="min-w-0 flex-1">
                <div className="mb-1 truncate text-base font-medium text-ink-100">{m.title}</div>
                {m.summary && (
                  <div className="line-clamp-2 text-sm text-ink-300">{m.summary}</div>
                )}
                <div className="mt-2 text-xs text-ink-400">
                  보관: {m.archivedAt ? new Date(m.archivedAt).toLocaleString("ko-KR") : "-"}
                </div>
              </Link>
              <button
                type="button"
                onClick={() => restoreMutation.mutate(m.extId)}
                disabled={restoreMutation.isPending}
                className="shrink-0 rounded border border-ink-600 px-3 py-1.5 text-sm hover:bg-ink-700"
              >
                복구
              </button>
            </li>
          ))}
        </ul>
      </main>
    </>
  );
}
