import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';

export interface KpiDonutChartItem {
  label: string;
  value: number;
  color: string;
}

export interface KpiDonutChartProps {
  title: string;
  description?: string;
  data: KpiDonutChartItem[];
}

export function KpiDonutChart({ title, description, data }: KpiDonutChartProps) {
  const total = data.reduce((sum, item) => sum + item.value, 0);

  return (
    <section className="chart-panel" aria-label={title}>
      <div className="chart-panel-header">
        <div>
          <h2>{title}</h2>
          {description ? <p className="muted">{description}</p> : null}
        </div>
        <strong className="chart-total">{total}</strong>
      </div>
      {total > 0 ? (
        <div className="donut-chart-layout">
          <div className="donut-chart-container">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={data} dataKey="value" nameKey="label" innerRadius={54} outerRadius={82} paddingAngle={3}>
                  {data.map((entry) => (
                    <Cell key={entry.label} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value) => `${Number(value).toLocaleString('ko-KR')}개`}
                  labelClassName="chart-tooltip-label"
                  wrapperClassName="chart-tooltip"
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="chart-legend">
            {data.map((item) => (
              <li key={item.label}>
                <span className="chart-legend-swatch" style={{ background: item.color }} />
                <span>{item.label}</span>
                <strong>{item.value.toLocaleString('ko-KR')}</strong>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <div className="chart-empty">동기화된 상품 데이터가 없습니다.</div>
      )}
    </section>
  );
}
