import { apiGet } from "./client";

export interface GraphNode {
  extId: string;
  title: string;
  byteSize: number;
}

export interface GraphEdge {
  sourceExtId: string;
  targetExtId: string;
  label: string;
}

export interface BrokenLink {
  sourceExtId: string;
  targetLabel: string;
}

export interface GraphResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
  broken: BrokenLink[];
}

export interface BacklinkDto {
  extId: string;
  title: string;
  summary: string | null;
}

export function fetchGraph(): Promise<GraphResponse> {
  return apiGet<GraphResponse>("/graph");
}

export function fetchBacklinks(memoryExtId: string): Promise<BacklinkDto[]> {
  return apiGet<BacklinkDto[]>(`/memories/${memoryExtId}/backlinks`);
}
