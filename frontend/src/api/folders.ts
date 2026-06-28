import { apiGet } from "./client";

/** 폴더 응답 DTO. 백엔드 FolderResponse와 일치. */
export interface FolderDto {
  extId: string;
  parentExtId: string | null;
  name: string;
  path: string;
}

/**
 * 사용자 본인의 모든 폴더 조회.
 *
 * @returns 폴더 배열(루트 직속부터 깊이 순)
 * @since phase 11
 */
export function fetchFolders(): Promise<FolderDto[]> {
  return apiGet<FolderDto[]>("/folders");
}
