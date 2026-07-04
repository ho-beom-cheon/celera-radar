import { apiRequest } from './httpClient';
import { PageResponse } from './keywords';

export type CsvEncoding = 'AUTO' | 'UTF_8' | 'CP949';
export type WholesaleFileStatus = 'UPLOADED' | 'MAPPED' | 'PARSED' | 'FAILED';
export type WholesaleProductParseStatus = 'PARSED' | 'INVALID';

export interface WholesaleFile {
  fileId: number;
  status: WholesaleFileStatus;
  rowCount: number;
  detectedColumns: string[];
  sourceName: string | null;
  originalFilename: string;
  detectedEncoding: CsvEncoding;
  createdAt: string;
}

export interface WholesaleColumnMapping {
  productName: string;
  supplyPrice: string;
  shippingFee?: string;
  imageUrl?: string;
  category?: string;
  productUrl?: string;
}

export interface WholesaleFilePreviewCell {
  header: string;
  rawValue: string;
  decimalValue: number | null;
  longValue: number | null;
}

export interface WholesaleFilePreviewRow {
  rowNo: number;
  cells: WholesaleFilePreviewCell[];
}

export interface WholesaleFilePreview {
  originalFilename: string;
  fileType: 'CSV' | 'XLSX';
  headers: string[];
  rows: WholesaleFilePreviewRow[];
  rowCount: number;
}

export interface WholesaleUploadPreview {
  uploadId: number;
  status: WholesaleFileStatus;
  fileType: 'CSV' | 'XLSX';
  detectedEncoding: CsvEncoding;
  preview: WholesaleFilePreview;
}

export interface WholesaleUploadConfirmResult {
  uploadId: number;
  successCount: number;
  failureCount: number;
  failureReasons: Array<{
    rowNo: number;
    message: string;
  }>;
}

export interface WholesaleParseResult {
  fileId: number;
  parsedCount: number;
  invalidCount: number;
}

export interface WholesaleProductRow {
  id: number;
  rowNo: number;
  productName: string | null;
  supplyPrice: number | null;
  shippingFee: number | null;
  sourceCategory: string | null;
  productUrl: string | null;
  parseStatus: WholesaleProductParseStatus;
  errorMessage: string | null;
}

export interface WholesaleCandidateGenerationResult {
  fileId: number;
  generatedCount: number;
  skippedCount: number;
}

export function uploadWholesaleFile(file: File, encoding: CsvEncoding, sourceName: string) {
  const body = new FormData();
  body.append('file', file);
  body.append('encoding', encoding);
  if (sourceName.trim()) {
    body.append('sourceName', sourceName.trim());
  }
  return apiRequest<WholesaleFile>('/wholesale-files', {
    method: 'POST',
    body
  });
}

export function previewWholesaleUpload(file: File, encoding: CsvEncoding, sourceName: string) {
  const body = new FormData();
  body.append('file', file);
  body.append('encoding', encoding);
  if (sourceName.trim()) {
    body.append('sourceName', sourceName.trim());
  }
  return apiRequest<WholesaleUploadPreview>('/wholesale-uploads/preview', {
    method: 'POST',
    body
  });
}

export function confirmWholesaleUpload(uploadId: number, mapping: WholesaleColumnMapping) {
  return apiRequest<WholesaleUploadConfirmResult>(`/wholesale-uploads/${uploadId}/confirm`, {
    method: 'POST',
    body: { mapping }
  });
}

export function updateWholesaleColumnMapping(fileId: number, mapping: WholesaleColumnMapping) {
  return apiRequest<WholesaleFile>(`/wholesale-files/${fileId}/column-mapping`, {
    method: 'POST',
    body: { mapping }
  });
}

export function parseWholesaleFile(fileId: number) {
  return apiRequest<WholesaleParseResult>(`/wholesale-files/${fileId}/parse`, {
    method: 'POST'
  });
}

export function listWholesaleRows(fileId: number, page = 0, size = 20) {
  return apiRequest<PageResponse<WholesaleProductRow>>(
    `/wholesale-files/${fileId}/rows?page=${page}&size=${size}`
  );
}

export function generateWholesaleCandidates(fileId: number) {
  return apiRequest<WholesaleCandidateGenerationResult>(`/wholesale-files/${fileId}/candidates`, {
    method: 'POST'
  });
}
