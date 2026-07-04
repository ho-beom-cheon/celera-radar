import { Fragment, FormEvent, useCallback, useEffect, useState } from 'react';
import {
  CandidateDetail,
  CandidateGrade,
  CandidateListItem,
  CandidateScoreBreakdown,
  CandidateSource,
  excludeCandidate,
  getCandidate,
  gradeLabels,
  listCandidates,
  riskLabels,
  saveCandidate,
  sourceLabels,
  statusLabels
} from '../../api/candidates';
import { CategoryCode, categoryOptions } from '../../api/keywords';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';
import { DataTable, EmptyState, ErrorState, HelpTooltip, LoadingState } from '../../components/ui';

const gradeOptions: Array<{ value: CandidateGrade; label: string }> = [
  { value: 'RECOMMENDED', label: '추천 검토' },
  { value: 'REVIEW', label: '검토' },
  { value: 'HOLD', label: '보류' },
  { value: 'EXCLUDED', label: '제외' }
];

const sourceOptions: Array<{ value: CandidateSource; label: string }> = [
  { value: 'KEYWORD', label: '키워드' },
  { value: 'CSV', label: 'CSV' },
  { value: 'API', label: 'API' }
];

export function CandidatesPage() {
  const [items, setItems] = useState<CandidateListItem[]>([]);
  const [detailsById, setDetailsById] = useState<Record<number, CandidateDetail>>({});
  const [expandedCandidateId, setExpandedCandidateId] = useState<number | null>(null);
  const [loadingDetailId, setLoadingDetailId] = useState<number | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [grade, setGrade] = useState<CandidateGrade | ''>('');
  const [categoryCode, setCategoryCode] = useState<CategoryCode | ''>('');
  const [source, setSource] = useState<CandidateSource | ''>('');
  const [minScore, setMinScore] = useState('');
  const [minMarginRate, setMinMarginRate] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const loadCandidates = useCallback(async () => {
    if (!getAccessToken()) {
      setItems([]);
      setTotalElements(0);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await listCandidates({
        grade,
        categoryCode,
        source,
        minScore: minScore === '' ? '' : Number(minScore),
        minMarginRate: minMarginRate === '' ? '' : Number(minMarginRate),
        page: 0,
        size: 50
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [categoryCode, grade, minMarginRate, minScore, source]);

  useEffect(() => {
    void loadCandidates();
  }, [loadCandidates]);

  async function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadCandidates();
  }

  function resetFilters() {
    setGrade('');
    setCategoryCode('');
    setSource('');
    setMinScore('');
    setMinMarginRate('');
  }

  async function handleSave(candidateId: number) {
    setMessage('');
    setError('');
    try {
      await saveCandidate(candidateId);
      setMessage('관심 후보로 저장했습니다.');
      await loadCandidates();
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  async function handleExclude(candidateId: number) {
    setMessage('');
    setError('');
    try {
      await excludeCandidate(candidateId);
      setMessage('후보를 제외 처리했습니다.');
      await loadCandidates();
      setExpandedCandidateId((current) => (current === candidateId ? null : current));
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  async function toggleBreakdown(candidateId: number) {
    setMessage('');
    setError('');
    if (expandedCandidateId === candidateId) {
      setExpandedCandidateId(null);
      return;
    }
    setExpandedCandidateId(candidateId);
    if (detailsById[candidateId]) {
      return;
    }
    setLoadingDetailId(candidateId);
    try {
      const detail = await getCandidate(candidateId);
      setDetailsById((current) => ({ ...current, [candidateId]: detail }));
    } catch (requestError) {
      setError(errorMessage(requestError));
      setExpandedCandidateId(null);
    } finally {
      setLoadingDetailId(null);
    }
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="후보 작업">
        <div className="toolbar-title">
          <p className="eyebrow">Candidates</p>
          <h1>상품 검토 후보</h1>
        </div>
        <div className="limit-meter">
          <span className="metric-label">
            <span>현재 조건 후보</span>
            <HelpTooltip contentKey="candidateCount" compact />
          </span>
          <strong>{totalElements}개</strong>
        </div>
      </section>

      <form className="panel keyword-form-panel" onSubmit={handleFilterSubmit}>
        <div className="panel-header">
          <div>
            <h2>후보 필터</h2>
            <p className="muted">점수, 마진율, 카테고리, 생성 소스로 데이터 기반 후보를 좁힙니다.</p>
          </div>
          <button type="button" className="ghost-button" onClick={resetFilters}>
            초기화
          </button>
        </div>
        <div className="form-grid form-grid-four">
          <label className="field">
            <span>등급</span>
            <select value={grade} onChange={(event) => setGrade(event.target.value as CandidateGrade | '')}>
              <option value="">전체</option>
              {gradeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>카테고리</span>
            <select
              value={categoryCode}
              onChange={(event) => setCategoryCode(event.target.value as CategoryCode | '')}
            >
              <option value="">전체</option>
              {categoryOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>소스</span>
            <select value={source} onChange={(event) => setSource(event.target.value as CandidateSource | '')}>
              <option value="">전체</option>
              {sourceOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>최소 점수</span>
            <input
              type="number"
              min="0"
              max="100"
              value={minScore}
              onChange={(event) => setMinScore(event.target.value)}
            />
          </label>
          <label className="field">
            <span>최소 마진율</span>
            <input
              type="number"
              min="0"
              step="0.1"
              value={minMarginRate}
              onChange={(event) => setMinMarginRate(event.target.value)}
            />
          </label>
        </div>
        <div className="button-row">
          <button type="submit" className="primary-button" disabled={!getAccessToken() || loading}>
            조회
          </button>
          <a className="secondary-button" href="/wholesale/uploads">
            CSV 후보 생성
          </a>
        </div>
      </form>

      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <ErrorState>{error}</ErrorState> : null}

      <section className="panel keywords-table-panel">
        <div className="panel-header table-header">
          <div>
            <h2>후보 목록</h2>
            <p className="muted">데이터 기반 검토 후보입니다. 판매나 수익을 보장하지 않습니다.</p>
          </div>
          <button type="button" className="ghost-button" onClick={() => void loadCandidates()}>
            새로고침
          </button>
        </div>

        <DataTable className="candidates-data-table">
            <thead>
              <tr>
                <th>상품</th>
                <th>등급</th>
                <th>
                  <span className="table-heading-help">
                    <span>점수</span>
                    <HelpTooltip contentKey="productScore" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>예상 판매가</span>
                    <HelpTooltip contentKey="salePrice" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>공급가</span>
                    <HelpTooltip contentKey="supplyPrice" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>예상 마진율</span>
                    <HelpTooltip contentKey="marginRate" compact />
                  </span>
                </th>
                <th>상태</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <Fragment key={item.candidateId}>
                  <tr>
                    <td className="strong-cell">
                      <div className="candidate-product">
                        <span className="candidate-avatar" aria-hidden="true">
                          {avatarText(item.name)}
                        </span>
                        <div>
                          <a className="detail-link" href={`/candidates/${item.candidateId}`}>
                            {item.name}
                          </a>
                          <span className="candidate-subtext">
                            {sourceLabels[item.source]} · {categoryLabel(item.categoryCode)}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td>{gradeLabels[item.grade]}</td>
                    <td>
                      <strong>{item.score}</strong>
                    </td>
                    <td>{formatCurrency(item.expectedSalePrice)}</td>
                    <td>{formatCurrency(item.supplyPrice)}</td>
                    <td>{formatPercent(item.expectedMarginRate)}</td>
                    <td>
                      {statusLabels[item.status]} · 위험 {riskLabels[item.riskLevel]}
                    </td>
                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="text-button text-button-neutral"
                          onClick={() => void toggleBreakdown(item.candidateId)}
                        >
                          {expandedCandidateId === item.candidateId ? '접기' : '점수 보기'}
                        </button>
                        <button
                          type="button"
                          className="text-button text-button-neutral"
                          onClick={() => void handleSave(item.candidateId)}
                          disabled={item.status === 'SAVED'}
                        >
                          관심
                        </button>
                        <button type="button" className="text-button" onClick={() => void handleExclude(item.candidateId)}>
                          제외
                        </button>
                      </div>
                    </td>
                  </tr>
                  {expandedCandidateId === item.candidateId ? (
                    <tr className="candidate-expanded-row">
                      <td colSpan={8}>
                        {loadingDetailId === item.candidateId ? (
                          <LoadingState>점수 구성을 불러오는 중입니다.</LoadingState>
                        ) : (
                          <CandidateBreakdown detail={detailsById[item.candidateId]} />
                        )}
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              ))}
            </tbody>
        </DataTable>

        {loading ? <LoadingState>후보를 불러오는 중입니다.</LoadingState> : null}
        {!loading && !getAccessToken() ? <EmptyState>키워드 레이더에서 계정 연결 후 후보를 확인할 수 있습니다.</EmptyState> : null}
        {!loading && getAccessToken() && items.length === 0 ? (
          <EmptyState>아직 후보가 없습니다. 도매 CSV에서 후보 생성을 먼저 실행하세요.</EmptyState>
        ) : null}
      </section>
    </div>
  );
}

function CandidateBreakdown({ detail }: { detail?: CandidateDetail }) {
  if (!detail) {
    return null;
  }
  return (
    <div className="candidate-breakdown-panel">
      <div className="score-grid">
        {scoreItems(detail.scoreBreakdown).map((item) => (
          <span key={item.label}>
            {item.label} <strong>{item.value}</strong>
          </span>
        ))}
      </div>
      <div className="candidate-breakdown-notes">
        <section>
          <h3>검토 이유</h3>
          <NoteList values={detail.reasons} emptyText="아직 기록된 검토 이유가 없습니다." />
        </section>
        <section>
          <h3>주의사항</h3>
          <NoteList values={detail.warnings} emptyText="아직 기록된 주의사항이 없습니다." />
        </section>
      </div>
    </div>
  );
}

function NoteList({ values, emptyText }: { values: string[]; emptyText: string }) {
  if (values.length === 0) {
    return <p className="muted">{emptyText}</p>;
  }
  return (
    <ul className="plain-list">
      {values.map((value) => (
        <li key={value}>{value}</li>
      ))}
    </ul>
  );
}

function scoreItems(breakdown: CandidateScoreBreakdown) {
  return [
    { label: '트렌드', value: breakdown.trendScore },
    { label: '경쟁', value: breakdown.competitionScore },
    { label: '마진', value: breakdown.marginScore },
    { label: '가격대', value: breakdown.priceScore },
    { label: '공급', value: breakdown.supplyScore },
    { label: '위험', value: breakdown.riskPenalty }
  ];
}

function categoryLabel(categoryCode: CategoryCode) {
  return categoryOptions.find((option) => option.value === categoryCode)?.label ?? categoryCode;
}

function avatarText(name: string) {
  const trimmed = name.trim();
  return trimmed.length === 0 ? '검토' : trimmed.slice(0, 1).toUpperCase();
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

function formatPercent(value: number | null) {
  if (value === null) {
    return '-';
  }
  return `${value}%`;
}

function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '요청을 처리하지 못했습니다.';
}
