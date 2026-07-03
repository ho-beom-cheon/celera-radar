import { apiRequest } from './httpClient';

export type Plan = 'FREE' | 'BASIC' | 'PRO';

export interface AuthResponse {
  userId: number;
  email: string;
  plan: Plan;
  accessToken: string;
  refreshToken: string;
}

export function signup(email: string, password: string, termsAgreed: boolean) {
  return apiRequest<AuthResponse>('/auth/signup', {
    method: 'POST',
    body: { email, password, termsAgreed },
    accessToken: null
  });
}

export function login(email: string, password: string) {
  return apiRequest<AuthResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
    accessToken: null
  });
}
