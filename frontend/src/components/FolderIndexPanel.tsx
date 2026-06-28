import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type JSX } from "react";
import { ApiError } from "../api/client";
import { fetchFolderIndex, rebuildFolderIndex, type FolderIndexDto } from "../api/folderIndex";
import MarkdownView from "./MarkdownView";

interface Props {
  folderExtId: string;
}

/**
 * 폴더 페이지 상단에 노출하는 LLM 합성 인덱스.
 *
 * 인덱스가 없으면 "생성" 버튼만, 있으면 요약 + 본문(축약/전체 토글) + 재생성 버튼.
 *
 * @since phase 21
 */
export default function FolderIndexPanel({ folderExtId }: Props): JSX.Element {
  const queryClient = useQueryClient();
  const [expanded, setExpanded] = useState(false);

  const { data, isError, error } = useQuery({
    queryKey: ["folderIndex", folderExtId],
    queryFn: () => fetchFolderIndex(folderExtId),
    retry: false,
    refetchOnMount: false,
  });

  const rebuild = useMutation({
    mutationFn: () => rebuildFolderIndex(folderExtId),
    onSuccess: (fresh: FolderIndexDto) => {
      queryClient.setQueryData(["folderIndex", folderExtId], fresh);
    },
  });

  const notFound = isError && error instanceof ApiError && error.status === 404;
  const isEmpty = isError && error instanceof ApiError && error.status === 400;

  return (
    <section className="mb-4 rounded border border-ink-700 bg-ink-800 p-4">
      <header className="mb-2 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-medium uppercase tracking-wider text-ink-300">폴더 인덱스</h2>
          {data && (
            <div className="text-xs text-ink-400">
              {data.memoryCount}개 메모리 · {new Date(data.generatedAt).toLocaleString("ko-KR")}
            </div>
          )}
        </div>
        <button
          type="button"
          onClick={() => rebuild.mutate()}
          disabled={rebuild.isPending}
          className="shrink-0 rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700 disabled:opacity-50"
        >
          {rebuild.isPending ? "생성 중…" : data ? "재생성" : "생성"}
        </button>
      </header>

      {rebuild.isError && (
        <div className="text-xs text-red-300">
          오류: {(rebuild.error as Error).message}
          {rebuild.error instanceof ApiError && rebuild.error.status === 400 && " (폴더가 비어 있어 생성할 수 없습니다)"}
        </div>
      )}

      {isEmpty && !data && (
        <div className="text-xs text-ink-400">폴더가 비어 있습니다.</div>
      )}
      {notFound && !data && !rebuild.isPending && (
        <div className="text-xs text-ink-400">아직 인덱스가 없습니다. "생성" 버튼을 눌러 LLM이 요약하도록 하세요.</div>
      )}

      {data && (
        <div className="space-y-2">
          {!expanded && (
            <p className="text-sm text-ink-100">{data.summary}</p>
          )}
          {expanded && <MarkdownView source={data.body} />}
          <button
            type="button"
            onClick={() => setExpanded((v) => !v)}
            className="text-xs text-ink-300 underline hover:text-ink-100"
          >
            {expanded ? "축약" : "전체 보기"}
          </button>
        </div>
      )}
    </section>
  );
}
