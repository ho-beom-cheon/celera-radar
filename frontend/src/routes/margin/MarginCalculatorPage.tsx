import { useMemo, useState } from 'react';

export function MarginCalculatorPage() {
  const [supplyPrice, setSupplyPrice] = useState(12000);
  const [shippingFee, setShippingFee] = useState(3000);
  const [targetMarginRate, setTargetMarginRate] = useState(25);
  const [salePrice, setSalePrice] = useState(20000);

  const calculated = useMemo(() => {
    const safeSupply = Math.max(0, supplyPrice);
    const safeShipping = Math.max(0, shippingFee);
    const totalCost = safeSupply + safeShipping;
    const targetRatio = Math.min(Math.max(targetMarginRate, 1), 90) / 100;
    const recommendedSalePrice = totalCost > 0 ? roundUpToHundred(totalCost / (1 - targetRatio)) : 0;
    const manualSalePrice = Math.max(0, salePrice);
    const marginAmount = manualSalePrice - totalCost;
    const marginRate = manualSalePrice > 0 ? (marginAmount / manualSalePrice) * 100 : 0;
    const recommendedMarginAmount = recommendedSalePrice - totalCost;
    const recommendedMarginRate =
      recommendedSalePrice > 0 ? (recommendedMarginAmount / recommendedSalePrice) * 100 : 0;

    return {
      totalCost,
      recommendedSalePrice,
      recommendedMarginAmount,
      recommendedMarginRate,
      marginAmount,
      marginRate
    };
  }, [salePrice, shippingFee, supplyPrice, targetMarginRate]);

  function applyRecommendedPrice() {
    setSalePrice(calculated.recommendedSalePrice);
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="마진 계산">
        <div className="toolbar-title">
          <p className="eyebrow">Margin</p>
          <h1>마진 계산기</h1>
        </div>
        <div className="limit-meter">
          <span>현재 마진율</span>
          <strong>{formatRate(calculated.marginRate)}</strong>
        </div>
      </section>

      <section className="calculator-grid">
        <section className="panel keyword-form-panel">
          <div className="panel-header">
            <div>
              <h2>원가 입력</h2>
              <p className="muted">공급가와 배송비를 기준으로 검토용 판매가를 계산합니다.</p>
            </div>
          </div>
          <div className="form-grid form-grid-two">
            <label className="field">
              <span>공급가</span>
              <input
                type="number"
                min="0"
                value={supplyPrice}
                onChange={(event) => setSupplyPrice(Number(event.target.value))}
              />
            </label>
            <label className="field">
              <span>배송비</span>
              <input
                type="number"
                min="0"
                value={shippingFee}
                onChange={(event) => setShippingFee(Number(event.target.value))}
              />
            </label>
            <label className="field">
              <span>목표 마진율</span>
              <input
                type="number"
                min="1"
                max="90"
                step="0.1"
                value={targetMarginRate}
                onChange={(event) => setTargetMarginRate(Number(event.target.value))}
              />
            </label>
            <label className="field">
              <span>판매가 직접 입력</span>
              <input
                type="number"
                min="0"
                value={salePrice}
                onChange={(event) => setSalePrice(Number(event.target.value))}
              />
            </label>
          </div>
          <div className="button-row">
            <button type="button" className="primary-button" onClick={applyRecommendedPrice}>
              권장가 적용
            </button>
          </div>
        </section>

        <section className="panel keyword-form-panel">
          <div className="panel-header">
            <div>
              <h2>계산 결과</h2>
              <p className="muted">세금, 플랫폼 수수료, 반품 비용은 별도 검토가 필요합니다.</p>
            </div>
          </div>
          <div className="result-grid">
            <Metric label="총 원가" value={formatCurrency(calculated.totalCost)} />
            <Metric label="목표 기준 권장 판매가" value={formatCurrency(calculated.recommendedSalePrice)} />
            <Metric label="권장가 기준 마진" value={formatCurrency(calculated.recommendedMarginAmount)} />
            <Metric label="권장가 기준 마진율" value={formatRate(calculated.recommendedMarginRate)} />
            <Metric label="입력 판매가 기준 마진" value={formatCurrency(calculated.marginAmount)} />
            <Metric label="입력 판매가 기준 마진율" value={formatRate(calculated.marginRate)} />
          </div>
        </section>
      </section>

      <div className="notice">
        계산 결과는 도매 CSV 후보 검토를 돕기 위한 참고값입니다. 판매나 수익을 보장하지 않습니다.
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric-box">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function roundUpToHundred(value: number) {
  return Math.ceil(value / 100) * 100;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0
  }).format(value);
}

function formatRate(value: number) {
  return `${value.toFixed(1)}%`;
}
