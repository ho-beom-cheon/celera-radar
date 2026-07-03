import type { ReactNode } from 'react';

interface MetricCardProps {
  label: ReactNode;
  value: ReactNode;
  variant?: 'summary' | 'box';
  valueClassName?: string;
}

export function MetricCard({ label, value, variant = 'summary', valueClassName }: MetricCardProps) {
  if (variant === 'box') {
    return (
      <div className="metric-box">
        <span>{label}</span>
        <strong className={valueClassName}>{value}</strong>
      </div>
    );
  }

  return (
    <article className="summary-card">
      <span>{label}</span>
      <strong className={valueClassName}>{value}</strong>
    </article>
  );
}
