import { useCallback, useEffect, useState } from 'react';
import {
  KeywordItem,
  ShoppingSnapshot,
  analyzeShopping,
  competitionLabels,
  getKeyword,
  getLatestShoppingSnapshot,
  statusLabels
} from '../../api/keywords';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';
import { ProductCard } from './ProductCard';

interface KeywordDetailPageProps {
  keywordId: number;
}

export function KeywordDetailPage({ keywordId }: KeywordDetailPageProps) {
  const [keyword, setKeyword] = useState<KeywordItem | null>(null);
  const [snapshot, setSnapshot] = useState<ShoppingSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadDetail = useCallback(async () => {
    if (!Number.isFinite(keywordId) || keywordId <= 0) {
      setError('키워드를 찾을 수 없습니다.');
      setLoading(false);
      return;
    }
    if (!getAccessToken()) {
      setError('계정 연결 후 분석 결과를 확인할 수 있습니다.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');
    setNotice('');
    try {
      const keywordResponse = await getKeyword(keywordId);
      setKeyword(keywordResponse);
      try {
        const snapshotResponse = await getLatestShoppingSnapshot(keywordId);
        setSnapshot(snapshotResponse);
      } catch (requestError) {
        if (isAnalysisNotReady(requestError)) {
          setSnapshot(null);
          setNotice('아직 저장된 쇼핑 스냅샷이 없습니다. 분석 실행 후 상품 카드를 확인할 수 있습니다.');
        } else {
          throw requestError;
        }
      }
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [keywordId]);

  useEffect(() => {
    void loadDetail();
  }, [loadDetail]);

  async function handleAnalyzeShopping() {
    if (analyzing) {
      return;
    }
    setAnalyzing(true);
    setError('');
    setNotice('');
    try {
      const response = await analyzeShopping(keywordId);
      setSnapshot(response);
      setKeyword((current) =>
        current
          ? {
              ...current,
              analysisStatus: 'SUCCESS',
              lastAnalyzedAt: response.fetchedAt ?? current.lastAnalyzedAt,
              lastSnapshotDate: response.searchDate,
              latestMinPrice: response.minPrice,
              latestAvgPrice: response.avgPrice,
              latestCompetitionLevel: response.competitionLevel
            }
          : current
      );
      setNotice(response.cached ? '오늘 저장된 쇼핑 스냅샷을 재사용했습니다.' : '쇼핑 분석이 완료되었습니다.');
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setAnalyzing(false);
    }
  }

  const title = keyword?.keyword ?? snapshot?.keyword ?? '키워드 상세';

  return (
    <div className="keywords-page detail-page">
      <section className="toolbar-row" aria-label="키워드 상세">
        <div className="toolbar-title">
          <a className="back-link" href="/keywords">
            키워드 목록
          </a>
          <h1>{title}</h1>
          {keyword ? (
            <p className="muted">
              <span className={`status-badge status-${keyword.analysisStatus.toLowerCase()}`}>
                {statusLabels[keyword.analysisStatus]}
              </span>
              <span className="inline-meta">마지막 분석 {formatDateTime(keyword.lastAnalyzedAt)}</span>
            </p>
          ) : null}
        </div>
        <div className="detail-actions">
          <div className="limit-meter cache-meter">
            <span>스냅샷 기준일</span>
            <strong>{snapshot?.searchDate ?? keyword?.lastSnapshotDate ?? '-'}</strong>
          </div>
          <button type="button" className="primary-button" onClick={() => void handleAnalyzeShopping()} disabled={loading || analyzing}>
            {analyzing ? '분석 중' : '쇼핑 분석 실행'}
          </button>
        </div>
      </section>

      {loading ? <div className="notice">키워드 상세 정보를 불러오는 중입니다.</div> : null}
      {notice ? <div className="notice notice-success">{notice}</div> : null}
      {error ? <div className="notice notice-error">{error}</div> : null}

      {!loading && !error && !snapshot ? (
        <section className="panel empty-analysis-panel">
          <h2>쇼핑 스냅샷 대기</h2>
          <p>저장된 네이버 쇼핑 스냅샷이 없습니다. 분석을 실행하면 가격 요약과 상위 상품 카드가 표시됩니다.</p>
        </section>
      ) : null}

      {snapshot ? (
        <>
          <section className="summary-grid analysis-summary" aria-label="쇼핑 검색 요약">
            <article className="summary-card">
              <span>검색 결과 수</span>
              <strong>{formatNumber(snapshot.totalCount)}</strong>
            </article>
            <article className="summary-card">
              <span>최저가</span>
              <strong>{formatCurrency(snapshot.minPrice)}</strong>
            </article>
            <article className="summary-card">
              <span>평균가</span>
              <strong>{formatCurrency(snapshot.avgPrice)}</strong>
            </article>
            <article className="summary-card">
              <span>최고가</span>
              <strong>{formatCurrency(snapshot.maxPrice)}</strong>
            </article>
            <article className="summary-card">
              <span>경쟁강도</span>
              <strong className="summary-badge-value">
                <span className={`competition-badge competition-${(snapshot.competitionLevel ?? 'UNKNOWN').toLowerCase()}`}>
                  {competitionLabels[snapshot.competitionLevel ?? 'UNKNOWN']}
                </span>
              </strong>
            </article>
          </section>

          <section className="panel keywords-table-panel">
            <div className="panel-header">
              <div>
                <h2>상위 상품 카드</h2>
                <p className="muted">
                  {snapshot.searchDate} 기준 네이버 쇼핑 스냅샷입니다. {snapshot.cached ? '캐시 결과를 표시 중입니다.' : '새로 수집한 결과입니다.'}
                </p>
              </div>
              <span className="muted">수집 시각 {formatDateTime(snapshot.fetchedAt)}</span>
            </div>
            {snapshot.topItems.length > 0 ? (
              <div className="product-card-grid">
                {snapshot.topItems.map((item) => (
                  <ProductCard key={`${item.rankNo}-${item.productUrl ?? item.title}`} item={item} />
                ))}
              </div>
            ) : (
              <div className="state-row">저장된 상위 상품이 없습니다.</div>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}

function formatNumber(value: number | null) {
  if (value === null) {
    return '-';
  }
  return new Intl.NumberFormat('ko-KR').format(value);
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

function formatDateTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short'
  }).format(new Date(value));
}

function isAnalysisNotReady(error: unknown) {
  return error instanceof ApiRequestError && error.code === 'ANALYSIS_NOT_READY';
}

function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '요청을 처리하지 못했습니다.';
}
