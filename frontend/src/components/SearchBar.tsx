import { useRef, useState, type FormEvent, type JSX } from "react";
import { useNavigate } from "react-router-dom";
import { useShortcut } from "../lib/shortcuts";

/**
 * 상단 검색바. 엔터 시 `/search?q=...`로 이동.
 *
 * @since phase 11 step 3
 */
export default function SearchBar(): JSX.Element {
  const [q, setQ] = useState("");
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  useShortcut("mod+k", () => inputRef.current?.focus());
  return (
    <form
      onSubmit={(e: FormEvent) => {
        e.preventDefault();
        if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`);
      }}
      className="flex-1 max-w-md"
    >
      <input
        ref={inputRef}
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="검색… (⌘/Ctrl+K)"
        className="w-full rounded border border-ink-600 bg-ink-800 px-3 py-1.5 text-sm outline-none placeholder:text-ink-400 focus:border-ink-400"
      />
    </form>
  );
}
