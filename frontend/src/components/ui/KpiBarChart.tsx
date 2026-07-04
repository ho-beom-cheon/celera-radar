import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { HelpContentKey } from '../../lib/helpContent';
import { HelpTooltip } from './HelpTooltip';

export interface KpiBarChartItem {
  label: string;
  value: number;
  color?: string;
}

export interface KpiBarChartProps {
  title: string;
  description?: string;
  data: KpiBarChartItem[];
  helpKey?: HelpContentKey;
  valueFormatter?: (value: number) => string;
  maxDomain?: number;
}

const defaultFormatter = (value: number) => String(value);
const defaultColor = 'var(--sr-color-brand)';

export function KpiBarChart({
  title,
  description,
  data,
  helpKey,
  valueFormatter = defaultFormatter,
  maxDomain
}: KpiBarChartProps) {
  const hasData = data.some((item) => item.value > 0);
  const domainMax = maxDomain ?? Math.max(1, ...data.map((item) => item.value));

  return (
    <section className="chart-panel" aria-label={title}>
      <div className="chart-panel-header">
        <div>
          <h2>
            <span className="table-heading-help">
              <span>{title}</span>
              {helpKey ? <HelpTooltip contentKey={helpKey} compact /> : null}
            </span>
          </h2>
          {description ? <p className="muted">{description}</p> : null}
        </div>
      </div>
      {hasData ? (
        <>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="var(--sr-color-border-subtle)" vertical={false} />
                <XAxis dataKey="label" tickLine={false} axisLine={false} tickMargin={8} />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  width={52}
                  domain={[0, domainMax]}
                  tickFormatter={valueFormatter}
                />
                <Tooltip
                  cursor={{ fill: 'var(--sr-color-surface-subtle)' }}
                  formatter={(value) => valueFormatter(Number(value))}
                  labelClassName="chart-tooltip-label"
                  wrapperClassName="chart-tooltip"
                />
                <Bar dataKey="value" radius={[6, 6, 0, 0]} barSize={32}>
                  {data.map((entry) => (
                    <Cell key={entry.label} fill={entry.color ?? defaultColor} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <ul className="chart-value-list">
            {data.map((item) => (
              <li key={item.label}>
                <span className="chart-legend-swatch" style={{ background: item.color ?? defaultColor }} />
                <span>{item.label}</span>
                <strong>{valueFormatter(item.value)}</strong>
              </li>
            ))}
          </ul>
        </>
      ) : (
        <div className="chart-empty">표시할 KPI 데이터가 없습니다.</div>
      )}
    </section>
  );
}
