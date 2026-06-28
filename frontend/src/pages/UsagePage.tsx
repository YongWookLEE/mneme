import { useQuery } from "@tanstack/react-query";
import type { JSX } from "react";
import { fetchUsage, type UsageDailyDto } from "../api/observability";
import Sidebar from "../components/Sidebar";

/**
 * 본인 일별 사용량(최근 30일).
 *
 * @since phase 14
 */
export default function UsagePage(): JSX.Element {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["usage"],
    queryFn: fetchUsage,
  });

  const totals = data?.reduce(
    (acc, r) => ({
      embed: acc.embed + r.embedTokens,
      llmIn: acc.llmIn + r.llmInTokens,
      llmOut: acc.llmOut + r.llmOutTokens,
      requests: acc.requests + r.requestCount,
    }),
    { embed: 0, llmIn: 0, llmOut: 0, requests: 0 },
  );

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-1 text-lg font-medium tracking-tight">사용량</h1>
        <p className="mb-4 text-xs text-ink-400">최근 30일 일별 토큰 + 요청 수.</p>
        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {totals && (
          <div className="mb-4 grid grid-cols-4 gap-2 text-sm">
            <Card label="요청" value={totals.requests} />
            <Card label="임베딩 토큰" value={totals.embed} />
            <Card label="LLM 입력 토큰" value={totals.llmIn} />
            <Card label="LLM 출력 토큰" value={totals.llmOut} />
          </div>
        )}
        {data && data.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            기록된 사용량이 없습니다.
          </div>
        )}
        {data && data.length > 0 && (
          <table className="w-full table-auto text-sm">
            <thead className="text-left text-xs uppercase text-ink-400">
              <tr>
                <th className="py-2">날짜</th>
                <th>임베딩</th>
                <th>LLM in</th>
                <th>LLM out</th>
                <th>요청</th>
              </tr>
            </thead>
            <tbody>
              {data.map((r: UsageDailyDto) => (
                <tr key={r.date} className="border-t border-ink-700">
                  <td className="py-1.5">{r.date}</td>
                  <td>{r.embedTokens}</td>
                  <td>{r.llmInTokens}</td>
                  <td>{r.llmOutTokens}</td>
                  <td>{r.requestCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}

interface CardProps {
  label: string;
  value: number;
}

function Card({ label, value }: CardProps): JSX.Element {
  return (
    <div className="rounded border border-ink-700 bg-ink-800 p-3">
      <div className="text-xs text-ink-400">{label}</div>
      <div className="text-lg font-medium text-ink-100">{value.toLocaleString()}</div>
    </div>
  );
}
