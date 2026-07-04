import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  AlertFrequency,
  AlertItem,
  AlertRulePayload,
  createAlertRule,
  listAlerts,
  markAlertRead
} from '../../api/alerts';
import { getAccessToken } from '../../api/httpClient';
import { CategoryCode } from '../../api/keywords';
import {
  DataTable,
  DataTableStateRow,
  EmptyState,
  ErrorState,
  FieldMessage,
  HelpTooltip,
  LoadingState,
  StatusBadge
} from '../../components/ui';
import { authRequiredMessage, formatApiError } from '../../lib/apiError';

const categoryOptions: Array<{ value: CategoryCode; label: string }> = [
  { value: 'CAR_ACCESSORY', label: '차량용품' },
  { value: 'DESK_OFFICE', label: '데스크/오피스' },
  { value: 'HOME_STORAGE', label: '자취/수납' },
  { value: 'BATH_CLEANING', label: '욕실/청소' },
  { value: 'TRAVEL_ORGANIZER', label: '여행정리' },
  { value: 'PET_WALK_HYGIENE', label: '반려동물' },
  { value: 'KITCHEN_STORAGE', label: '주방수납' },
  { value: 'CAMPING_PICNIC', label: '캠핑/피크닉' },
  { value: 'HOME_TRAINING', label: '홈트레이닝' },
  { value: 'SEASONAL_LIVING', label: '시즌생활' }
];

interface AlertsPageProps {
  mode: 'list' | 'rules';
}

interface AlertRuleFormErrors {
  name?: string;
  minScore?: string;
  minMarginRate?: string;
  categoryCodes?: string;
}

export function AlertsPage({ mode }: AlertsPageProps) {
  const hasAccessToken = Boolean(getAccessToken());
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [ruleName, setRuleName] = useState('추천점수 80 이상');
  const [minScore, setMinScore] = useState('80');
  const [minMarginRate, setMinMarginRate] = useState('25');
  const [selectedCategories, setSelectedCategories] = useState<CategoryCode[]>(['CAR_ACCESSORY']);
  const [riskExcluded, setRiskExcluded] = useState(true);
  const [frequency, setFrequency] = useState<AlertFrequency>('DAILY_SUMMARY');
  const [ruleSubmitting, setRuleSubmitting] = useState(false);
  const [ruleFormErrors, setRuleFormErrors] = useState<AlertRuleFormErrors>({});
  const alertRuleValues = {
    name: ruleName,
    minScore,
    minMarginRate,
    categoryCodes: selectedCategories
  };
  const liveRuleErrors = validateAlertRuleForm(alertRuleValues);
  const ruleNameFieldError = ruleFormErrors.name ?? liveRuleErrors.name;
  const minScoreFieldError = ruleFormErrors.minScore ?? liveRuleErrors.minScore;
  const minMarginRateFieldError = ruleFormErrors.minMarginRate ?? liveRuleErrors.minMarginRate;
  const categoryCodesFieldError = ruleFormErrors.categoryCodes ?? liveRuleErrors.categoryCodes;
  const hasVisibleRuleErrors = Boolean(
    ruleNameFieldError || minScoreFieldError || minMarginRateFieldError || categoryCodesFieldError
  );
  const ruleSubmitReason = getRuleSubmitReason(hasAccessToken, ruleSubmitting, alertRuleValues);

  const loadAlerts = useCallback(async () => {
    if (!getAccessToken()) {
      setAlerts([]);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await listAlerts();
      setAlerts(response.items);
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (mode === 'list') {
      void loadAlerts();
    }
  }, [loadAlerts, mode]);

  async function handleRead(alertId: number) {
    setError('');
    setMessage('');
    try {
      await markAlertRead(alertId);
      setMessage('알림을 읽음 처리했습니다.');
      await loadAlerts();
    } catch (requestError) {
      setError(formatApiError(requestError));
    }
  }

  async function handleRuleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validateAlertRuleForm({
      name: ruleName,
      minScore,
      minMarginRate,
      categoryCodes: selectedCategories
    });
    setRuleFormErrors(nextErrors);
    if (!hasAccessToken || ruleSubmitting || hasFormErrors(nextErrors)) {
      return;
    }
    const payload: AlertRulePayload = {
      name: ruleName.trim(),
      minScore: Number(minScore),
      minMarginRate: Number(minMarginRate),
      categoryCodes: selectedCategories,
      riskExcluded,
      frequency
    };
    setError('');
    setMessage('');
    setRuleSubmitting(true);
    try {
      await createAlertRule(payload);
      setRuleFormErrors({});
      setMessage('알림 조건을 저장했습니다. 다음 알림 생성 배치부터 적용됩니다.');
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setRuleSubmitting(false);
    }
  }

  function toggleCategory(categoryCode: CategoryCode) {
    setRuleFormErrors((current) => ({
      ...current,
      categoryCodes: undefined
    }));
    setSelectedCategories((current) =>
      current.includes(categoryCode)
        ? current.filter((value) => value !== categoryCode)
        : [...current, categoryCode]
    );
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row">
        <div className="toolbar-title">
          <p className="eyebrow">Alerts</p>
          <h1>{mode === 'list' ? '알림 목록' : '알림 조건 설정'}</h1>
          <p className="muted">
            후보 점수와 예상 마진율 기준으로 검토할 알림을 관리합니다.
          </p>
        </div>
        <div className="button-row">
          <a className="secondary-button" href="/alerts">
            알림 목록
          </a>
          <a className="primary-button" href="/alert-rules">
            조건 설정
          </a>
        </div>
      </section>

      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <ErrorState>{error}</ErrorState> : null}

      {mode === 'list' ? (
        <section className="panel keywords-table-panel">
          <div className="panel-header">
            <div>
              <h2>최근 알림</h2>
              <p className="muted">조건에 맞는 데이터 기반 후보가 생성되면 표시됩니다.</p>
            </div>
            <button type="button" className="ghost-button" onClick={() => void loadAlerts()}>
              새로고침
            </button>
          </div>
          <DataTable>
              <thead>
                <tr>
                  <th>상태</th>
                  <th>알림</th>
                  <th>후보</th>
                  <th>조건</th>
                  <th>생성일</th>
                  <th>액션</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert) => (
                  <tr key={alert.id}>
                    <td>
                      <StatusBadge className={alert.status === 'READ' ? 'status-analyzed' : 'status-pending'}>
                        {alert.status === 'READ' ? '읽음' : '미확인'}
                      </StatusBadge>
                    </td>
                    <td>
                      <strong>{alert.title}</strong>
                      <p className="muted">{alert.message}</p>
                    </td>
                    <td>
                      <a className="detail-link" href={`/candidates/${alert.candidateId}`}>
                        {alert.candidateName}
                      </a>
                    </td>
                    <td>{alert.ruleName}</td>
                    <td>{formatDateTime(alert.createdAt)}</td>
                    <td>
                      <button
                        type="button"
                        className="ghost-button"
                        disabled={alert.status === 'READ'}
                        onClick={() => void handleRead(alert.id)}
                      >
                        읽음
                      </button>
                    </td>
                  </tr>
                ))}
                {loading ? (
                  <DataTableStateRow colSpan={6}>
                    <LoadingState>알림을 불러오는 중입니다.</LoadingState>
                  </DataTableStateRow>
                ) : null}
                {!loading && !hasAccessToken ? (
                  <DataTableStateRow colSpan={6}>
                    <EmptyState>{authRequiredMessage('알림을 확인')}</EmptyState>
                  </DataTableStateRow>
                ) : null}
                {!loading && hasAccessToken && alerts.length === 0 ? (
                  <DataTableStateRow colSpan={6}>
                    <EmptyState>아직 확인할 알림이 없습니다.</EmptyState>
                  </DataTableStateRow>
                ) : null}
              </tbody>
          </DataTable>
        </section>
      ) : (
        <form className="panel keyword-form-panel alert-rule-form" onSubmit={handleRuleSubmit}>
          <div className="panel-header">
            <h2>
              <span className="table-heading-help">
                <span>새 알림 조건</span>
                <HelpTooltip contentKey="alertRule" compact />
              </span>
            </h2>
            <p className="muted">점수, 마진율, 카테고리 조건이 모두 맞는 후보에 알림을 생성합니다.</p>
          </div>
          <div className="form-grid form-grid-two">
            <label className="field">
              <span>조건명</span>
              <input
                value={ruleName}
                onChange={(event) => {
                  setRuleName(event.target.value);
                  setRuleFormErrors((current) => ({ ...current, name: undefined }));
                }}
                aria-invalid={Boolean(ruleNameFieldError)}
                aria-describedby="alert-rule-name-message"
              />
              <FieldMessage
                id="alert-rule-name-message"
                tone={ruleNameFieldError ? 'error' : 'hint'}
              >
                {ruleNameFieldError ?? '알림 목록에 표시할 조건명입니다.'}
              </FieldMessage>
            </label>
            <label className="field">
              <span>생성 주기</span>
              <select
                value={frequency}
                onChange={(event) => setFrequency(event.target.value as AlertFrequency)}
              >
                <option value="DAILY_SUMMARY">매일 요약</option>
                <option value="WEEKLY_SUMMARY">주간 요약</option>
              </select>
            </label>
            <label className="field">
              <span>최소 점수</span>
              <input
                type="number"
                min={0}
                max={100}
                value={minScore}
                onChange={(event) => {
                  setMinScore(event.target.value);
                  setRuleFormErrors((current) => ({ ...current, minScore: undefined }));
                }}
                aria-invalid={Boolean(minScoreFieldError)}
                aria-describedby="alert-rule-min-score-message"
              />
              <FieldMessage
                id="alert-rule-min-score-message"
                tone={minScoreFieldError ? 'error' : 'hint'}
              >
                {minScoreFieldError ?? '0~100 사이로 입력하세요.'}
              </FieldMessage>
            </label>
            <label className="field">
              <span>최소 예상 마진율</span>
              <input
                type="number"
                min={0}
                step="0.1"
                value={minMarginRate}
                onChange={(event) => {
                  setMinMarginRate(event.target.value);
                  setRuleFormErrors((current) => ({ ...current, minMarginRate: undefined }));
                }}
                aria-invalid={Boolean(minMarginRateFieldError)}
                aria-describedby="alert-rule-min-margin-message"
              />
              <FieldMessage
                id="alert-rule-min-margin-message"
                tone={minMarginRateFieldError ? 'error' : 'hint'}
              >
                {minMarginRateFieldError ?? '0 이상 숫자로 입력하세요.'}
              </FieldMessage>
            </label>
          </div>
          <fieldset
            className={`category-checks ${categoryCodesFieldError ? 'category-checks-error' : ''}`}
            aria-describedby="alert-rule-category-message"
          >
            <legend>카테고리</legend>
            {categoryOptions.map((option) => (
              <label key={option.value} className="checkbox-field">
                <input
                  type="checkbox"
                  checked={selectedCategories.includes(option.value)}
                  onChange={() => toggleCategory(option.value)}
                />
                <span>{option.label}</span>
              </label>
            ))}
            <FieldMessage
              id="alert-rule-category-message"
              tone={categoryCodesFieldError ? 'error' : 'hint'}
            >
              {categoryCodesFieldError ?? '하나 이상 선택하세요.'}
            </FieldMessage>
          </fieldset>
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={riskExcluded}
              onChange={(event) => setRiskExcluded(event.target.checked)}
            />
            <span>위험 제외 후보는 알림에서 제외</span>
          </label>
          <div className="button-row">
            <button type="submit" className="primary-button" disabled={Boolean(ruleSubmitReason)}>
              {ruleSubmitting ? '저장 중' : '조건 저장'}
            </button>
            {ruleSubmitReason ? (
              <p className={`form-action-hint ${hasVisibleRuleErrors ? 'form-action-hint-error' : ''}`}>
                {ruleSubmitReason}
              </p>
            ) : null}
          </div>
        </form>
      )}
    </div>
  );
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short'
  }).format(new Date(value));
}

interface AlertRuleFormValues {
  name: string;
  minScore: string;
  minMarginRate: string;
  categoryCodes: CategoryCode[];
}

function validateAlertRuleForm(values: AlertRuleFormValues): AlertRuleFormErrors {
  const errors: AlertRuleFormErrors = {};
  const minScore = Number(values.minScore);
  const minMarginRate = Number(values.minMarginRate);

  if (!values.name.trim()) {
    errors.name = '조건명을 입력하세요.';
  }
  if (!values.minScore.trim()) {
    errors.minScore = '최소 점수를 입력하세요.';
  } else if (!Number.isFinite(minScore) || minScore < 0 || minScore > 100) {
    errors.minScore = '최소 점수는 0~100 사이여야 합니다.';
  }
  if (!values.minMarginRate.trim()) {
    errors.minMarginRate = '최소 예상 마진율을 입력하세요.';
  } else if (!Number.isFinite(minMarginRate) || minMarginRate < 0) {
    errors.minMarginRate = '최소 예상 마진율은 0 이상이어야 합니다.';
  }
  if (values.categoryCodes.length === 0) {
    errors.categoryCodes = '카테고리를 하나 이상 선택하세요.';
  }
  return errors;
}

function getRuleSubmitReason(
  hasAccessToken: boolean,
  submitting: boolean,
  values: AlertRuleFormValues
) {
  if (!hasAccessToken) {
    return '계정 연결 후 조건을 저장할 수 있습니다.';
  }
  if (submitting) {
    return '알림 조건을 저장하는 중입니다.';
  }
  const errors = validateAlertRuleForm(values);
  return errors.name ?? errors.minScore ?? errors.minMarginRate ?? errors.categoryCodes ?? '';
}

function hasFormErrors(errors: AlertRuleFormErrors) {
  return Object.values(errors).some(Boolean);
}
