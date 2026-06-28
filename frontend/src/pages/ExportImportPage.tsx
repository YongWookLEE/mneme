import { useState, type ChangeEvent, type JSX } from "react";
import { apiPost } from "../api/client";
import { getBearer } from "../lib/auth";
import Sidebar from "../components/Sidebar";

interface PreviewItem {
  index: number;
  filename: string;
  title: string;
  folderPath: string;
  tagNames: string[];
  byteSize: number;
  conflictExtId: string | null;
}

interface PreviewResult {
  sessionId: string;
  items: PreviewItem[];
}

interface ApplyResult {
  imported: number;
  skipped: number;
  errors: string[];
}

type Action = "skip" | "replace" | "create-new";

/**
 * 데이터 포터빌리티 페이지 — export 다운로드 + import 업로드/충돌 해결.
 *
 * @since phase 13
 */
export default function ExportImportPage(): JSX.Element {
  const [preview, setPreview] = useState<PreviewResult | null>(null);
  const [decisions, setDecisions] = useState<Record<number, Action>>({});
  const [applying, setApplying] = useState(false);
  const [applyResult, setApplyResult] = useState<ApplyResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function downloadExport(): Promise<void> {
    setError(null);
    const bearer = getBearer();
    const res = await fetch("/api/export", {
      headers: bearer ? { Authorization: `Bearer ${bearer}` } : {},
    });
    if (!res.ok) {
      setError(`Export 실패: HTTP ${res.status}`);
      return;
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `mneme-export-${new Date().toISOString().slice(0, 10)}.zip`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function onFile(e: ChangeEvent<HTMLInputElement>): Promise<void> {
    setError(null);
    setApplyResult(null);
    const file = e.target.files?.[0];
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    const bearer = getBearer();
    const res = await fetch("/api/import/preview", {
      method: "POST",
      headers: bearer ? { Authorization: `Bearer ${bearer}` } : {},
      body: form,
    });
    if (!res.ok) {
      setError(`Preview 실패: HTTP ${res.status}`);
      return;
    }
    const json = (await res.json()) as PreviewResult;
    setPreview(json);
    const initial: Record<number, Action> = {};
    for (const item of json.items) {
      initial[item.index] = item.conflictExtId ? "skip" : "create-new";
    }
    setDecisions(initial);
  }

  async function applyAll(): Promise<void> {
    if (!preview) return;
    setApplying(true);
    setError(null);
    try {
      const body = preview.items.map((it) => ({
        index: it.index,
        action: decisions[it.index] ?? "skip",
      }));
      const result = await apiPost<ApplyResult>(
        `/import/apply?sessionId=${encodeURIComponent(preview.sessionId)}`,
        body,
      );
      setApplyResult(result);
      setPreview(null);
    } catch (err) {
      setError(`Apply 실패: ${(err as Error).message}`);
    } finally {
      setApplying(false);
    }
  }

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">데이터 포터빌리티</h1>
        <p className="mb-6 text-xs text-ink-400">
          모든 메모리를 zip(manifest.json + 마크다운)으로 내보내거나 외부 zip을 가져옵니다.
        </p>

        <section className="mb-6 rounded border border-ink-700 bg-ink-800 p-4">
          <h2 className="mb-2 font-medium text-ink-100">Export</h2>
          <p className="mb-3 text-xs text-ink-400">
            본인 메모리 + 폴더 + 태그 전체. 외부 ID·생성/수정 시각·태그·archive 플래그 보존.
          </p>
          <button
            type="button"
            onClick={() => void downloadExport()}
            className="rounded bg-ink-100 px-3 py-1.5 text-sm font-medium text-ink-900 hover:bg-white"
          >
            zip 다운로드
          </button>
        </section>

        <section className="rounded border border-ink-700 bg-ink-800 p-4">
          <h2 className="mb-2 font-medium text-ink-100">Import</h2>
          <p className="mb-3 text-xs text-ink-400">
            Mneme zip 또는 일반 마크다운 zip. 같은 폴더의 동일 제목은 충돌로 표시되며, 항목별 건너뛰기/대체/새로 작성을 선택할 수 있습니다.
          </p>
          <input type="file" accept=".zip" onChange={onFile} className="mb-3 text-xs" />

          {error && <div className="mb-3 text-sm text-red-300">{error}</div>}

          {preview && (
            <div className="space-y-2">
              <div className="text-xs text-ink-400">
                총 {preview.items.length}건 · 충돌 {preview.items.filter((i) => i.conflictExtId).length}건
              </div>
              <ul className="max-h-96 space-y-1 overflow-auto rounded border border-ink-700 bg-ink-900 p-2">
                {preview.items.map((it) => (
                  <li key={it.index} className="flex items-center gap-2 text-xs">
                    <span className="flex-1 truncate" title={it.filename}>
                      <span className="text-ink-100">{it.title}</span>
                      <span className="ml-2 text-ink-400">{it.folderPath}</span>
                      <span className="ml-2 text-ink-500">{it.byteSize}B</span>
                      {it.conflictExtId && <span className="ml-2 text-yellow-300">충돌</span>}
                    </span>
                    <select
                      value={decisions[it.index] ?? "skip"}
                      onChange={(e) =>
                        setDecisions((d) => ({ ...d, [it.index]: e.target.value as Action }))
                      }
                      className="rounded border border-ink-600 bg-ink-800 px-2 py-0.5"
                    >
                      <option value="skip">건너뛰기</option>
                      <option value="replace">대체</option>
                      <option value="create-new">새로 작성</option>
                    </select>
                  </li>
                ))}
              </ul>
              <button
                type="button"
                disabled={applying}
                onClick={() => void applyAll()}
                className="rounded bg-ink-100 px-3 py-1.5 text-sm font-medium text-ink-900 hover:bg-white disabled:opacity-50"
              >
                {applying ? "처리 중…" : "적용"}
              </button>
            </div>
          )}

          {applyResult && (
            <div className="mt-3 rounded border border-ink-700 bg-ink-900 p-3 text-sm">
              <div>가져오기 {applyResult.imported}건 · 건너뜀 {applyResult.skipped}건</div>
              {applyResult.errors.length > 0 && (
                <ul className="mt-2 list-disc pl-5 text-xs text-red-300">
                  {applyResult.errors.map((e, i) => (
                    <li key={i}>{e}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </section>
      </main>
    </>
  );
}
