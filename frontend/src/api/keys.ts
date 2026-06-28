import { apiDelete, apiGet, apiPost } from "./client";

/** API 키 응답(평문 미포함). */
export interface KeyDto {
  extId: string;
  name: string;
  prefix: string;
  lastUsedAt: string | null;
  createdAt: string;
}

/** 발급 응답 — 평문 plaintext는 1회만 노출. */
export interface IssuedKeyDto extends KeyDto {
  plaintext: string;
}

/** 본인 활성 키 목록. */
export function fetchKeys(): Promise<KeyDto[]> {
  return apiGet<KeyDto[]>("/keys");
}

/** 새 키 발급. 응답의 plaintext는 즉시 안전한 곳에 옮기고 폐기해야 한다. */
export function issueKey(name: string): Promise<IssuedKeyDto> {
  return apiPost<IssuedKeyDto>("/keys", { name });
}

/** 폐기. */
export function revokeKey(extId: string): Promise<void> {
  return apiDelete<void>(`/keys/${extId}`);
}

/** 회전(같은 이름으로 폐기 + 새 키 발급). */
export function rotateKey(extId: string): Promise<IssuedKeyDto> {
  return apiPost<IssuedKeyDto>(`/keys/${extId}/rotate`, {});
}
