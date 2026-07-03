import { apiRequest } from './httpClient';

export type CategoryCode =
  | 'CAR_ACCESSORY'
  | 'DESK_OFFICE'
  | 'HOME_STORAGE'
  | 'BATH_CLEANING'
  | 'TRAVEL_ORGANIZER'
  | 'PET_WALK_HYGIENE'
  | 'KITCHEN_STORAGE'
  | 'CAMPING_PICNIC'
  | 'HOME_TRAINING'
  | 'SEASONAL_LIVING';

export type KeywordPriority = 'HIGH' | 'MEDIUM' | 'LOW';
export type AnalysisStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED';

export interface KeywordItem {
  id: number;
  keyword: string;
  categoryCode: CategoryCode;
  priority: KeywordPriority;
  analysisStatus: AnalysisStatus;
  lastAnalyzedAt: string | null;
  createdAt: string;
}

export interface KeywordAnalysis {
  keywordId: number;
  keyword: string;
  status: AnalysisStatus;
  lastAnalyzedAt: string | null;
  shopping: ShoppingAnalysis | null;
  trend: unknown | null;
  score: unknown | null;
}

export interface ShoppingAnalysis {
  baseDate: string;
  totalResults: number;
  minPrice: number | null;
  maxPrice: number | null;
  avgPrice: number | null;
  topItems: ShoppingTopItem[];
}

export interface ShoppingTopItem {
  itemRank: number;
  title: string;
  link: string | null;
  image: string | null;
  lprice: number | null;
  hprice: number | null;
  mallName: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  category4: string | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface KeywordFilters {
  category?: CategoryCode | '';
  status?: AnalysisStatus | '';
  page?: number;
  size?: number;
}

export interface KeywordPayload {
  keyword: string;
  categoryCode: CategoryCode;
  priority?: KeywordPriority;
}

export const categoryOptions: Array<{ value: CategoryCode; label: string }> = [
  { value: 'CAR_ACCESSORY', label: '차량용품' },
  { value: 'DESK_OFFICE', label: '데스크/오피스' },
  { value: 'HOME_STORAGE', label: '홈수납' },
  { value: 'BATH_CLEANING', label: '욕실/청소' },
  { value: 'TRAVEL_ORGANIZER', label: '여행정리' },
  { value: 'PET_WALK_HYGIENE', label: '반려동물 산책/위생' },
  { value: 'KITCHEN_STORAGE', label: '주방수납' },
  { value: 'CAMPING_PICNIC', label: '캠핑/피크닉' },
  { value: 'HOME_TRAINING', label: '홈트레이닝' },
  { value: 'SEASONAL_LIVING', label: '시즌생활' }
];

export const priorityLabels: Record<KeywordPriority, string> = {
  HIGH: '높음',
  MEDIUM: '보통',
  LOW: '낮음'
};

export const statusLabels: Record<AnalysisStatus, string> = {
  PENDING: '대기',
  RUNNING: '분석 중',
  SUCCESS: '완료',
  FAILED: '실패',
  SKIPPED: '건너뜀'
};

export function listKeywords(filters: KeywordFilters = {}) {
  const params = new URLSearchParams();
  params.set('page', String(filters.page ?? 0));
  params.set('size', String(filters.size ?? 20));
  if (filters.category) {
    params.set('category', filters.category);
  }
  if (filters.status) {
    params.set('status', filters.status);
  }
  return apiRequest<PageResponse<KeywordItem>>(`/keywords?${params.toString()}`);
}

export function createKeyword(payload: KeywordPayload) {
  return apiRequest<KeywordItem>('/keywords', {
    method: 'POST',
    body: payload
  });
}

export function deleteKeyword(keywordId: number) {
  return apiRequest<null>(`/keywords/${keywordId}`, {
    method: 'DELETE'
  });
}

export function getKeywordAnalysis(keywordId: number) {
  return apiRequest<KeywordAnalysis>(`/keywords/${keywordId}/analysis`);
}
