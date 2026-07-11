import { apiRequest } from './httpClient';
import { CategoryCode, PageResponse } from './keywords';

export type CandidateSource = 'KEYWORD' | 'CSV' | 'API';
export type CandidateGrade = 'RECOMMENDED' | 'REVIEW' | 'HOLD' | 'EXCLUDED';
export type CandidateStatus = 'ACTIVE' | 'SAVED' | 'EXCLUDED';
export type RiskLevel = 'LOW' | 'CAUTION' | 'EXCLUDED';

export interface CandidateScoreBreakdown {
  trendScore: number;
  competitionScore: number;
  marginScore: number;
  priceBandScore: number;
  priceScore: number;
  supplyScore: number;
  riskPenalty: number;
}

export interface CandidateListItem {
  candidateId: number;
  name: string;
  source: CandidateSource;
  categoryCode: CategoryCode;
  score: number;
  grade: CandidateGrade;
  expectedSalePrice: number;
  supplyPrice: number | null;
  shippingFee: number | null;
  expectedMarginRate: number;
  riskLevel: RiskLevel;
  status: CandidateStatus;
  createdAt: string;
}

export interface CandidateDetail extends CandidateListItem {
  scoreBreakdown: CandidateScoreBreakdown;
  reasons: string[];
  warnings: string[];
  keywordId: number | null;
  wholesaleProductId: number | null;
  updatedAt: string;
}

export interface CandidateFilters {
  grade?: CandidateGrade | '';
  categoryCode?: CategoryCode | '';
  minScore?: number | '';
  minMarginRate?: number | '';
  source?: CandidateSource | '';
  page?: number;
  size?: number;
}

export interface CandidateRecalculationResponse {
  recalculatedCount: number;
  skippedCount: number;
}

export const gradeLabels: Record<CandidateGrade, string> = {
  RECOMMENDED: '추천 검토',
  REVIEW: '검토',
  HOLD: '보류',
  EXCLUDED: '제외'
};

export const riskLabels: Record<RiskLevel, string> = {
  LOW: '낮음',
  CAUTION: '주의',
  EXCLUDED: '제외'
};

export const sourceLabels: Record<CandidateSource, string> = {
  KEYWORD: '키워드',
  CSV: 'CSV',
  API: 'API'
};

export const statusLabels: Record<CandidateStatus, string> = {
  ACTIVE: '활성',
  SAVED: '관심',
  EXCLUDED: '제외'
};

export function listCandidates(filters: CandidateFilters = {}) {
  const params = new URLSearchParams();
  params.set('page', String(filters.page ?? 0));
  params.set('size', String(filters.size ?? 20));
  if (filters.grade) {
    params.set('grade', filters.grade);
  }
  if (filters.categoryCode) {
    params.set('categoryCode', filters.categoryCode);
  }
  if (filters.minScore !== undefined && filters.minScore !== '') {
    params.set('minScore', String(filters.minScore));
  }
  if (filters.minMarginRate !== undefined && filters.minMarginRate !== '') {
    params.set('minMarginRate', String(filters.minMarginRate));
  }
  if (filters.source) {
    params.set('source', filters.source);
  }
  return apiRequest<PageResponse<CandidateListItem>>(`/candidates?${params.toString()}`);
}

export function getCandidate(candidateId: number) {
  return apiRequest<CandidateDetail>(`/candidates/${candidateId}`);
}

export function saveCandidate(candidateId: number) {
  return apiRequest<CandidateDetail>(`/candidates/${candidateId}/save`, {
    method: 'POST'
  });
}

export function excludeCandidate(candidateId: number) {
  return apiRequest<CandidateDetail>(`/candidates/${candidateId}/exclude`, {
    method: 'POST'
  });
}

export function recalculateCandidates() {
  return apiRequest<CandidateRecalculationResponse>('/candidates/recalculate', {
    method: 'POST'
  });
}
