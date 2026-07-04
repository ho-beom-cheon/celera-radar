import { apiRequest } from './httpClient';
import { PageResponse } from './keywords';

export interface SmartStoreProduct {
  productId: number;
  connectionId: number;
  sourceProductId: string;
  originProductNo: string | null;
  productName: string;
  salePrice: number;
  saleStatus: string;
  imageUrl: string | null;
  categoryName: string | null;
  lastSyncedAt: string;
}

export interface StoreProductCost {
  id: number;
  storeProductId: number;
  wholesaleProductId: number | null;
  wholesaleProductName: string | null;
  purchaseCost: number;
  shippingFee: number;
  packagingFee: number;
  extraCost: number;
  platformFeeRate: number;
  targetMarginRate: number;
  salePrice: number;
  totalCost: number;
  expectedProfit: number;
  expectedMarginRate: number;
  recommendedSalePrice: number;
  memo: string | null;
  createdAt: string;
  updatedAt: string;
}

export function listSmartStoreProducts(page = 0, size = 100) {
  return apiRequest<PageResponse<SmartStoreProduct>>(`/smartstore/products?page=${page}&size=${size}`);
}

export function getStoreProductCost(productId: number) {
  return apiRequest<StoreProductCost>(`/smartstore/products/${productId}/cost`);
}
