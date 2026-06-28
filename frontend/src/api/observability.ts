import { apiGet } from "./client";

/** 감사 이벤트 1건. */
export interface AuditEventDto {
  actorKind: string;
  action: string;
  targetKind: string | null;
  targetId: string | null;
  createdAt: string;
}

/** 일별 사용량 1행. */
export interface UsageDailyDto {
  date: string;
  embedTokens: number;
  llmInTokens: number;
  llmOutTokens: number;
  requestCount: number;
}

export function fetchAudit(): Promise<AuditEventDto[]> {
  return apiGet<AuditEventDto[]>("/audit");
}

export function fetchUsage(): Promise<UsageDailyDto[]> {
  return apiGet<UsageDailyDto[]>("/usage");
}
