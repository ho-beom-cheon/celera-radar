import { lazy, Suspense } from 'react';
import type { KpiBarChartProps } from './KpiBarChart';
import type { KpiDonutChartProps } from './KpiDonutChart';

const KpiBarChart = lazy(() => import('./KpiBarChart').then((module) => ({ default: module.KpiBarChart })));
const KpiDonutChart = lazy(() => import('./KpiDonutChart').then((module) => ({ default: module.KpiDonutChart })));

export function LazyKpiBarChart(props: KpiBarChartProps) {
  return (
    <Suspense fallback={<ChartLoadingFallback title={props.title} />}>
      <KpiBarChart {...props} />
    </Suspense>
  );
}

export function LazyKpiDonutChart(props: KpiDonutChartProps) {
  return (
    <Suspense fallback={<ChartLoadingFallback title={props.title} />}>
      <KpiDonutChart {...props} />
    </Suspense>
  );
}

function ChartLoadingFallback({ title }: { title: string }) {
  return (
    <section className="chart-panel chart-loading" aria-label={title} aria-busy="true">
      <span>차트를 불러오는 중입니다.</span>
    </section>
  );
}
