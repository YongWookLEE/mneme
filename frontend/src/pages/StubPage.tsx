import type { JSX } from "react";
import Sidebar from "../components/Sidebar";

interface Props {
  title: string;
  hint: string;
}

/**
 * 미구현 페이지 자리 표시(아카이브/키 등 phase 11 step 3에서 채움).
 *
 * @since phase 11
 */
export default function StubPage({ title, hint }: Props): JSX.Element {
  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <h1 className="mb-2 text-lg font-medium tracking-tight">{title}</h1>
        <p className="text-sm text-ink-300">{hint}</p>
      </main>
    </>
  );
}
