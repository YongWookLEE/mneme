import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import type { JSX } from "react";
import AuthGate from "./components/AuthGate";
import Shell from "./components/Shell";
import MemoryListPage from "./pages/MemoryListPage";
import MemoryDetailPage from "./pages/MemoryDetailPage";
import StubPage from "./pages/StubPage";

/**
 * Mneme 대시보드 루트.
 *
 * AuthGate(Bearer 입력) → QueryClientProvider → Router → Shell + 라우트.
 *
 * @since phase 11
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, refetchOnWindowFocus: false, retry: 1 },
  },
});

export default function App(): JSX.Element {
  return (
    <AuthGate>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route element={<Shell />}>
              <Route index element={<MemoryListPage />} />
              <Route path="folder/:folderExtId" element={<MemoryListPage />} />
              <Route path="memory/:extId" element={<MemoryDetailPage />} />
              <Route
                path="archive"
                element={<StubPage title="아카이브" hint="archive된 메모리 + 복구는 step 3에서 구현." />}
              />
              <Route
                path="keys"
                element={
                  <StubPage
                    title="API 키 + MCP 명령 빌더"
                    hint="키 발급/폐기/회전 + 클라이언트별 connect 명령 복사 위젯은 step 3에서."
                  />
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </AuthGate>
  );
}
