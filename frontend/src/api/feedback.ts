import { apiGet, apiPost } from "./client";

export type FeedbackTarget = "folder" | "summary" | "tags" | "index" | "general";
export type FeedbackValue = "up" | "down";

export interface FeedbackDto {
  memoryExtId: string;
  target: FeedbackTarget;
  value: FeedbackValue;
  note: string | null;
  createdAt: string;
}

export function submitFeedback(
  memoryExtId: string,
  target: FeedbackTarget,
  value: FeedbackValue,
  note?: string,
): Promise<FeedbackDto> {
  return apiPost<FeedbackDto>(`/memories/${memoryExtId}/feedback`, { target, value, note });
}

export function fetchFeedback(memoryExtId: string): Promise<FeedbackDto[]> {
  return apiGet<FeedbackDto[]>(`/memories/${memoryExtId}/feedback`);
}
