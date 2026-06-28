import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type JSX } from "react";
import { useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import {
  archiveMemory,
  fetchMemory,
  updateMemory,
  type MemoryDto,
} from "../api/memories";
import MarkdownView from "../components/MarkdownView";
import Sidebar from "../components/Sidebar";

/**
 * 메모리 단건 페이지.
 *
 * 기본은 마크다운 뷰. 편집 버튼을 누르면 textarea로 본문/제목 수정.
 * 저장은 PATCH `/api/memories/{extId}`에 version 동봉(낙관적 락). 409 충돌 시
 * 서버 본문과 사용자 본문을 좌우로 보여주고 사용자가 직접 병합한다.
 *
 * @since phase 11 step 2
 */
export default function MemoryDetailPage(): JSX.Element {
  const { extId } = useParams<{ extId: string }>();
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["memory", extId],
    queryFn: () => fetchMemory(extId!),
    enabled: Boolean(extId),
  });

  const [editing, setEditing] = useState(false);
  const [draftTitle, setDraftTitle] = useState("");
  const [draftContent, setDraftContent] = useState("");
  const [conflict, setConflict] = useState<MemoryDto | null>(null);

  useEffect(() => {
    if (data && !editing) {
      setDraftTitle(data.title);
      setDraftContent(data.content);
    }
  }, [data, editing]);

  const saveMutation = useMutation({
    mutationFn: async (mem: MemoryDto) =>
      updateMemory(mem.extId, {
        version: mem.version,
        title: draftTitle,
        content: draftContent,
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData(["memory", extId], updated);
      queryClient.invalidateQueries({ queryKey: ["memories"] });
      setEditing(false);
      setConflict(null);
    },
    onError: async (err: unknown) => {
      if (err instanceof ApiError && err.status === 409 && data) {
        try {
          const latest = await fetchMemory(data.extId);
          setConflict(latest);
          queryClient.setQueryData(["memory", extId], latest);
        } catch {
          // 무시 - 일반 에러 채널로 노출
        }
      }
    },
  });

  const archiveMutation = useMutation({
    mutationFn: async (mem: MemoryDto) => archiveMemory(mem.extId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["memory", extId] });
      queryClient.invalidateQueries({ queryKey: ["memories"] });
    },
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
            <header className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                {editing ? (
                  <input
                    value={draftTitle}
                    onChange={(e) => setDraftTitle(e.target.value)}
                    className="w-full rounded border border-ink-600 bg-ink-800 px-3 py-2 text-lg font-medium tracking-tight outline-none focus:border-ink-400"
                    placeholder="제목"
                  />
                ) : (
                  <h1 className="truncate text-xl font-medium tracking-tight">{data.title}</h1>
                )}
                <div className="mt-1 text-xs text-ink-400">
                  {new Date(data.updatedAt).toLocaleString("ko-KR")} · v{data.version} · {data.byteSize}B
                  {data.archivedAt ? " · 보관됨" : ""}
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                {editing ? (
                  <>
                    <button
                      type="button"
                      onClick={() => saveMutation.mutate(data)}
                      disabled={saveMutation.isPending}
                      className="rounded bg-ink-100 px-3 py-1.5 text-sm font-medium text-ink-900 hover:bg-white disabled:opacity-50"
                    >
                      {saveMutation.isPending ? "저장 중…" : "저장"}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setEditing(false);
                        setConflict(null);
                        setDraftTitle(data.title);
                        setDraftContent(data.content);
                      }}
                      className="rounded border border-ink-600 px-3 py-1.5 text-sm hover:bg-ink-700"
                    >
                      취소
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={() => setEditing(true)}
                      className="rounded border border-ink-600 px-3 py-1.5 text-sm hover:bg-ink-700"
                    >
                      편집
                    </button>
                    {!data.archivedAt && (
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm("이 메모리를 보관(soft delete)합니다.")) {
                            archiveMutation.mutate(data);
                          }
                        }}
                        className="rounded border border-ink-600 px-3 py-1.5 text-sm hover:bg-ink-700"
                      >
                        보관
                      </button>
                    )}
                  </>
                )}
              </div>
            </header>

            {data.summary && (
              <p className="rounded border border-ink-700 bg-ink-800 p-3 text-sm text-ink-200">
                {data.summary}
              </p>
            )}

            {conflict && (
              <ConflictPanel
                serverContent={conflict.content}
                myContent={draftContent}
                serverVersion={conflict.version}
                onKeepMine={() => {
                  setDraftContent(draftContent);
                  setConflict(null);
                }}
                onTakeServer={() => {
                  setDraftContent(conflict.content);
                  setConflict(null);
                }}
                onMergeManually={() => setConflict(null)}
              />
            )}

            {editing ? (
              <textarea
                value={draftContent}
                onChange={(e) => setDraftContent(e.target.value)}
                rows={24}
                className="w-full rounded border border-ink-700 bg-ink-800 p-4 font-mono text-sm text-ink-100 outline-none focus:border-ink-500"
              />
            ) : (
              <MarkdownView source={data.content} />
            )}

            {saveMutation.isError && !conflict && (
              <div className="rounded border border-red-700 bg-red-900/30 p-3 text-sm text-red-200">
                저장 실패: {(saveMutation.error as Error).message}
              </div>
            )}
          </article>
        )}
      </main>
    </>
  );
}

interface ConflictProps {
  serverContent: string;
  myContent: string;
  serverVersion: number;
  onKeepMine: () => void;
  onTakeServer: () => void;
  onMergeManually: () => void;
}

/** 409 충돌 패널 — 두 본문을 좌우로 보여 사용자가 직접 선택/병합. */
function ConflictPanel(props: ConflictProps): JSX.Element {
  return (
    <div className="space-y-3 rounded border border-yellow-700 bg-yellow-900/20 p-3 text-sm">
      <div className="flex items-center justify-between">
        <div className="font-medium text-yellow-200">
          충돌: 다른 곳에서 v{props.serverVersion}로 갱신됨
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={props.onTakeServer}
            className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
          >
            서버 본문 사용
          </button>
          <button
            type="button"
            onClick={props.onKeepMine}
            className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
          >
            내 본문 유지
          </button>
          <button
            type="button"
            onClick={props.onMergeManually}
            className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
          >
            수동 병합
          </button>
        </div>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        <div>
          <div className="mb-1 text-xs text-ink-400">서버 본문 (최신)</div>
          <pre className="max-h-48 overflow-auto whitespace-pre-wrap break-words rounded border border-ink-700 bg-ink-900 p-2 font-mono text-xs">
            {props.serverContent}
          </pre>
        </div>
        <div>
          <div className="mb-1 text-xs text-ink-400">내 본문 (편집 중)</div>
          <pre className="max-h-48 overflow-auto whitespace-pre-wrap break-words rounded border border-ink-700 bg-ink-900 p-2 font-mono text-xs">
            {props.myContent}
          </pre>
        </div>
      </div>
    </div>
  );
}
