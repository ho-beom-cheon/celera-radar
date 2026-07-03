export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

const accessTokenKey = 'seller-radar.access-token';
const userPlanKey = 'seller-radar.user-plan';

export interface ApiErrorBody {
  code: string;
  message: string;
  field?: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: ApiErrorBody | null;
  meta: {
    requestId?: string;
    generatedAt?: string;
    cached?: boolean;
  };
}

export class ApiRequestError extends Error {
  status: number;
  code?: string;
  field?: string;

  constructor(status: number, message: string, code?: string, field?: string) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    this.field = field;
  }
}

export function getAccessToken() {
  return window.localStorage.getItem(accessTokenKey);
}

export function setAccessToken(accessToken: string) {
  window.localStorage.setItem(accessTokenKey, accessToken);
}

export function clearAccessToken() {
  window.localStorage.removeItem(accessTokenKey);
}

export function getStoredPlan() {
  return window.localStorage.getItem(userPlanKey) ?? 'FREE';
}

export function setStoredPlan(plan: string) {
  window.localStorage.setItem(userPlanKey, plan);
}

interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  accessToken?: string | null;
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}) {
  const headers: Record<string, string> = {
    Accept: 'application/json'
  };
  const token = options.accessToken ?? getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const init: RequestInit = {
    method: options.method ?? 'GET',
    headers
  };
  if (options.body instanceof FormData) {
    init.body = options.body;
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(options.body);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, init);
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;

  if (!response.ok || !payload?.success) {
    const error = payload?.error;
    throw new ApiRequestError(
      response.status,
      error?.message ?? '요청을 처리하지 못했습니다.',
      error?.code,
      error?.field
    );
  }

  return payload.data;
}
