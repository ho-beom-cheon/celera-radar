import type { ReactNode } from 'react';

interface DataTableProps {
  children: ReactNode;
  className?: string;
}

export function DataTable({ children, className }: DataTableProps) {
  const tableClassName = ['data-table', className].filter(Boolean).join(' ');

  return (
    <div className="table-wrap">
      <table className={tableClassName}>{children}</table>
    </div>
  );
}
