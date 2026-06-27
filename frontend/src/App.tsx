/**
 * Mneme 루트 컴포넌트 (Phase 01 placeholder).
 *
 * 실제 페이지(Dashboard, Login, ApiKeys 등)는 phase 11에서 도입한다.
 * 지금은 골격이 살아있다는 신호만 보여준다.
 */
export default function App(): JSX.Element {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-semibold tracking-tight">Mneme</h1>
        <p className="mt-3 text-sm text-neutral-400">
          여러 AI 클라이언트가 공유하는 영구 기억층 — coming soon.
        </p>
      </div>
    </main>
  );
}
