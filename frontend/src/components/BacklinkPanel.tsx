import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import type { JSX } from "react";
import { fetchBacklinks, type BacklinkDto } from "../api/graph";

interface Props {
  memoryExtId: string;
}

/**
 * 메모리 상세 페이지 우측 — 이 메모리를 가리키는 다른 메모리 목록.
 *
 * @since phase 17
 */
export default function BacklinkPanel({ memoryExtId }: Props): JSX.Element {
  const { data, isLoading } = useQuery({
    queryKey: ["backlinks", memoryExtId],
    queryFn: () => fetchBacklinks(memoryExtId),
  });

  return (
    <aside className="mt-6 rounded border border-ink-700 bg-ink-800 p-4">
      <div className="mb-2 text-xs uppercase tracking-wider text-ink-400">Backlinks</div>
      {isLoading && <div className="text-xs text-ink-300">불러오는 중…</div>}
      {data && data.length === 0 && (
        <div className="text-xs text-ink-300">이 메모리를 참조하는 다른 메모리가 없습니다.</div>
      )}
      <ul className="space-y-1 text-sm">
        {data?.map((b: BacklinkDto) => (
          <li key={b.extId}>
            <Link to={`/memory/${b.extId}`} className="block rounded px-2 py-1 hover:bg-ink-700">
              <div className="truncate text-ink-100">{b.title}</div>
              {b.summary && (
                <div className="line-clamp-1 text-xs text-ink-400">{b.summary}</div>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </aside>
  );
}
