import { useMemo, useState } from 'react';
import { FieldMessage, HelpTooltip, LazyKpiBarChart, MetricCard } from '../../components/ui';
import { hasFormErrors, isBlank, parseFiniteNumber } from '../../lib/formValidation';

interface MarginFormErrors {
  supplyPrice?: string;
  shippingFee?: string;
  targetMarginRate?: string;
  salePrice?: string;
}

export function MarginCalculatorPage() {
  const [supplyPrice, setSupplyPrice] = useState('12000');
  const [shippingFee, setShippingFee] = useState('3000');
  const [targetMarginRate, setTargetMarginRate] = useState('25');
  const [salePrice, setSalePrice] = useState('20000');

  const formErrors = validateMarginForm({
    supplyPrice,
    shippingFee,
    targetMarginRate,
    salePrice
  });
  const calculationReady = !hasFormErrors(formErrors);

  const calculated = useMemo(() => {
    const safeSupply = parseFiniteNumber(supplyPrice) ?? 0;
    const safeShipping = parseOptionalNumber(shippingFee) ?? 0;
    const totalCost = safeSupply + safeShipping;
    const targetRatio = (parseFiniteNumber(targetMarginRate) ?? 1) / 100;
    const recommendedSalePrice = totalCost > 0 ? roundUpToHundred(totalCost / (1 - targetRatio)) : 0;
    const manualSalePrice = parseFiniteNumber(salePrice) ?? 0;
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

  const applyRecommendedPriceReason = getApplyRecommendedPriceReason(formErrors, calculated.recommendedSalePrice);
  const marginChartData = useMemo(
    () => [
      { label: '총 원가', value: calculated.totalCost, color: 'var(--sr-color-danger-muted)' },
      { label: '권장 판매가', value: calculated.recommendedSalePrice, color: 'var(--sr-color-brand)' },
      { label: '입력 판매가', value: parseFiniteNumber(salePrice) ?? 0, color: 'var(--sr-color-accent)' },
      { label: '입력 마진', value: Math.max(0, calculated.marginAmount), color: 'var(--sr-color-success)' }
    ],
    [calculated.marginAmount, calculated.recommendedSalePrice, calculated.totalCost, salePrice]
  );

  function applyRecommendedPrice() {
    if (applyRecommendedPriceReason) {
      return;
    }
    setSalePrice(String(calculated.recommendedSalePrice));
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="마진 계산">
        <div className="toolbar-title">
          <p className="eyebrow">Margin</p>
          <h1>마진 계산기</h1>
        </div>
        <div className="limit-meter">
          <span className="metric-label">
            <span>현재 마진율</span>
            <HelpTooltip contentKey="marginRate" compact />
          </span>
          <strong>{calculationReady ? formatRate(calculated.marginRate) : '-'}</strong>
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
            <div className="field">
              <div className="field-label-row">
                <label htmlFor="margin-supply-price">공급가</label>
                <HelpTooltip contentKey="supplyPrice" compact />
              </div>
              <input
                id="margin-supply-price"
                type="number"
                min="0"
                value={supplyPrice}
                onChange={(event) => setSupplyPrice(event.target.value)}
                aria-invalid={Boolean(formErrors.supplyPrice)}
                aria-describedby="margin-supply-price-message"
              />
              <FieldMessage
                id="margin-supply-price-message"
                tone={formErrors.supplyPrice ? 'error' : 'hint'}
              >
                {formErrors.supplyPrice ?? '0보다 큰 공급가를 입력하세요.'}
              </FieldMessage>
            </div>
            <div className="field">
              <div className="field-label-row">
                <label htmlFor="margin-shipping-fee">배송비</label>
                <HelpTooltip contentKey="shippingFee" compact />
              </div>
              <input
                id="margin-shipping-fee"
                type="number"
                min="0"
                value={shippingFee}
                onChange={(event) => setShippingFee(event.target.value)}
                aria-invalid={Boolean(formErrors.shippingFee)}
                aria-describedby="margin-shipping-fee-message"
              />
              <FieldMessage
                id="margin-shipping-fee-message"
                tone={formErrors.shippingFee ? 'error' : 'hint'}
              >
                {formErrors.shippingFee ?? '배송비가 없으면 0 또는 빈 값으로 둡니다.'}
              </FieldMessage>
            </div>
            <div className="field">
              <div className="field-label-row">
                <label htmlFor="margin-target-rate">목표 마진율</label>
                <HelpTooltip contentKey="targetMarginRate" compact />
              </div>
              <input
                id="margin-target-rate"
                type="number"
                min="1"
                max="90"
                step="0.1"
                value={targetMarginRate}
                onChange={(event) => setTargetMarginRate(event.target.value)}
                aria-invalid={Boolean(formErrors.targetMarginRate)}
                aria-describedby="margin-target-rate-message"
              />
              <FieldMessage
                id="margin-target-rate-message"
                tone={formErrors.targetMarginRate ? 'error' : 'hint'}
              >
                {formErrors.targetMarginRate ?? '1~90 사이로 입력하세요.'}
              </FieldMessage>
            </div>
            <div className="field">
              <div className="field-label-row">
                <label htmlFor="margin-sale-price">판매가 직접 입력</label>
                <HelpTooltip contentKey="salePrice" compact />
              </div>
              <input
                id="margin-sale-price"
                type="number"
                min="0"
                value={salePrice}
                onChange={(event) => setSalePrice(event.target.value)}
                aria-invalid={Boolean(formErrors.salePrice)}
                aria-describedby="margin-sale-price-message"
              />
              <FieldMessage
                id="margin-sale-price-message"
                tone={formErrors.salePrice ? 'error' : 'hint'}
              >
                {formErrors.salePrice ?? '0보다 큰 판매가를 입력하세요.'}
              </FieldMessage>
            </div>
          </div>
          <div className="button-row">
            <button
              type="button"
              className="primary-button"
              onClick={applyRecommendedPrice}
              disabled={Boolean(applyRecommendedPriceReason)}
            >
              권장가 적용
            </button>
            {applyRecommendedPriceReason ? (
              <p className="form-action-hint form-action-hint-error">{applyRecommendedPriceReason}</p>
            ) : null}
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
            <MetricCard
              variant="box"
              label="총 원가"
              value={calculationReady ? formatCurrency(calculated.totalCost) : '-'}
              helpKey="totalCost"
            />
            <MetricCard
              variant="box"
              label="목표 기준 권장 판매가"
              value={calculationReady ? formatCurrency(calculated.recommendedSalePrice) : '-'}
              helpKey="recommendedSalePrice"
            />
            <MetricCard
              variant="box"
              label="권장가 기준 마진"
              value={calculationReady ? formatCurrency(calculated.recommendedMarginAmount) : '-'}
              helpKey="expectedMargin"
            />
            <MetricCard
              variant="box"
              label="권장가 기준 마진율"
              value={calculationReady ? formatRate(calculated.recommendedMarginRate) : '-'}
              helpKey="marginRate"
            />
            <MetricCard
              variant="box"
              label="입력 판매가 기준 마진"
              value={calculationReady ? formatCurrency(calculated.marginAmount) : '-'}
              helpKey="expectedMargin"
            />
            <MetricCard
              variant="box"
              label="입력 판매가 기준 마진율"
              value={calculationReady ? formatRate(calculated.marginRate) : '-'}
              helpKey="marginRate"
            />
          </div>
        </section>
      </section>

      <LazyKpiBarChart
        title="판매가와 마진 비교"
        description="입력 판매가가 총 원가와 목표 기준 권장 판매가 대비 어느 위치인지 비교합니다."
        data={calculationReady ? marginChartData : []}
        valueFormatter={formatCurrency}
        helpKey="expectedMargin"
      />

      <div className="notice">
        계산 결과는 도매 CSV 후보 검토를 돕기 위한 참고값입니다. 판매나 수익을 보장하지 않습니다.
      </div>
    </div>
  );
}

function roundUpToHundred(value: number) {
  return Math.ceil(value / 100) * 100;
}

function validateMarginForm(values: {
  supplyPrice: string;
  shippingFee: string;
  targetMarginRate: string;
  salePrice: string;
}): MarginFormErrors {
  return {
    supplyPrice: validateRequiredPositiveNumber(values.supplyPrice, '공급가'),
    shippingFee: validateOptionalNonNegativeNumber(values.shippingFee, '배송비'),
    targetMarginRate: validateTargetMarginRate(values.targetMarginRate),
    salePrice: validateRequiredPositiveNumber(values.salePrice, '판매가')
  };
}

function validateRequiredPositiveNumber(value: string, label: string) {
  if (isBlank(value)) {
    return `${label}를 입력하세요.`;
  }
  const numberValue = parseFiniteNumber(value);
  if (numberValue === undefined) {
    return `${label}는 숫자로 입력하세요.`;
  }
  if (numberValue <= 0) {
    return `${label}는 0보다 커야 합니다.`;
  }
  return undefined;
}

function validateOptionalNonNegativeNumber(value: string, label: string) {
  if (isBlank(value)) {
    return undefined;
  }
  const numberValue = parseFiniteNumber(value);
  if (numberValue === undefined) {
    return `${label}는 숫자로 입력하세요.`;
  }
  if (numberValue < 0) {
    return `${label}는 0 이상이어야 합니다.`;
  }
  return undefined;
}

function validateTargetMarginRate(value: string) {
  if (isBlank(value)) {
    return '목표 마진율을 입력하세요.';
  }
  const numberValue = parseFiniteNumber(value);
  if (numberValue === undefined) {
    return '목표 마진율은 숫자로 입력하세요.';
  }
  if (numberValue < 1 || numberValue > 90) {
    return '목표 마진율은 1~90 사이여야 합니다.';
  }
  return undefined;
}

function parseOptionalNumber(value: string) {
  if (isBlank(value)) {
    return 0;
  }
  return parseFiniteNumber(value);
}

function getApplyRecommendedPriceReason(errors: MarginFormErrors, recommendedSalePrice: number) {
  const firstError = errors.supplyPrice ?? errors.shippingFee ?? errors.targetMarginRate ?? errors.salePrice;
  if (firstError) {
    return firstError;
  }
  if (recommendedSalePrice <= 0) {
    return '권장 판매가를 계산할 수 없습니다.';
  }
  return '';
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
