import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import type { JSX } from "react";
import AuthGate from "./components/AuthGate";
import Shell from "./components/Shell";
import MemoryListPage from "./pages/MemoryListPage";
import OnboardingTour from "./components/OnboardingTour";
import ArchivePage from "./pages/ArchivePage";
import AuditPage from "./pages/AuditPage";
import ConnectGuidePage from "./pages/ConnectGuidePage";
import ExportImportPage from "./pages/ExportImportPage";
import KeysPage from "./pages/KeysPage";
import UsagePage from "./pages/UsagePage";
import MemoryDetailPage from "./pages/MemoryDetailPage";
import SearchPage from "./pages/SearchPage";

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
        <OnboardingTour />
        <BrowserRouter>
          <Routes>
            <Route element={<Shell />}>
              <Route index element={<MemoryListPage />} />
              <Route path="folder/:folderExtId" element={<MemoryListPage />} />
              <Route path="memory/:extId" element={<MemoryDetailPage />} />
              <Route path="search" element={<SearchPage />} />
              <Route path="archive" element={<ArchivePage />} />
              <Route path="keys" element={<KeysPage />} />
              <Route path="connect" element={<ConnectGuidePage />} />
              <Route path="data" element={<ExportImportPage />} />
              <Route path="audit" element={<AuditPage />} />
              <Route path="usage" element={<UsagePage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </AuthGate>
  );
}
