import type { JSX } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface Props {
  source: string;
}

/**
 * 메모리 본문 마크다운 렌더링.
 *
 * GFM(테이블·작업 목록·strikethrough) 지원. `[[wiki-link]]`는 phase 16 파서에서 별도 처리.
 * 톤은 모노크롬 그레이만 사용.
 *
 * @since phase 11 step 2
 */
export default function MarkdownView({ source }: Props): JSX.Element {
  return (
    <div className="prose prose-invert max-w-none text-ink-100 prose-headings:tracking-tight prose-a:text-ink-100 prose-a:underline prose-code:text-ink-100 prose-code:bg-ink-700 prose-code:px-1 prose-code:rounded prose-pre:bg-ink-800 prose-pre:border prose-pre:border-ink-700 prose-blockquote:border-ink-600 prose-blockquote:text-ink-200">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{source}</ReactMarkdown>
    </div>
  );
}
