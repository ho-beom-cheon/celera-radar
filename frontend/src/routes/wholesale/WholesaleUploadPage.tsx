import { FormEvent, useId, useMemo, useState } from 'react';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';
import {
  CsvEncoding,
  WholesaleColumnMapping,
  WholesaleFilePreviewRow,
  WholesaleUploadConfirmResult,
  WholesaleUploadPreview,
  confirmWholesaleUpload,
  previewWholesaleUpload
} from '../../api/wholesale';
import { DataTable, EmptyState, ErrorState, HelpTooltip, LoadingState, MetricCard } from '../../components/ui';
import type { HelpContentKey } from '../../lib/helpContent';

const encodingOptions: Array<{ value: CsvEncoding; label: string }> = [
  { value: 'AUTO', label: '자동 감지' },
  { value: 'UTF_8', label: 'UTF-8' },
  { value: 'CP949', label: 'CP949' }
];

const previewRowLimit = 20;

export function WholesaleUploadPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [encoding, setEncoding] = useState<CsvEncoding>('AUTO');
  const [sourceName, setSourceName] = useState('도매 업로드');
  const [preview, setPreview] = useState<WholesaleUploadPreview | null>(null);
  const [mapping, setMapping] = useState<WholesaleColumnMapping>({
    productName: '',
    supplyPrice: ''
  });
  const [confirmResult, setConfirmResult] = useState<WholesaleUploadConfirmResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const headers = preview?.preview.headers ?? [];
  const previewRows = useMemo(() => preview?.preview.rows.slice(0, previewRowLimit) ?? [], [preview]);
  const canConfirm = preview !== null && mapping.productName !== '' && mapping.supplyPrice !== '' && !loading;

  async function handlePreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile || loading) {
      return;
    }
    setLoading(true);
    setMessage('');
    setError('');
    setPreview(null);
    setConfirmResult(null);
    try {
      const response = await previewWholesaleUpload(selectedFile, encoding, sourceName);
      setPreview(response);
      setMapping(defaultMapping(response.preview.headers));
      setMessage(`${response.preview.originalFilename} preview가 준비됐습니다.`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function handleConfirm() {
    if (!preview || !canConfirm) {
      return;
    }
    setLoading(true);
    setMessage('');
    setError('');
    setConfirmResult(null);
    try {
      const response = await confirmWholesaleUpload(preview.uploadId, compactMapping(mapping));
      setConfirmResult(response);
      setMessage(`저장 완료: 정상 ${response.successCount}건, 오류 ${response.failureCount}건`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  function updateMapping(field: keyof WholesaleColumnMapping, value: string) {
    setMapping((current) => ({
      ...current,
      [field]: value
    }));
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="도매 업로드 작업">
        <div className="toolbar-title">
          <p className="eyebrow">Wholesale Upload</p>
          <h1>도매 CSV/XLSX 업로드</h1>
        </div>
        <div className="limit-meter">
          <span className="metric-label">
            <span>Preview rows</span>
            <HelpTooltip contentKey="uploadFileFormat" compact />
          </span>
          <strong>{preview ? `${preview.preview.rowCount}행` : '-'}</strong>
        </div>
      </section>

      <section className="workflow-grid">
        <form className="panel keyword-form-panel" onSubmit={handlePreview}>
          <div className="panel-header">
            <div>
              <h2>1. 파일 preview</h2>
              <p className="muted">CSV 또는 XLSX 파일을 업로드해 header와 일부 row를 먼저 확인합니다.</p>
            </div>
          </div>
          <div className="form-grid">
            <div className="field">
              <div className="field-label-row">
                <label htmlFor="wholesale-upload-file">파일</label>
                <HelpTooltip contentKey="uploadFileFormat" compact />
              </div>
              <input
                id="wholesale-upload-file"
                type="file"
                accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
                disabled={!getAccessToken() || loading}
              />
            </div>
            <div className="field">
              <div className="field-label-row">
                <label htmlFor="wholesale-upload-encoding">인코딩</label>
                <HelpTooltip contentKey="uploadEncoding" compact />
              </div>
              <select
                id="wholesale-upload-encoding"
                value={encoding}
                onChange={(event) => setEncoding(event.target.value as CsvEncoding)}
                disabled={loading}
              >
                {encodingOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <label className="field">
              <span>도매처명</span>
              <input value={sourceName} onChange={(event) => setSourceName(event.target.value)} disabled={loading} />
            </label>
          </div>
          <div className="button-row">
            <button type="submit" className="primary-button" disabled={!getAccessToken() || !selectedFile || loading}>
              Preview
            </button>
          </div>
        </form>

        <section className="panel keyword-form-panel">
          <div className="panel-header">
            <div>
              <h2>2. 컬럼 매핑</h2>
              <p className="muted">필수 컬럼은 상품명과 공급가입니다. 선택 컬럼은 비워둘 수 있습니다.</p>
            </div>
          </div>
          <div className="form-grid form-grid-two">
            <ColumnSelect
              label="상품명"
              value={mapping.productName}
              columns={headers}
              onChange={(value) => updateMapping('productName', value)}
              helpKey="requiredColumns"
              required
            />
            <ColumnSelect
              label="공급가"
              value={mapping.supplyPrice}
              columns={headers}
              onChange={(value) => updateMapping('supplyPrice', value)}
              helpKey="requiredColumns"
              required
            />
            <ColumnSelect
              label="배송비"
              value={mapping.shippingFee ?? ''}
              columns={headers}
              onChange={(value) => updateMapping('shippingFee', value)}
              helpKey="shippingFee"
            />
            <ColumnSelect label="이미지 URL" value={mapping.imageUrl ?? ''} columns={headers} onChange={(value) => updateMapping('imageUrl', value)} />
            <ColumnSelect label="상품 URL" value={mapping.productUrl ?? ''} columns={headers} onChange={(value) => updateMapping('productUrl', value)} />
            <ColumnSelect label="카테고리" value={mapping.category ?? ''} columns={headers} onChange={(value) => updateMapping('category', value)} />
          </div>
          <div className="button-row">
            <button type="button" className="primary-button" onClick={() => void handleConfirm()} disabled={!canConfirm}>
              저장 확정
            </button>
          </div>
        </section>
      </section>

      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <ErrorState>{error}</ErrorState> : null}

      <section className="panel keywords-table-panel">
        <div className="panel-header table-header">
          <div>
            <h2>Preview</h2>
            <p className="muted">
              {preview
                ? `${preview.preview.originalFilename} · ${preview.fileType} · ${preview.detectedEncoding} · ${preview.preview.rowCount}행`
                : '파일을 선택하고 preview를 실행하면 header와 row가 표시됩니다.'}
            </p>
          </div>
          {preview ? <span className="inline-meta">최대 {previewRowLimit}행 표시</span> : null}
        </div>
        <PreviewTable headers={headers} rows={previewRows} />
      </section>

      {confirmResult ? (
        <section className="panel keywords-table-panel">
          <div className="panel-header table-header">
            <div>
              <h2>저장 결과</h2>
              <p className="muted">정상 저장 row와 실패 row를 구분해 확인합니다.</p>
            </div>
            <div className="result-grid upload-result-grid">
              <MetricCard variant="box" label="정상" value={confirmResult.successCount} helpKey="requiredColumns" />
              <MetricCard variant="box" label="오류" value={confirmResult.failureCount} helpKey="errorRows" />
            </div>
          </div>
          {confirmResult.failureReasons.length > 0 ? (
            <DataTable className="upload-failure-table">
                <thead>
                  <tr>
                    <th>행</th>
                    <th>사유</th>
                  </tr>
                </thead>
                <tbody>
                  {confirmResult.failureReasons.map((failure) => (
                    <tr key={`${failure.rowNo}-${failure.message}`}>
                      <td>{failure.rowNo}</td>
                      <td>{failure.message}</td>
                    </tr>
                  ))}
                </tbody>
            </DataTable>
          ) : (
            <EmptyState>실패 row가 없습니다.</EmptyState>
          )}
        </section>
      ) : null}

      {loading ? <LoadingState>처리 중입니다.</LoadingState> : null}
      {!loading && !getAccessToken() ? <EmptyState>계정 연결 후 도매 파일을 업로드할 수 있습니다.</EmptyState> : null}
    </div>
  );
}

interface ColumnSelectProps {
  label: string;
  value: string;
  columns: string[];
  helpKey?: HelpContentKey;
  required?: boolean;
  onChange: (value: string) => void;
}

function ColumnSelect({ label, value, columns, helpKey, required, onChange }: ColumnSelectProps) {
  const selectId = useId();

  return (
    <div className="field">
      <div className="field-label-row">
        <label htmlFor={selectId}>
          {label}
          {required ? ' *' : ''}
        </label>
        {helpKey ? <HelpTooltip contentKey={helpKey} compact /> : null}
      </div>
      <select id={selectId} value={value} onChange={(event) => onChange(event.target.value)} disabled={columns.length === 0}>
        <option value="">선택 안 함</option>
        {columns.map((column) => (
          <option key={column} value={column}>
            {column}
          </option>
        ))}
      </select>
    </div>
  );
}

function PreviewTable({ headers, rows }: { headers: string[]; rows: WholesaleFilePreviewRow[] }) {
  if (headers.length === 0) {
    return <EmptyState>Preview 데이터가 없습니다.</EmptyState>;
  }
  return (
    <DataTable className="upload-preview-table">
        <thead>
          <tr>
            <th>행</th>
            {headers.map((header) => (
              <th key={header}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.rowNo}>
              <td>{row.rowNo}</td>
              {headers.map((header, index) => {
                const cell = row.cells[index];
                return <td key={`${row.rowNo}-${header}`}>{cell?.rawValue || '-'}</td>;
              })}
            </tr>
          ))}
        </tbody>
    </DataTable>
  );
}

function defaultMapping(headers: string[]): WholesaleColumnMapping {
  const mapping: WholesaleColumnMapping = {
    productName: '',
    supplyPrice: ''
  };
  for (const header of headers) {
    const normalized = header.toLowerCase().replace(/\s|_/g, '');
    if (!mapping.productName && /(product|상품|name|품명|상품명)/.test(normalized)) {
      mapping.productName = header;
    }
    if (!mapping.supplyPrice && /(supply|공급|cost|price|가격|원가)/.test(normalized)) {
      mapping.supplyPrice = header;
    }
    if (!mapping.shippingFee && /(shipping|배송|delivery|택배)/.test(normalized)) {
      mapping.shippingFee = header;
    }
    if (!mapping.imageUrl && /(image|이미지|img|thumbnail)/.test(normalized)) {
      mapping.imageUrl = header;
    }
    if (!mapping.productUrl && /(url|link|링크|주소)/.test(normalized)) {
      mapping.productUrl = header;
    }
    if (!mapping.category && /(category|카테고리|분류)/.test(normalized)) {
      mapping.category = header;
    }
  }
  return mapping;
}

function compactMapping(mapping: WholesaleColumnMapping): WholesaleColumnMapping {
  return {
    productName: mapping.productName,
    supplyPrice: mapping.supplyPrice,
    shippingFee: emptyToUndefined(mapping.shippingFee),
    imageUrl: emptyToUndefined(mapping.imageUrl),
    productUrl: emptyToUndefined(mapping.productUrl),
    category: emptyToUndefined(mapping.category)
  };
}

function emptyToUndefined(value?: string) {
  return value?.trim() ? value.trim() : undefined;
}

function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '요청을 처리하지 못했습니다.';
}
