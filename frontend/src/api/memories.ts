import { apiGet } from "./client";

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
