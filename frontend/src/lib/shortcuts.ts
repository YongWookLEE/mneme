import { useEffect } from "react";

/**
 * 키보드 단축키 훅.
 *
 * 입력 필드(input/textarea/contenteditable) 포커스 중에는 동작하지 않는다.
 * 수정자 키는 Mac은 metaKey(⌘), 그 외는 ctrlKey로 추상화한다.
 *
 * @since phase 11 step 4
 */
export function useShortcut(
  combo: string,
  handler: (e: KeyboardEvent) => void,
  enabled = true,
): void {
  useEffect(() => {
    if (!enabled) return;
    const parts = combo.toLowerCase().split("+").map((s) => s.trim());
    const wantsMod = parts.includes("mod");
    const wantsShift = parts.includes("shift");
    const key = parts[parts.length - 1];

    function onKey(e: KeyboardEvent): void {
      const target = e.target as HTMLElement | null;
      if (target) {
        const tag = target.tagName.toLowerCase();
        if (tag === "input" || tag === "textarea" || target.isContentEditable) {
          // 검색바 포커스용 Mod+K만 예외
          if (!(wantsMod && key === "k")) return;
        }
      }
      const modPressed = e.metaKey || e.ctrlKey;
      if (wantsMod !== modPressed) return;
      if (wantsShift !== e.shiftKey) return;
      if (e.key.toLowerCase() !== key) return;
      e.preventDefault();
      handler(e);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [combo, handler, enabled]);
}
