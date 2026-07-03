import type { ReactNode } from 'react';

interface StatusBadgeProps {
  children: ReactNode;
  tone?: string;
  className?: string;
}

export function StatusBadge({ children, tone, className }: StatusBadgeProps) {
  const statusClassName = tone ? `status-${tone.toLowerCase()}` : '';
  const badgeClassName = ['status-badge', statusClassName, className].filter(Boolean).join(' ');

  return <span className={badgeClassName}>{children}</span>;
}
