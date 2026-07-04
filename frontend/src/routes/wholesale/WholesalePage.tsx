import { FormEvent, useMemo, useState } from 'react';
import { ApiRequestError, getAccessToken } from '../../api/httpClient';
import {
  CsvEncoding,
  WholesaleFile,
  WholesaleProductRow,
  generateWholesaleCandidates,
  listWholesaleRows,
  parseWholesaleFile,
  updateWholesaleColumnMapping,
  uploadWholesaleFile
} from '../../api/wholesale';
import { DataTable, EmptyState, ErrorState, LoadingState } from '../../components/ui';

const encodingOptions: Array<{ value: CsvEncoding; label: string }> = [
  { value: 'AUTO', label: '자동 감지' },
  { value: 'UTF_8', label: 'UTF-8' },
  { value: 'CP949', label: 'CP949' }
];

export function WholesalePage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [encoding, setEncoding] = useState<CsvEncoding>('AUTO');
  const [sourceName, setSourceName] = useState('도매 CSV');
  const [wholesaleFile, setWholesaleFile] = useState<WholesaleFile | null>(null);
  const [productName, setProductName] = useState('');
  const [supplyPrice, setSupplyPrice] = useState('');
  const [shippingFee, setShippingFee] = useState('');
  const [category, setCategory] = useState('');
  const [productUrl, setProductUrl] = useState('');
  const [rows, setRows] = useState<WholesaleProductRow[]>([]);
  const [parsedCount, setParsedCount] = useState<number | null>(null);
  const [invalidCount, setInvalidCount] = useState<number | null>(null);
  const [generatedCount, setGeneratedCount] = useState<number | null>(null);
  const [skippedCount, setSkippedCount] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const detectedColumns = wholesaleFile?.detectedColumns ?? [];
  const canMap = wholesaleFile !== null && productName !== '' && supplyPrice !== '';
  const canParse = wholesaleFile !== null && wholesaleFile.status !== 'UPLOADED';
  const parsedRows = useMemo(() => rows.filter((row) => row.parseStatus === 'PARSED').length, [rows]);

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile || loading) {
      return;
    }
    setLoading(true);
    resetResultState();
    try {
      const response = await uploadWholesaleFile(selectedFile, encoding, sourceName);
      setWholesaleFile(response);
      setMessage(`${response.originalFilename} 업로드가 완료되었습니다.`);
      applyDefaultMapping(response.detectedColumns);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function handleMapping() {
    if (!wholesaleFile || !canMap || loading) {
      return;
    }
    setLoading(true);
    setMessage('');
    setError('');
    try {
      const response = await updateWholesaleColumnMapping(wholesaleFile.fileId, {
        productName,
        supplyPrice,
        shippingFee: emptyToUndefined(shippingFee),
        category: emptyToUndefined(category),
        productUrl: emptyToUndefined(productUrl)
      });
      setWholesaleFile(response);
      setMessage('컬럼 매핑이 저장되었습니다.');
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function handleParse() {
    if (!wholesaleFile || loading) {
      return;
    }
    setLoading(true);
    setMessage('');
    setError('');
    try {
      const parseResult = await parseWholesaleFile(wholesaleFile.fileId);
      const rowPage = await listWholesaleRows(wholesaleFile.fileId, 0, 50);
      setParsedCount(parseResult.parsedCount);
      setInvalidCount(parseResult.invalidCount);
      setRows(rowPage.items);
      setMessage(`파싱 완료: 정상 ${parseResult.parsedCount}건, 오류 ${parseResult.invalidCount}건`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerateCandidates() {
    if (!wholesaleFile || loading) {
      return;
    }
    setLoading(true);
    setMessage('');
    setError('');
    try {
      const response = await generateWholesaleCandidates(wholesaleFile.fileId);
      setGeneratedCount(response.generatedCount);
      setSkippedCount(response.skippedCount);
      setMessage(`후보 생성 완료: 신규 ${response.generatedCount}건, 중복 제외 ${response.skippedCount}건`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  function applyDefaultMapping(columns: string[]) {
    applyDefaultMappingToColumns(columns, (mapping) => {
      setProductName(mapping.productName ?? '');
      setSupplyPrice(mapping.supplyPrice ?? '');
      setShippingFee(mapping.shippingFee ?? '');
      setCategory(mapping.category ?? '');
      setProductUrl(mapping.productUrl ?? '');
    });
  }

  function resetResultState() {
    setMessage('');
    setError('');
    setRows([]);
    setParsedCount(null);
    setInvalidCount(null);
    setGeneratedCount(null);
    setSkippedCount(null);
  }

  return (
    <div className="keywords-page">
      <section className="toolbar-row" aria-label="도매 CSV 작업">
        <div className="toolbar-title">
          <p className="eyebrow">Wholesale CSV</p>
          <h1>도매 CSV</h1>
        </div>
        <div className="limit-meter">
          <span>파싱 상품</span>
          <strong>{parsedRows}건</strong>
        </div>
      </section>

      <section className="workflow-grid">
        <form className="panel keyword-form-panel" onSubmit={handleUpload}>
          <div className="panel-header">
            <div>
              <h2>1. 파일 업로드</h2>
              <p className="muted">MVP에서는 `.csv` 파일만 지원합니다.</p>
            </div>
          </div>
          <div className="form-grid">
            <label className="field">
              <span>CSV 파일</span>
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
                disabled={!getAccessToken()}
              />
            </label>
            <label className="field">
              <span>인코딩</span>
              <select value={encoding} onChange={(event) => setEncoding(event.target.value as CsvEncoding)}>
                {encodingOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>출처 이름</span>
              <input value={sourceName} onChange={(event) => setSourceName(event.target.value)} />
            </label>
          </div>
          <div className="button-row">
            <button type="submit" className="primary-button" disabled={!getAccessToken() || !selectedFile || loading}>
              업로드
            </button>
          </div>
        </form>

        <section className="panel keyword-form-panel">
          <div className="panel-header">
            <div>
              <h2>2. 컬럼 매핑</h2>
              <p className="muted">감지된 CSV 헤더를 상품 필드에 연결합니다.</p>
            </div>
          </div>
          <div className="form-grid form-grid-two">
            <ColumnSelect label="상품명" value={productName} columns={detectedColumns} onChange={setProductName} required />
            <ColumnSelect label="공급가" value={supplyPrice} columns={detectedColumns} onChange={setSupplyPrice} required />
            <ColumnSelect label="배송비" value={shippingFee} columns={detectedColumns} onChange={setShippingFee} />
            <ColumnSelect label="카테고리" value={category} columns={detectedColumns} onChange={setCategory} />
            <ColumnSelect label="상품 URL" value={productUrl} columns={detectedColumns} onChange={setProductUrl} />
          </div>
          <div className="button-row">
            <button type="button" className="primary-button" onClick={() => void handleMapping()} disabled={!canMap || loading}>
              매핑 저장
            </button>
            <button type="button" className="secondary-button" onClick={() => void handleParse()} disabled={!canParse || loading}>
              파싱 실행
            </button>
          </div>
        </section>
      </section>

      {message ? <div className="notice notice-success">{message}</div> : null}
      {error ? <ErrorState>{error}</ErrorState> : null}

      <section className="panel keywords-table-panel">
        <div className="panel-header table-header">
          <div>
            <h2>파싱 결과</h2>
            <p className="muted">
              {wholesaleFile
                ? `${wholesaleFile.originalFilename} · ${wholesaleFile.rowCount}행 · ${wholesaleFile.detectedEncoding}`
                : '파일을 업로드하면 감지된 컬럼과 파싱 결과가 표시됩니다.'}
            </p>
          </div>
          <div className="button-row table-button-row">
            {parsedCount !== null ? (
              <span className="inline-meta">
                정상 {parsedCount} · 오류 {invalidCount}
              </span>
            ) : null}
            <button
              type="button"
              className="primary-button"
              onClick={() => void handleGenerateCandidates()}
              disabled={!wholesaleFile || parsedRows === 0 || loading}
            >
              후보 생성
            </button>
          </div>
        </div>

        <DataTable>
            <thead>
              <tr>
                <th>행</th>
                <th>상품명</th>
                <th>공급가</th>
                <th>배송비</th>
                <th>카테고리</th>
                <th>상태</th>
                <th>오류</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.rowNo}</td>
                  <td className="strong-cell">{row.productName ?? '-'}</td>
                  <td>{formatCurrency(row.supplyPrice)}</td>
                  <td>{formatCurrency(row.shippingFee)}</td>
                  <td>{row.sourceCategory ?? '-'}</td>
                  <td>{row.parseStatus === 'PARSED' ? '정상' : '오류'}</td>
                  <td>{row.errorMessage ?? '-'}</td>
                </tr>
              ))}
            </tbody>
        </DataTable>

        {loading ? <LoadingState>처리 중입니다.</LoadingState> : null}
        {!loading && !getAccessToken() ? <EmptyState>키워드 레이더에서 계정 연결 후 CSV를 업로드할 수 있습니다.</EmptyState> : null}
        {!loading && getAccessToken() && rows.length === 0 ? (
          <EmptyState>업로드 후 컬럼 매핑과 파싱을 실행하세요.</EmptyState>
        ) : null}
      </section>

      {generatedCount !== null ? (
        <div className="notice notice-success">
          생성된 후보 {generatedCount}건, 중복으로 건너뛴 상품 {skippedCount}건입니다.
          <a className="inline-link" href="/candidates">
            후보 목록 보기
          </a>
        </div>
      ) : null}
    </div>
  );
}

interface ColumnSelectProps {
  label: string;
  value: string;
  columns: string[];
  required?: boolean;
  onChange: (value: string) => void;
}

function ColumnSelect({ label, value, columns, required, onChange }: ColumnSelectProps) {
  return (
    <label className="field">
      <span>
        {label}
        {required ? ' *' : ''}
      </span>
      <select value={value} onChange={(event) => onChange(event.target.value)} disabled={columns.length === 0}>
        <option value="">선택 안 함</option>
        {columns.map((column) => (
          <option key={column} value={column}>
            {column}
          </option>
        ))}
      </select>
    </label>
  );
}

function applyDefaultMappingToColumns(columns: string[], setter: (mapping: Record<string, string>) => void) {
  const mapping: Record<string, string> = {};
  for (const column of columns) {
    const normalized = column.toLowerCase().replace(/\s|_/g, '');
    if (!mapping.productName && /(product|상품|name|품명)/.test(normalized)) {
      mapping.productName = column;
    }
    if (!mapping.supplyPrice && /(supply|공급|cost|price|가격|원가)/.test(normalized)) {
      mapping.supplyPrice = column;
    }
    if (!mapping.shippingFee && /(shipping|배송|delivery|택배)/.test(normalized)) {
      mapping.shippingFee = column;
    }
    if (!mapping.category && /(category|카테고리|분류)/.test(normalized)) {
      mapping.category = column;
    }
    if (!mapping.productUrl && /(url|link|링크|주소)/.test(normalized)) {
      mapping.productUrl = column;
    }
  }
  setter(mapping);
}

function emptyToUndefined(value: string) {
  return value.trim() === '' ? undefined : value;
}

function formatCurrency(value: number | null) {
  if (value === null) {
    return '-';
  }
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0
  }).format(value);
}

function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '요청을 처리하지 못했습니다.';
}
