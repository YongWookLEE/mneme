/**
 * 클라이언트 인증 토큰 보관.
 *
 * localStorage `mneme.bearer`에 평문 Bearer 토큰(API 키 `mn_...` 또는 OAuth access)을 저장한다.
 * 토큰은 모든 API 호출에 `Authorization: Bearer ...` 헤더로 부착된다.
 * 운영 환경에서는 phase 03 세션 쿠키와 병행하지만 본 phase 11은 키 입력만 다룬다(phase 12에서 통합).
 *
 * @author Mneme
 * @since phase 11
 */
const STORAGE_KEY = "mneme.bearer";

/** 현재 저장된 Bearer 토큰. 없으면 null. */
export function getBearer(): string | null {
  return typeof window === "undefined" ? null : window.localStorage.getItem(STORAGE_KEY);
}

/** Bearer 토큰을 저장한다. 빈 문자열이면 삭제. */
export function setBearer(token: string): void {
  if (typeof window === "undefined") return;
  if (token.trim() === "") {
    window.localStorage.removeItem(STORAGE_KEY);
  } else {
    window.localStorage.setItem(STORAGE_KEY, token.trim());
  }
}

/** 인증 토큰을 제거(로그아웃 유사). */
export function clearBearer(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(STORAGE_KEY);
}
