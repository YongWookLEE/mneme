import { apiGet } from "./client";

/** 검색 hit DTO. SearchController.SearchResponse와 동일. */
export interface SearchHitDto {
  extId: string;
  folderExtId: string;
  title: string;
  summary: string | null;
  score: number;
  createdAt: string;
  updatedAt: string;
}

/** 하이브리드 검색 요청 옵션. */
export interface SearchOptions {
  q: string;
  folderExtId?: string | null;
  tags?: string[];
  limit?: number;
}

/** GET /api/search. q가 비어 있으면 빈 배열 반환(요청 차단). */
export async function searchMemories(opts: SearchOptions): Promise<SearchHitDto[]> {
  const params = new URLSearchParams({ q: opts.q });
  if (opts.folderExtId) params.set("folderExtId", opts.folderExtId);
  if (opts.tags && opts.tags.length > 0) params.set("tags", opts.tags.join(","));
  if (opts.limit) params.set("limit", String(opts.limit));
  return apiGet<SearchHitDto[]>(`/search?${params.toString()}`);
}
