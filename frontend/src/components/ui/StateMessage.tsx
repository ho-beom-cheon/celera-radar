import type { ReactNode } from 'react';

interface StateMessageProps {
  children: ReactNode;
}

export function LoadingState({ children }: StateMessageProps) {
  return <div className="state-row state-loading">{children}</div>;
}

export function EmptyState({ children }: StateMessageProps) {
  return <div className="state-row state-empty">{children}</div>;
}

export function ErrorState({ children }: StateMessageProps) {
  return <div className="notice notice-error">{children}</div>;
}
