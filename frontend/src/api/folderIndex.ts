import { apiGet, apiPost } from "./client";

/** 폴더 인덱스 응답. */
export interface FolderIndexDto {
  folderExtId: string;
  summary: string;
  body: string;
  memoryCount: number;
  generatedAt: string;
}

/** 현재 인덱스 조회. 없으면 ApiError(404). */
export function fetchFolderIndex(extId: string): Promise<FolderIndexDto> {
  return apiGet<FolderIndexDto>(`/folders/${extId}/index`);
}

/** LLM 호출로 인덱스를 새로 생성한다. 폴더가 비었으면 400. */
export function rebuildFolderIndex(extId: string): Promise<FolderIndexDto> {
  return apiPost<FolderIndexDto>(`/folders/${extId}/index/rebuild`, {});
}
