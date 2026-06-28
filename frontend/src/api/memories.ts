import { apiGet, apiPatch, apiPost } from "./client";

/** 메모리 응답 DTO. 백엔드 MemoryResponse와 일치. */
export interface MemoryDto {
  extId: string;
  folderExtId: string;
  title: string;
  content: string;
  summary: string | null;
  byteSize: number;
  createdAt: string;
  updatedAt: string;
  version: number;
  archivedAt?: string | null;
}

/**
 * 사용자 본인의 활성 메모리 목록(archived 제외).
 *
 * @since phase 11
 */
export function fetchMemories(): Promise<MemoryDto[]> {
  return apiGet<MemoryDto[]>("/memories");
}

/** 단건 메모리 본문 포함 조회. */
export function fetchMemory(extId: string): Promise<MemoryDto> {
  return apiGet<MemoryDto>(`/memories/${extId}`);
}

/** 메모리 갱신 요청. 낙관적 락을 위해 `version`은 필수. */
export interface UpdateMemoryRequest {
  version: number;
  title?: string;
  content?: string;
  summary?: string;
  folderExtId?: string;
}

/** PATCH /api/memories/{extId} — 낙관적 락 충돌 시 409. */
export function updateMemory(extId: string, body: UpdateMemoryRequest): Promise<MemoryDto> {
  return apiPatch<MemoryDto>(`/memories/${extId}`, body);
}

/** POST /api/memories/{extId}/archive — soft delete. */
export function archiveMemory(extId: string): Promise<void> {
  return apiPost<void>(`/memories/${extId}/archive`, {});
}

/** POST /api/memories/{extId}/restore — 복구. */
export function restoreMemory(extId: string): Promise<void> {
  return apiPost<void>(`/memories/${extId}/restore`, {});
}
