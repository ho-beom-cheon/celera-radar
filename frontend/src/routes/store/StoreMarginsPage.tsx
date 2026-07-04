import { useEffect, useMemo, useState } from 'react';
import {
  getStoreProductCost,
  listSmartStoreProducts,
  SmartStoreProduct,
  StoreProductCost
} from '../../api/smartstore';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';
import {
  DataTable,
  DataTableStateRow,
  EmptyState,
  ErrorState,
  HelpTooltip,
  LazyKpiDonutChart,
  LoadingState,
  MetricCard,
  StatusBadge
} from '../../components/ui';
import { authRequiredMessage, formatApiError } from '../../lib/apiError';

type MarginRisk = 'RISK' | 'CAUTION' | 'SAFE' | 'UNSET';

interface StoreMarginRow {
  product: SmartStoreProduct;
  cost: StoreProductCost | null;
  risk: MarginRisk;
}

const riskLabels: Record<MarginRisk, string> = {
  RISK: '위험',
  CAUTION: '주의',
  SAFE: '안전',
  UNSET: '미설정'
};

const riskClassNames: Record<MarginRisk, string> = {
  RISK: 'status-failed',
  CAUTION: 'status-pending',
  SAFE: 'status-success',
  UNSET: 'status-skipped'
};

export function StoreMarginsPage() {
  const hasAccessToken = Boolean(getAccessToken());
  const [rows, setRows] = useState<StoreMarginRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const counts = useMemo(
    () => ({
      risk: rows.filter((row) => row.risk === 'RISK').length,
      caution: rows.filter((row) => row.risk === 'CAUTION').length,
      safe: rows.filter((row) => row.risk === 'SAFE').length,
      unset: rows.filter((row) => row.risk === 'UNSET').length
    }),
    [rows]
  );

  const riskChartData = useMemo(
    () => [
      { label: '위험', value: counts.risk, color: 'var(--sr-color-danger-muted)' },
      { label: '주의', value: counts.caution, color: 'var(--sr-color-warning-muted)' },
      { label: '안전', value: counts.safe, color: 'var(--sr-color-success)' },
      { label: '원가 미설정', value: counts.unset, color: 'var(--sr-color-neutral)' }
    ],
    [counts.caution, counts.risk, counts.safe, counts.unset]
  );

  async function loadMargins() {
    if (!getAccessToken()) {
      setRows([]);
      setMessage('');
      return;
    }
    setLoading(true);
    setMessage('');
    try {
      const products = await listSmartStoreProducts(0, 100);
      const nextRows = await Promise.all(
        products.items.map(async (product) => {
          const cost = await loadCost(product.productId);
          return {
            product,
            cost,
            risk: resolveRisk(cost)
          };
        })
      );
      setRows(nextRows);
      setMessage('');
    } catch (error) {
      setMessage(formatApiError(error, '상품 마진 상태를 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadMargins();
  }, []);

  return (
    <div className="keywords-page">
      <header className="toolbar-row">
        <div className="toolbar-title">
          <p className="eyebrow">SmartStore Margin</p>
          <h1>내 상품 마진 위험</h1>
          <p className="muted">
            동기화된 스마트스토어 상품의 판매가와 등록된 원가 기준으로 예상 마진 상태를 확인합니다.
          </p>
        </div>
        <button className="secondary-button" type="button" onClick={loadMargins} disabled={loading}>
          새로고침
        </button>
      </header>

      {message ? <ErrorState>{message}</ErrorState> : null}

      <section className="summary-grid" aria-label="마진 위험 요약">
        <MetricCard label="위험" value={String(counts.risk)} helpKey="riskLevel" />
        <MetricCard label="주의" value={String(counts.caution)} helpKey="riskLevel" />
        <MetricCard label="안전" value={String(counts.safe)} helpKey="riskLevel" />
        <MetricCard label="원가 미설정" value={String(counts.unset)} helpKey="riskLevel" />
      </section>

      <LazyKpiDonutChart
        title="마진 위험 분포"
        description="동기화된 상품을 원가와 목표 마진 기준으로 나눈 검토용 분포입니다."
        data={riskChartData}
      />

      <section className="panel keywords-table-panel">
        <div className="panel-header table-header">
          <div>
            <h2>상품별 예상 마진</h2>
            <p className="muted">목표 마진율과 현재 예상 마진율을 비교해 검토 우선순위를 나눕니다.</p>
          </div>
        </div>

        <DataTable className="store-margin-data-table">
            <thead>
              <tr>
                <th>상품</th>
                <th>
                  <span className="table-heading-help">
                    <span>판매가</span>
                    <HelpTooltip contentKey="salePrice" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>원가</span>
                    <HelpTooltip contentKey="totalCost" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>예상 마진율</span>
                    <HelpTooltip contentKey="marginRate" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>예상 이익</span>
                    <HelpTooltip contentKey="expectedMargin" compact />
                  </span>
                </th>
                <th>
                  <span className="table-heading-help">
                    <span>위험 상태</span>
                    <HelpTooltip contentKey="riskLevel" compact />
                  </span>
                </th>
                <th>원가 출처</th>
                <th>
                  <span className="table-heading-help">
                    <span>최근 동기화</span>
                    <HelpTooltip contentKey="lastSyncedAt" compact />
                  </span>
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <DataTableStateRow colSpan={8}>
                  <LoadingState>상품 마진 상태를 불러오는 중입니다.</LoadingState>
                </DataTableStateRow>
              ) : null}
              {!loading && !hasAccessToken ? (
                <DataTableStateRow colSpan={8}>
                  <EmptyState>{authRequiredMessage('내 상품 마진 상태를 확인')}</EmptyState>
                </DataTableStateRow>
              ) : null}
              {!loading && hasAccessToken && rows.length === 0 ? (
                <DataTableStateRow colSpan={8}>
                  <EmptyState>동기화된 스마트스토어 상품이 없습니다.</EmptyState>
                </DataTableStateRow>
              ) : null}
              {!loading
                ? rows.map((row) => (
                    <tr key={row.product.productId}>
                      <td>
                        <strong className="strong-cell">{row.product.productName}</strong>
                        <span className="candidate-subtext">
                          {row.product.saleStatus} · {row.product.sourceProductId}
                        </span>
                      </td>
                      <td>{formatCurrency(row.product.salePrice)}</td>
                      <td>{row.cost ? formatCurrency(row.cost.totalCost) : '미설정'}</td>
                      <td>{row.cost ? formatPercent(row.cost.expectedMarginRate) : '-'}</td>
                      <td>{row.cost ? formatCurrency(row.cost.expectedProfit) : '-'}</td>
                      <td>
                        <StatusBadge className={riskClassNames[row.risk]}>{riskLabels[row.risk]}</StatusBadge>
                      </td>
                      <td>{row.cost?.wholesaleProductName ?? (row.cost ? '수동 입력' : '원가 매핑 필요')}</td>
                      <td>{formatDateTime(row.product.lastSyncedAt)}</td>
                    </tr>
                  ))
                : null}
            </tbody>
        </DataTable>
      </section>
    </div>
  );
}

async function loadCost(productId: number) {
  try {
    return await getStoreProductCost(productId);
  } catch (error) {
    if (error instanceof ApiRequestError && error.code === 'STORE_PRODUCT_COST_NOT_FOUND') {
      return null;
    }
    throw error;
  }
}

function resolveRisk(cost: StoreProductCost | null): MarginRisk {
  if (!cost) {
    return 'UNSET';
  }
  if (cost.expectedProfit < 0 || cost.expectedMarginRate < 5) {
    return 'RISK';
  }
  if (cost.expectedMarginRate < cost.targetMarginRate) {
    return 'CAUTION';
  }
  return 'SAFE';
}

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return '-';
  }
  return `${Math.round(value).toLocaleString('ko-KR')}원`;
}

function formatPercent(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return '-';
  }
  return `${value.toFixed(2)}%`;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}
