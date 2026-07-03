import { apiRequest } from './httpClient';
import { CategoryCode, PageResponse } from './keywords';

export type AlertFrequency = 'DAILY_SUMMARY' | 'WEEKLY_SUMMARY';
export type AlertStatus = 'UNREAD' | 'READ';
export type AlertType = 'CANDIDATE_SCORE';

export interface AlertItem {
  id: number;
  type: AlertType;
  status: AlertStatus;
  title: string;
  message: string;
  candidateId: number;
  candidateName: string;
  ruleId: number;
  ruleName: string;
  createdAt: string;
  readAt: string | null;
}

export interface AlertRulePayload {
  name: string;
  minScore: number;
  minMarginRate: number;
  categoryCodes: CategoryCode[];
  riskExcluded: boolean;
  frequency: AlertFrequency;
}

export interface AlertRule extends AlertRulePayload {
  id: number;
  active: boolean;
  createdAt: string;
}

export function listAlerts(page = 0, size = 20) {
  return apiRequest<PageResponse<AlertItem>>(`/alerts?page=${page}&size=${size}`);
}

export function createAlertRule(payload: AlertRulePayload) {
  return apiRequest<AlertRule>('/alert-rules', {
    method: 'POST',
    body: payload
  });
}

export function markAlertRead(alertId: number) {
  return apiRequest<AlertItem>(`/alerts/${alertId}/read`, {
    method: 'PATCH'
  });
}
