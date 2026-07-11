import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

export interface KpiLineChartItem {
  label: string;
  value: number;
}

export interface KpiLineChartProps {
  title: string;
  description?: string;
  data: KpiLineChartItem[];
  valueFormatter?: (value: number) => string;
}

const defaultFormatter = (value: number) => String(value);

export function KpiLineChart({
  title,
  description,
  data,
  valueFormatter = defaultFormatter
}: KpiLineChartProps) {
  return (
    <section className="chart-panel" aria-label={title}>
      <div className="chart-panel-header">
        <div>
          <h2>{title}</h2>
          {description ? <p className="muted">{description}</p> : null}
        </div>
      </div>
      {data.length > 0 ? (
        <>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="var(--sr-color-border-subtle)" vertical={false} />
                <XAxis dataKey="label" tickLine={false} axisLine={false} tickMargin={8} minTickGap={24} />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  width={44}
                  domain={[0, 100]}
                  tickFormatter={valueFormatter}
                />
                <Tooltip
                  formatter={(value) => valueFormatter(Number(value))}
                  labelClassName="chart-tooltip-label"
                  wrapperClassName="chart-tooltip"
                />
                <Line
                  type="monotone"
                  dataKey="value"
                  name="상대 지수"
                  stroke="var(--sr-color-brand)"
                  strokeWidth={3}
                  dot={false}
                  activeDot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <p className="sr-only">
            시작 {data[0].label} {valueFormatter(data[0].value)}, 종료 {data[data.length - 1].label}{' '}
            {valueFormatter(data[data.length - 1].value)}
          </p>
        </>
      ) : (
        <div className="chart-empty">표시할 트렌드 데이터가 없습니다.</div>
      )}
    </section>
  );
}
