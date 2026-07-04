import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { login, signup, Plan } from '../../api/auth';
import {
  AnalysisStatus,
  KeywordItem,
  KeywordCategory,
  categoryOptions,
  competitionLabels,
  createKeyword,
  deleteKeyword,
  listKeywords,
  statusLabels
} from '../../api/keywords';
import {
  clearAccessToken,
  getAccessToken,
  getStoredPlan,
  setAccessToken,
  setStoredPlan
} from '../../api/httpClient';
import { DataTable, EmptyState, ErrorState, LoadingState, StatusBadge } from '../../components/ui';
import { formatApiError } from '../../lib/apiError';

const planLimits: Record<Plan, number> = {
  FREE: 3,
  BASIC: 30,
  PRO: 100
};

const statusOptions: Array<{ value: AnalysisStatus; label: string }> = [
  { value: 'PENDING', label: '대기' },
  { value: 'RUNNING', label: '분석 중' },
  { value: 'SUCCESS', label: '완료' },
  { value: 'FAILED', label: '실패' },
  { value: 'SKIPPED', label: '건너뜀' }
];

export function KeywordsPage() {
  const [accessToken, setTokenState] = useState(() => getAccessToken());
  const [plan, setPlan] = useState<Plan>(() => {
    const storedPlan = getStoredPlan();
    return storedPlan === 'BASIC' || storedPlan === 'PRO' ? storedPlan : 'FREE';
  });
  const [authEmail, setAuthEmail] = useState('seller@example.com');
  const [authPassword, setAuthPassword] = useState('password1234');
  const [termsAgreed, setTermsAgreed] = useState(true);
  const [keywords, setKeywords] = useState<KeywordItem[]>([]);
  const [usageCount, setUsageCount] = useState(0);
  const [categoryFilter, setCategoryFilter] = useState<KeywordCategory | ''>('');
  const [statusFilter, setStatusFilter] = useState<AnalysisStatus | ''>('');
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState<KeywordCategory>('CAR_ACCESSORY');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const limit = planLimits[plan];
  const limitReached = usageCount >= limit;

  const usageText = useMemo(
    () => `${plan} 플랜 ${limit}개 중 ${usageCount}개 사용`,
    [limit, plan, usageCount]
  );

  const loadKeywords = useCallback(async () => {
    if (!accessToken) {
      setKeywords([]);
      setUsageCount(0);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const [filteredPage, usagePage] = await Promise.all([
        listKeywords({
          category: categoryFilter,
          analysisStatus: statusFilter,
          page: 0,
          size: 20
        }),
        listKeywords({ page: 0, size: 1 })
      ]);
      setKeywords(filteredPage.items);
      setUsageCount(usagePage.totalElements);
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setLoading(false);
    }
  }, [accessToken, categoryFilter, statusFilter]);

  useEffect(() => {
    void loadKeywords();
  }, [loadKeywords]);

  async function handleAuthSubmit(mode: 'login' | 'signup') {
    setAuthLoading(true);
    setError('');
    setMessage('');
    try {
      const response =
        mode === 'login'
          ? await login(authEmail, authPassword)
          : await signup(authEmail, authPassword, termsAgreed);
      setAccessToken(response.accessToken);
      setStoredPlan(response.plan);
      setTokenState(response.accessToken);
      setPlan(response.plan);
      setMessage(`${response.email} 계정으로 연결되었습니다.`);
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setAuthLoading(false);
    }
  }

  function handleLogout() {
    clearAccessToken();
    setTokenState(null);
    setKeywords([]);
    setUsageCount(0);
    setMessage('');
  }

  async function handleCreateKeyword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || submitting || limitReached) {
      return;
    }
    setSubmitting(true);
    setError('');
    setMessage('');
    try {
      await createKeyword({
        keyword,
        category
      });
      setKeyword('');
      setMessage('키워드가 등록되었습니다.');
      await loadKeywords();
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteKeyword(keywordId: number) {
    const confirmed = window.confirm('이 키워드를 삭제할까요?');
    if (!confirmed) {
      return;
    }
    setError('');
    setMessage('');
    try {
      await deleteKeyword(keywordId);
      setMessage('키워드가 삭제되었습니다.');
      await loadKeywords();
    } catch (requestError) {
      setError(formatApiError(requestError));
    }
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="키워드 작업">
        <div className="toolbar-title">
          <p className="eyebrow">Keyword Radar</p>
          <h1>키워드 레이더</h1>
        </div>
        <div className={`limit-meter ${limitReached ? 'limit-meter-warning' : ''}`}>
          <span>{usageText}</span>
          <strong>{limitReached ? '한도 도달' : '등록 가능'}</strong>
        </div>
      </section>

      <section className="keywords-layout">
        <div className="panel auth-panel">
          <div className="panel-header">
            <h2>계정</h2>
            {accessToken ? (
              <button type="button" className="ghost-button" onClick={handleLogout}>
                로그아웃
              </button>
            ) : null}
          </div>
          <div className="form-grid">
            <label className="field">
              <span>이메일</span>
              <input
                type="email"
                value={authEmail}
                onChange={(event) => setAuthEmail(event.target.value)}
                autoComplete="email"
              />
            </label>
            <label className="field">
              <span>비밀번호</span>
              <input
                type="password"
                value={authPassword}
                onChange={(event) => setAuthPassword(event.target.value)}
                autoComplete="current-password"
              />
            </label>
            <label className="checkbox-field">
              <input
                type="checkbox"
                checked={termsAgreed}
                onChange={(event) => setTermsAgreed(event.target.checked)}
              />
              <span>이용약관 동의</span>
            </label>
          </div>
          <div className="button-row">
            <button
              type="button"
              className="primary-button"
              onClick={() => void handleAuthSubmit('login')}
              disabled={authLoading}
            >
              로그인
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => void handleAuthSubmit('signup')}
              disabled={authLoading}
            >
              가입
            </button>
          </div>
        </div>

        <form className="panel keyword-form-panel" onSubmit={handleCreateKeyword}>
          <div className="panel-header">
            <h2>키워드 등록</h2>
            <span className="muted">{limitReached ? '무료 한도를 확인하세요.' : '다음 배치에서 분석됩니다.'}</span>
          </div>
          <div className="form-grid">
            <label className="field">
              <span>키워드</span>
              <input
                type="text"
                value={keyword}
                minLength={2}
                maxLength={50}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="차량용 수납함"
                disabled={!accessToken || limitReached}
              />
            </label>
            <label className="field">
              <span>카테고리</span>
              <select
                value={category}
                onChange={(event) => setCategory(event.target.value)}
                disabled={!accessToken || limitReached}
              >
                {categoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="button-row">
            <button
              type="submit"
              className="primary-button"
              disabled={!accessToken || submitting || keyword.trim().length < 2 || limitReached}
            >
              등록
            </button>
          </div>
        </form>
      </section>

      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <ErrorState>{error}</ErrorState> : null}

      <section className="panel keywords-table-panel">
        <div className="panel-header table-header">
          <div>
            <h2>키워드 목록</h2>
            <p className="muted">등록한 키워드는 다음 배치에서 분석됩니다.</p>
          </div>
          <div className="filter-row">
            <label className="field compact-field">
              <span>카테고리</span>
              <select
                value={categoryFilter}
                onChange={(event) => setCategoryFilter(event.target.value)}
                disabled={!accessToken}
              >
                <option value="">전체</option>
                {categoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field compact-field">
              <span>상태</span>
              <select
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value as AnalysisStatus | '')}
                disabled={!accessToken}
              >
                <option value="">전체</option>
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <DataTable className="keywords-data-table">
            <thead>
              <tr>
                <th>키워드</th>
                <th>카테고리</th>
                <th>상태</th>
                <th>최저가</th>
                <th>평균가</th>
                <th>경쟁강도</th>
                <th>마지막 분석</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {keywords.map((item) => (
                <tr key={item.id}>
                  <td className="strong-cell">
                    <a className="detail-link" href={`/keywords/${item.id}`}>
                      {item.keyword}
                    </a>
                  </td>
                  <td>{categoryLabel(item.category)}</td>
                  <td>
                    <StatusBadge tone={item.analysisStatus}>{statusLabels[item.analysisStatus]}</StatusBadge>
                  </td>
                  <td>{formatCurrency(item.latestMinPrice)}</td>
                  <td>{formatCurrency(item.latestAvgPrice)}</td>
                  <td>
                    <span className={`competition-badge competition-${(item.latestCompetitionLevel ?? 'UNKNOWN').toLowerCase()}`}>
                      {competitionLabels[item.latestCompetitionLevel ?? 'UNKNOWN']}
                    </span>
                  </td>
                  <td>{formatDateTime(item.lastAnalyzedAt)}</td>
                  <td>
                    <button
                      type="button"
                      className="text-button"
                      onClick={() => void handleDeleteKeyword(item.id)}
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
        </DataTable>

        {loading ? <LoadingState>불러오는 중입니다.</LoadingState> : null}
        {!loading && !accessToken ? <EmptyState>계정 연결 후 키워드를 등록할 수 있습니다.</EmptyState> : null}
        {!loading && accessToken && keywords.length === 0 ? (
          <EmptyState>아직 등록한 키워드가 없습니다. 관심 상품 키워드를 추가하세요.</EmptyState>
        ) : null}
      </section>
    </div>
  );
}

function categoryLabel(category: KeywordCategory | null) {
  if (!category) {
    return '-';
  }
  return categoryOptions.find((option) => option.value === category)?.label ?? category;
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

function formatCurrency(value: number | null) {
  if (value === null) {
    return '-';
  }
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0
  }).format(value);
}
