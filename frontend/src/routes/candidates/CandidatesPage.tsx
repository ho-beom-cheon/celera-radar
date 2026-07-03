import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  CandidateGrade,
  CandidateListItem,
  CandidateSource,
  excludeCandidate,
  gradeLabels,
  listCandidates,
  riskLabels,
  saveCandidate
} from '../../api/candidates';
import { CategoryCode, categoryOptions } from '../../api/keywords';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';

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
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="후보 작업">
        <div className="toolbar-title">
          <p className="eyebrow">Candidates</p>
          <h1>상품 후보</h1>
        </div>
        <div className="limit-meter">
          <span>현재 조건</span>
          <strong>{totalElements}개</strong>
        </div>
      </section>

      <form className="panel keyword-form-panel" onSubmit={handleFilterSubmit}>
        <div className="panel-header">
          <div>
            <h2>후보 필터</h2>
            <p className="muted">점수, 마진율, 카테고리, 생성 소스로 검토 후보를 좁힙니다.</p>
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
          <a className="secondary-button" href="/wholesale">
            CSV 후보 생성
          </a>
        </div>
      </form>

      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <div className="notice notice-error">{error}</div> : null}

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

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>상품명</th>
                <th>등급</th>
                <th>점수</th>
                <th>예상 판매가</th>
                <th>예상 마진율</th>
                <th>위험</th>
                <th>소스</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.candidateId}>
                  <td className="strong-cell">
                    <a className="detail-link" href={`/candidates/${item.candidateId}`}>
                      {item.name}
                    </a>
                  </td>
                  <td>{gradeLabels[item.grade]}</td>
                  <td>
                    <strong>{item.score}</strong>
                  </td>
                  <td>{formatCurrency(item.expectedSalePrice)}</td>
                  <td>{item.expectedMarginRate}%</td>
                  <td>{riskLabels[item.riskLevel]}</td>
                  <td>{sourceLabel(item.source)}</td>
                  <td>
                    <div className="table-actions">
                      <button type="button" className="text-button text-button-neutral" onClick={() => void handleSave(item.candidateId)}>
                        관심
                      </button>
                      <button type="button" className="text-button" onClick={() => void handleExclude(item.candidateId)}>
                        제외
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {loading ? <div className="state-row">후보를 불러오는 중입니다.</div> : null}
        {!loading && !getAccessToken() ? <div className="state-row">키워드 레이더에서 계정 연결 후 후보를 확인할 수 있습니다.</div> : null}
        {!loading && getAccessToken() && items.length === 0 ? (
          <div className="state-row">아직 후보가 없습니다. 도매 CSV에서 후보 생성을 먼저 실행하세요.</div>
        ) : null}
      </section>
    </div>
  );
}

function sourceLabel(source: CandidateSource) {
  return sourceOptions.find((option) => option.value === source)?.label ?? source;
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

function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '요청을 처리하지 못했습니다.';
}
