import { ReactNode } from 'react';

interface FieldMessageProps {
  children?: ReactNode;
  id?: string;
  tone?: 'hint' | 'error';
}

export function FieldMessage({ children, id, tone = 'hint' }: FieldMessageProps) {
  if (!children) {
    return null;
  }

  return (
    <p
      id={id}
      className={`field-message field-message-${tone}`}
      role={tone === 'error' ? 'alert' : undefined}
    >
      {children}
    </p>
  );
}
