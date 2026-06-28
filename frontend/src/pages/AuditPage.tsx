import { useQuery } from "@tanstack/react-query";
import type { JSX } from "react";
import { fetchAudit, type AuditEventDto } from "../api/observability";
import Sidebar from "../components/Sidebar";

/**
 * 본인 감사 이벤트 목록.
 *
 * @since phase 14
 */
export default function AuditPage(): JSX.Element {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["audit"],
    queryFn: fetchAudit,
  });

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">감사 이벤트</h1>
        <p className="mb-4 text-xs text-ink-400">키 발급/폐기, 메모리 archive 등 본인 계정의 보안 관련 활동.</p>
        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && data.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            기록된 이벤트가 없습니다.
          </div>
        )}
        <ul className="space-y-1">
          {data?.map((e: AuditEventDto, i: number) => (
            <li key={i} className="flex items-center justify-between gap-3 rounded border border-ink-700 bg-ink-800 px-3 py-2 text-sm">
              <div className="min-w-0">
                <div className="font-medium text-ink-100">{e.action}</div>
                <div className="text-xs text-ink-400">
                  {e.actorKind}
                  {e.targetKind ? ` · ${e.targetKind}` : ""}
                  {e.targetId ? ` · ${e.targetId}` : ""}
                </div>
              </div>
              <div className="shrink-0 text-xs text-ink-400">{new Date(e.createdAt).toLocaleString("ko-KR")}</div>
            </li>
          ))}
        </ul>
      </main>
    </>
  );
}
