import "@testing-library/jest-dom/vitest";

/**
 * jsdom localStorage가 일부 환경에서 메서드 없이 노출되는 케이스 대응.
 *
 * AuthGate가 마운트 시 getItem을 호출하므로 누락된 경우 Map 기반 stub으로 채운다.
 */
if (typeof window !== "undefined" && (typeof window.localStorage === "undefined" || typeof window.localStorage.getItem !== "function")) {
  const store = new Map<string, string>();
  Object.defineProperty(window, "localStorage", {
    configurable: true,
    value: {
      getItem: (k: string) => store.get(k) ?? null,
      setItem: (k: string, v: string) => {
        store.set(k, v);
      },
      removeItem: (k: string) => {
        store.delete(k);
      },
      clear: () => {
        store.clear();
      },
      key: (i: number) => Array.from(store.keys())[i] ?? null,
      get length() {
        return store.size;
      },
    } as Storage,
  });
}
