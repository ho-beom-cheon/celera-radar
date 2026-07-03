import { useEffect, useState } from 'react';
import {
  CandidateDetail,
  excludeCandidate,
  getCandidate,
  gradeLabels,
  riskLabels,
  saveCandidate
} from '../../api/candidates';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';

interface CandidateDetailPageProps {
  candidateId: number;
}

export function CandidateDetailPage({ candidateId }: CandidateDetailPageProps) {
  const [candidate, setCandidate] = useState<CandidateDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;
    async function loadCandidate() {
      if (!getAccessToken()) {
        setError('계정 연결 후 후보 상세를 확인할 수 있습니다.');
        setLoading(false);
        return;
      }
      setLoading(true);
      setError('');
      try {
        const response = await getCandidate(candidateId);
        if (!ignore) {
          setCandidate(response);
        }
      } catch (requestError) {
        if (!ignore) {
          setError(errorMessage(requestError));
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }
    void loadCandidate();
    return () => {
      ignore = true;
    };
  }, [candidateId]);

  async function handleSave() {
    setError('');
    setMessage('');
    try {
      const response = await saveCandidate(candidateId);
      setCandidate(response);
      setMessage('관심 후보로 저장했습니다.');
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  async function handleExclude() {
    setError('');
    setMessage('');
    try {
      const response = await excludeCandidate(candidateId);
      setCandidate(response);
      setMessage('후보를 제외 처리했습니다.');
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  return (
    <div className="keywords-page detail-page">
      <a className="back-link" href="/alerts">
        알림 목록
      </a>

      {loading ? <div className="notice">후보 상세를 불러오는 중입니다.</div> : null}
      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <div className="notice notice-error">{error}</div> : null}

      {candidate ? (
        <>
          <section className="panel keyword-form-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Candidate</p>
                <h1>{candidate.name}</h1>
                <p className="muted">
                  {gradeLabels[candidate.grade]} · 위험 {riskLabels[candidate.riskLevel]}
                </p>
              </div>
              <strong className="score-pill">{candidate.score}</strong>
            </div>
            <div className="candidate-metrics">
              <span>예상 판매가 {formatCurrency(candidate.expectedSalePrice)}</span>
              <span>공급가 {formatCurrency(candidate.supplyPrice)}</span>
              <span>예상 마진율 {candidate.expectedMarginRate}%</span>
              <span>상태 {candidate.status}</span>
            </div>
            <div className="button-row">
              <button type="button" className="primary-button" onClick={() => void handleSave()}>
                관심 저장
              </button>
              <button type="button" className="secondary-button" onClick={() => void handleExclude()}>
                제외
              </button>
            </div>
          </section>

          <section className="panel keyword-form-panel">
            <h2>점수 구성</h2>
            <div className="score-grid">
              <span>트렌드 {candidate.scoreBreakdown.trendScore}</span>
              <span>경쟁 {candidate.scoreBreakdown.competitionScore}</span>
              <span>마진 {candidate.scoreBreakdown.marginScore}</span>
              <span>가격대 {candidate.scoreBreakdown.priceBandScore}</span>
              <span>공급 {candidate.scoreBreakdown.supplyScore}</span>
              <span>위험 {candidate.scoreBreakdown.riskPenalty}</span>
            </div>
          </section>

          <section className="panel keyword-form-panel">
            <h2>검토 이유</h2>
            <ul className="plain-list">
              {candidate.reasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
            <h2>주의사항</h2>
            <ul className="plain-list">
              {candidate.warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
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
