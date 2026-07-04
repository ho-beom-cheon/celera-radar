import { ApiRequestError } from '../api/httpClient';

const defaultApiErrorMessage = '요청을 처리하지 못했습니다.';

export function formatApiError(error: unknown, fallbackMessage = defaultApiErrorMessage) {
  if (error instanceof ApiRequestError) {
    const message = error.message || fallbackMessage;
    return error.field ? `${message} (${error.field})` : message;
  }
  return fallbackMessage;
}

export function authRequiredMessage(action: string) {
  return `계정 연결 후 ${action}할 수 있습니다.`;
}
