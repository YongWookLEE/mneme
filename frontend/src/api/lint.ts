import { apiGet } from "./client";

export interface LintIssue {
  kind: string;
  memoryExtId: string;
  memoryTitle: string;
  detail: string;
}

export interface LintReport {
  counts: Record<string, number>;
  issues: LintIssue[];
}

export function fetchLint(): Promise<LintReport> {
  return apiGet<LintReport>("/lint");
}
