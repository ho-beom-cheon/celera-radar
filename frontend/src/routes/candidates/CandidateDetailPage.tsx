import { useEffect, useState } from 'react';
import {
  CandidateDetail,
  CandidateScoreBreakdown,
  excludeCandidate,
  getCandidate,
  gradeLabels,
  riskLabels,
  saveCandidate,
  sourceLabels,
  statusLabels
} from '../../api/candidates';
import { categoryOptions } from '../../api/keywords';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';
import { ErrorState, HelpText, LoadingState } from '../../components/ui';

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
      <a className="back-link" href="/candidates">
        후보 목록
      </a>

      {loading ? <LoadingState>후보 상세를 불러오는 중입니다.</LoadingState> : null}
      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <ErrorState>{error}</ErrorState> : null}

      {candidate ? (
        <>
          <section className="panel keyword-form-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Candidate</p>
                <h1>{candidate.name}</h1>
                <p className="muted">
                  {gradeLabels[candidate.grade]} · 위험 {riskLabels[candidate.riskLevel]} · {statusLabels[candidate.status]}
                </p>
              </div>
              <strong className="score-pill" aria-label={`상품 검토 점수 ${candidate.score}점`}>
                {candidate.score}
              </strong>
            </div>
            <div className="candidate-metrics">
              <span>예상 판매가 {formatCurrency(candidate.expectedSalePrice)}</span>
              <span>공급가 {formatCurrency(candidate.supplyPrice)}</span>
              <span>배송비 {formatCurrency(candidate.shippingFee)}</span>
              <span>예상 마진율 {formatPercent(candidate.expectedMarginRate)}</span>
              <span>소스 {sourceLabels[candidate.source]}</span>
              <span>카테고리 {categoryLabel(candidate.categoryCode)}</span>
              <span>상태 {statusLabels[candidate.status]}</span>
              <span>연결 키워드 {candidate.keywordId ?? '-'}</span>
            </div>
            <div className="button-row">
              <button
                type="button"
                className="primary-button"
                onClick={() => void handleSave()}
                disabled={candidate.status === 'SAVED'}
              >
                관심 저장
              </button>
              <button type="button" className="secondary-button" onClick={() => void handleExclude()}>
                제외
              </button>
            </div>
          </section>

          <section className="panel keyword-form-panel">
            <div className="panel-header">
              <div>
                <h2>점수 구성</h2>
                <p className="muted">검토 후보 점수는 트렌드, 경쟁, 마진, 가격대, 공급, 위험 기준으로 계산됩니다.</p>
                <HelpText contentKey="productScore" />
              </div>
            </div>
            <div className="score-grid">
              {scoreItems(candidate.scoreBreakdown).map((item) => (
                <span key={item.label}>
                  {item.label} <strong>{item.value}</strong>
                </span>
              ))}
            </div>
          </section>

          <section className="panel keyword-form-panel">
            <div className="candidate-breakdown-notes">
              <section>
                <h2>검토 이유</h2>
                <NoteList values={candidate.reasons} emptyText="아직 기록된 검토 이유가 없습니다." />
              </section>
              <section>
                <h2>주의사항</h2>
                <NoteList values={candidate.warnings} emptyText="아직 기록된 주의사항이 없습니다." />
              </section>
            </div>
          </section>
        </>
      ) : null}
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

function categoryLabel(categoryCode: string) {
  return categoryOptions.find((option) => option.value === categoryCode)?.label ?? categoryCode;
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
