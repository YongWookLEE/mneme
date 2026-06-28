import { clearBearer, getBearer } from "../lib/auth";

/**
 * Mneme 백엔드 호출용 fetch wrapper.
 *
 * - `Authorization: Bearer ...` 자동 부착(localStorage `mneme.bearer`)
 * - JSON 직렬화/역직렬화
 * - 401은 토큰 삭제 후 호출자에게 ApiError 전파
 *
 * @author Mneme
 * @since phase 11
 */

const BASE = "/api";

/** API 호출 실패. status + ProblemDetail 본문이 있으면 detail에 보존. */
export class ApiError extends Error {
  constructor(public readonly status: number, public readonly detail?: unknown) {
    super(`API ${status}`);
  }
}

/** 내부 fetch 헬퍼. */
async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  const bearer = getBearer();
  if (bearer) headers.Authorization = `Bearer ${bearer}`;
  let payload: string | undefined;
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const res = await fetch(`${BASE}${path}`, { method, headers, body: payload });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text.length > 0 ? (JSON.parse(text) as unknown) : undefined;
  if (!res.ok) {
    if (res.status === 401) clearBearer();
    throw new ApiError(res.status, data);
  }
  return data as T;
}

/** GET. */
export function apiGet<T>(path: string): Promise<T> {
  return request<T>("GET", path);
}

/** POST. */
export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>("POST", path, body);
}

/** PATCH. */
export function apiPatch<T>(path: string, body: unknown): Promise<T> {
  return request<T>("PATCH", path, body);
}

/** DELETE. */
export function apiDelete<T>(path: string): Promise<T> {
  return request<T>("DELETE", path);
}
