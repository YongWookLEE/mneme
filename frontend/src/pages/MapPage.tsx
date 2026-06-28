import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useEffect, useMemo, useRef, useState, type JSX } from "react";
import ForceGraph2D from "react-force-graph-2d";
import { fetchGraph } from "../api/graph";
import Sidebar from "../components/Sidebar";

/**
 * `/map` — 본인 메모리 관계 그래프. force-directed 레이아웃. 노드 클릭 시 상세 페이지로 이동.
 *
 * 깨진 링크(target 없음)는 별도 패널에 표시한다.
 *
 * @since phase 17
 */
export default function MapPage(): JSX.Element {
  const navigate = useNavigate();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["graph"],
    queryFn: fetchGraph,
  });

  const graphRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ w: 600, h: 400 });

  useEffect(() => {
    if (!graphRef.current) return;
    const el = graphRef.current;
    const update = (): void => setSize({ w: el.clientWidth, h: el.clientHeight });
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const graphData = useMemo(() => {
    if (!data) return { nodes: [], links: [] };
    return {
      nodes: data.nodes.map((n) => ({ id: n.extId, name: n.title, val: Math.max(1, n.byteSize / 200) })),
      links: data.edges.map((e) => ({ source: e.sourceExtId, target: e.targetExtId, label: e.label })),
    };
  }, [data]);

  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-hidden p-4">
        <div className="mb-2 flex items-center justify-between">
          <h1 className="text-lg font-medium tracking-tight">메모리 맵</h1>
          {data && (
            <div className="text-xs text-ink-400">
              노드 {data.nodes.length} · 엣지 {data.edges.length}
              {data.broken.length > 0 ? ` · 깨진 ${data.broken.length}` : ""}
            </div>
          )}
        </div>
        {isLoading && <div className="text-sm text-ink-300">불러오는 중…</div>}
        {isError && (
          <div className="text-sm text-red-300">오류: {(error as Error).message}</div>
        )}
        {data && data.nodes.length === 0 && (
          <div className="rounded border border-ink-700 bg-ink-800 p-6 text-sm text-ink-300">
            메모리가 없어 그래프를 그릴 수 없습니다.
          </div>
        )}
        {data && data.nodes.length > 0 && (
          <div className="flex h-[calc(100vh-9rem)] gap-3">
            <div ref={graphRef} className="flex-1 overflow-hidden rounded border border-ink-700 bg-ink-900">
              <ForceGraph2D
                width={size.w}
                height={size.h}
                graphData={graphData}
                nodeLabel="name"
                backgroundColor="#0a0a0b"
                nodeColor={() => "#d4d4d7"}
                linkColor={() => "#3a3a40"}
                linkDirectionalParticles={1}
                linkDirectionalParticleColor={() => "#7e7e87"}
                onNodeClick={(node) => {
                  navigate(`/memory/${(node as { id: string }).id}`);
                }}
              />
            </div>
            {data.broken.length > 0 && (
              <aside className="w-72 shrink-0 overflow-auto rounded border border-ink-700 bg-ink-800 p-3">
                <div className="mb-2 text-xs uppercase tracking-wider text-yellow-300">
                  깨진 링크 {data.broken.length}
                </div>
                <ul className="space-y-1 text-xs">
                  {data.broken.map((b, i) => (
                    <li key={i} className="rounded border border-ink-700 bg-ink-900 p-2">
                      <div className="text-ink-200">[[{b.targetLabel}]]</div>
                      <div className="text-ink-500">from {b.sourceExtId}</div>
                    </li>
                  ))}
                </ul>
              </aside>
            )}
          </div>
        )}
      </main>
    </>
  );
}
