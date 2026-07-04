import type { ReactNode } from 'react';
import type { HelpContentKey } from '../../lib/helpContent';
import { HelpTooltip } from './HelpTooltip';

interface MetricCardProps {
  label: ReactNode;
  value: ReactNode;
  variant?: 'summary' | 'box';
  valueClassName?: string;
  helpKey?: HelpContentKey;
}

export function MetricCard({ label, value, variant = 'summary', valueClassName, helpKey }: MetricCardProps) {
  const labelNode = (
    <span className="metric-label">
      <span>{label}</span>
      {helpKey ? <HelpTooltip contentKey={helpKey} compact /> : null}
    </span>
  );

  if (variant === 'box') {
    return (
      <div className="metric-box">
        {labelNode}
        <strong className={valueClassName}>{value}</strong>
      </div>
    );
  }

  return (
    <article className="summary-card">
      {labelNode}
      <strong className={valueClassName}>{value}</strong>
    </article>
  );
}
