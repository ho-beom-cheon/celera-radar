import { useEffect, useState } from 'react';
import {
  KeywordAnalysis,
  ShoppingTopItem,
  getKeywordAnalysis,
  statusLabels
} from '../../api/keywords';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';

interface KeywordDetailPageProps {
  keywordId: number;
}

export function KeywordDetailPage({ keywordId }: KeywordDetailPageProps) {
  const [analysis, setAnalysis] = useState<KeywordAnalysis | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;
    async function loadAnalysis() {
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
      try {
        const response = await getKeywordAnalysis(keywordId);
        if (!ignore) {
          setAnalysis(response);
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
    void loadAnalysis();
    return () => {
      ignore = true;
    };
  }, [keywordId]);

  const shopping = analysis?.shopping ?? null;

  return (
    <div className="keywords-page detail-page">
      <section className="toolbar-row" aria-label="키워드 상세">
        <div className="toolbar-title">
          <a className="back-link" href="/keywords">
            키워드 목록
          </a>
          <h1>{analysis?.keyword ?? '키워드 분석'}</h1>
          {analysis ? (
            <p className="muted">
              <span className={`status-badge status-${analysis.status.toLowerCase()}`}>
                {statusLabels[analysis.status]}
              </span>
              <span className="inline-meta">마지막 분석 {formatDateTime(analysis.lastAnalyzedAt)}</span>
            </p>
          ) : null}
        </div>
        <div className="limit-meter cache-meter">
          <span>캐시 기준일</span>
          <strong>{shopping?.baseDate ?? '-'}</strong>
        </div>
      </section>

      {loading ? <div className="notice">분석 결과를 불러오는 중입니다.</div> : null}
      {error ? <div className="notice notice-error">{error}</div> : null}

      {!loading && !error && analysis && !shopping ? (
        <section className="panel empty-analysis-panel">
          <h2>분석 대기</h2>
          <p>아직 저장된 쇼핑 검색 스냅샷이 없습니다. 다음 쇼핑 검색 배치 이후 가격대와 상위 상품이 표시됩니다.</p>
        </section>
      ) : null}

      {shopping ? (
        <>
          <section className="summary-grid analysis-summary" aria-label="쇼핑 검색 요약">
            <article className="summary-card">
              <span>검색 결과 수</span>
              <strong>{formatNumber(shopping.totalResults)}</strong>
            </article>
            <article className="summary-card">
              <span>최저가</span>
              <strong>{formatCurrency(shopping.minPrice)}</strong>
            </article>
            <article className="summary-card">
              <span>평균가</span>
              <strong>{formatCurrency(shopping.avgPrice)}</strong>
            </article>
            <article className="summary-card">
              <span>최고가</span>
              <strong>{formatCurrency(shopping.maxPrice)}</strong>
            </article>
          </section>

          <section className="panel keywords-table-panel">
            <div className="panel-header">
              <div>
                <h2>상위 상품</h2>
                <p className="muted">네이버 쇼핑 검색 스냅샷 기준 상위 결과입니다.</p>
              </div>
            </div>
            <div className="table-wrap">
              <table className="data-table detail-table">
                <thead>
                  <tr>
                    <th>순위</th>
                    <th>상품</th>
                    <th>가격</th>
                    <th>몰</th>
                    <th>카테고리</th>
                  </tr>
                </thead>
                <tbody>
                  {shopping.topItems.map((item) => (
                    <tr key={`${item.itemRank}-${item.title}`}>
                      <td>{item.itemRank}</td>
                      <td className="product-cell">
                        {item.link ? (
                          <a href={item.link} target="_blank" rel="noreferrer">
                            {stripTags(item.title)}
                          </a>
                        ) : (
                          stripTags(item.title)
                        )}
                      </td>
                      <td>{formatPriceRange(item)}</td>
                      <td>{item.mallName ?? '-'}</td>
                      <td>{categoryPath(item)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {shopping.topItems.length === 0 ? (
              <div className="state-row">저장된 상위 상품이 없습니다.</div>
            ) : null}
          </section>
        </>
      ) : null}
    </div>
  );
}

function formatNumber(value: number) {
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

function formatPriceRange(item: ShoppingTopItem) {
  if (item.lprice === null && item.hprice === null) {
    return '-';
  }
  if (item.hprice !== null && item.lprice !== null && item.hprice > item.lprice) {
    return `${formatCurrency(item.lprice)} ~ ${formatCurrency(item.hprice)}`;
  }
  return formatCurrency(item.lprice ?? item.hprice);
}

function categoryPath(item: ShoppingTopItem) {
  const values = [item.category1, item.category2, item.category3, item.category4].filter(Boolean);
  return values.length > 0 ? values.join(' > ') : '-';
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

function stripTags(value: string) {
  return value.replace(/<[^>]*>/g, '');
}

function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '요청을 처리하지 못했습니다.';
}
