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
import { DataTable, DataTableStateRow, EmptyState, ErrorState, HelpTooltip, LoadingState, StatusBadge } from '../../components/ui';
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

export function AlertsPage({ mode }: AlertsPageProps) {
  const hasAccessToken = Boolean(getAccessToken());
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [ruleName, setRuleName] = useState('추천점수 80 이상');
  const [minScore, setMinScore] = useState(80);
  const [minMarginRate, setMinMarginRate] = useState(25);
  const [selectedCategories, setSelectedCategories] = useState<CategoryCode[]>(['CAR_ACCESSORY']);
  const [riskExcluded, setRiskExcluded] = useState(true);
  const [frequency, setFrequency] = useState<AlertFrequency>('DAILY_SUMMARY');

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
    const payload: AlertRulePayload = {
      name: ruleName,
      minScore,
      minMarginRate,
      categoryCodes: selectedCategories,
      riskExcluded,
      frequency
    };
    setError('');
    setMessage('');
    try {
      await createAlertRule(payload);
      setMessage('알림 조건을 저장했습니다. 다음 알림 생성 배치부터 적용됩니다.');
    } catch (requestError) {
      setError(formatApiError(requestError));
    }
  }

  function toggleCategory(categoryCode: CategoryCode) {
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
              <input value={ruleName} onChange={(event) => setRuleName(event.target.value)} />
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
                onChange={(event) => setMinScore(Number(event.target.value))}
              />
            </label>
            <label className="field">
              <span>최소 예상 마진율</span>
              <input
                type="number"
                min={0}
                value={minMarginRate}
                onChange={(event) => setMinMarginRate(Number(event.target.value))}
              />
            </label>
          </div>
          <fieldset className="category-checks">
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
            <button type="submit" className="primary-button" disabled={!hasAccessToken}>
              조건 저장
            </button>
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
