import type { ReactNode } from 'react';

interface DataTableProps {
  children: ReactNode;
  className?: string;
}

interface DataTableStateRowProps {
  children: ReactNode;
  className?: string;
  colSpan: number;
}

export function DataTable({ children, className }: DataTableProps) {
  const tableClassName = ['data-table', className].filter(Boolean).join(' ');

  return (
    <div className="table-wrap">
      <table className={tableClassName}>{children}</table>
    </div>
  );
}

export function DataTableStateRow({ children, className, colSpan }: DataTableStateRowProps) {
  const rowClassName = ['data-table-state-row', className].filter(Boolean).join(' ');

  return (
    <tr className={rowClassName}>
      <td colSpan={colSpan}>{children}</td>
    </tr>
  );
}
