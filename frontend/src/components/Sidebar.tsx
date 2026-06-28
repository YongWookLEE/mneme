import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import type { JSX } from "react";
import { fetchFolders, type FolderDto } from "../api/folders";

/**
 * 좌측 폴더 트리(읽기 전용 phase 11 step 1).
 *
 * 폴더 경로(`/projects/mneme/`)를 파싱해 트리로 시각화한다. 클릭 시 우측 메모리 리스트 필터링은
 * step 3 검색·필터에서 라우터 query parameter로 통합.
 *
 * @since phase 11
 */
export default function Sidebar(): JSX.Element {
  const { folderExtId } = useParams<{ folderExtId?: string }>();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["folders"],
    queryFn: fetchFolders,
  });

  return (
    <aside className="hidden w-60 shrink-0 border-r border-ink-700 bg-ink-800 p-3 md:flex md:flex-col">
      <div className="mb-2 text-xs uppercase tracking-wider text-ink-400">폴더</div>
      {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
      {isError && (
        <div className="text-sm text-red-300">
          오류: {(error as Error).message}
        </div>
      )}
      {data && (
        <ul className="space-y-0.5 overflow-auto text-sm">
          <li>
            <Link
              to="/"
              className={`block rounded px-2 py-1 hover:bg-ink-700 ${
                folderExtId == null ? "bg-ink-700 text-ink-100" : "text-ink-200"
              }`}
            >
              전체
            </Link>
          </li>
          {data.map((f: FolderDto) => (
            <li key={f.extId}>
              <Link
                to={`/folder/${f.extId}`}
                className={`block truncate rounded px-2 py-1 hover:bg-ink-700 ${
                  folderExtId === f.extId ? "bg-ink-700 text-ink-100" : "text-ink-200"
                }`}
                title={f.path}
              >
                {f.name}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
