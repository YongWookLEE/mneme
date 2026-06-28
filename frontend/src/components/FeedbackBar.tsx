import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type JSX } from "react";
import { fetchFeedback, submitFeedback, type FeedbackDto, type FeedbackTarget, type FeedbackValue } from "../api/feedback";

interface Props {
  memoryExtId: string;
}

const TARGETS: { value: FeedbackTarget; label: string }[] = [
  { value: "summary", label: "요약" },
  { value: "folder", label: "분류" },
  { value: "tags", label: "태그" },
  { value: "index", label: "인덱스" },
  { value: "general", label: "전반" },
];

/**
 * 메모리 상세에 들어가는 작은 피드백 바.
 *
 * 사용자가 LLM의 요약/분류/태그/인덱스에 대해 👍/👎와 짧은 메모를 남기면 이후 LLM 호출의
 * 시스템 프롬프트에 자동 반영된다(`FeedbackHintBuilder`).
 *
 * @since phase 23
 */
export default function FeedbackBar({ memoryExtId }: Props): JSX.Element {
  const queryClient = useQueryClient();
  const [target, setTarget] = useState<FeedbackTarget>("summary");
  const [note, setNote] = useState("");

  const { data } = useQuery({
    queryKey: ["feedback", memoryExtId],
    queryFn: () => fetchFeedback(memoryExtId),
  });

  const submit = useMutation({
    mutationFn: (value: FeedbackValue) => submitFeedback(memoryExtId, target, value, note || undefined),
    onSuccess: () => {
      setNote("");
      queryClient.invalidateQueries({ queryKey: ["feedback", memoryExtId] });
    },
  });

  return (
    <section className="mt-4 rounded border border-ink-700 bg-ink-800 p-3">
      <div className="mb-2 text-xs uppercase tracking-wider text-ink-400">피드백</div>
      <div className="flex flex-wrap items-center gap-2">
        <select
          value={target}
          onChange={(e) => setTarget(e.target.value as FeedbackTarget)}
          className="rounded border border-ink-600 bg-ink-900 px-2 py-1 text-xs"
        >
          {TARGETS.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
        <input
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="짧은 이유 (선택)"
          maxLength={500}
          className="flex-1 rounded border border-ink-600 bg-ink-900 px-2 py-1 text-xs outline-none focus:border-ink-400"
        />
        <button
          type="button"
          onClick={() => submit.mutate("up")}
          disabled={submit.isPending}
          className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
        >
          👍
        </button>
        <button
          type="button"
          onClick={() => submit.mutate("down")}
          disabled={submit.isPending}
          className="rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
        >
          👎
        </button>
      </div>
      {data && data.length > 0 && (
        <ul className="mt-3 space-y-0.5 text-xs text-ink-300">
          {data.slice(0, 5).map((f: FeedbackDto, i: number) => (
            <li key={i} className="truncate">
              <span className="mr-1">{f.value === "up" ? "👍" : "👎"}</span>
              <span className="text-ink-400">{f.target}</span>
              {f.note && <span className="ml-1">: {f.note}</span>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
