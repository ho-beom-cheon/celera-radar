import { useState } from 'react';
import { ShoppingSnapshotItem } from '../../api/keywords';
import { safeExternalUrl } from '../../security/safeExternalUrl';

interface ProductCardProps {
  item: ShoppingSnapshotItem;
}

export function ProductCard({ item }: ProductCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const title = stripTags(item.title);
  const category = categoryPath(item);
  const imageUrl = safeExternalUrl(item.imageUrl);
  const productUrl = safeExternalUrl(item.productUrl);

  return (
    <article className="product-card">
      <div className="product-card-image">
        {imageUrl && !imageFailed ? (
          <img
            src={imageUrl}
            alt=""
            loading="lazy"
            referrerPolicy="no-referrer"
            onError={() => setImageFailed(true)}
          />
        ) : (
          <span>No image</span>
        )}
      </div>
      <div className="product-card-body">
        <div className="product-card-rank">#{item.rankNo}</div>
        <h3>{title}</h3>
        <dl className="product-card-meta">
          <div>
            <dt>최저가</dt>
            <dd>{formatCurrency(item.lowPrice)}</dd>
          </div>
          <div>
            <dt>몰</dt>
            <dd>{item.mallName ?? '-'}</dd>
          </div>
          <div>
            <dt>카테고리</dt>
            <dd>{category}</dd>
          </div>
        </dl>
        {productUrl ? (
          <a
            className="secondary-button product-card-link"
            href={productUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            원본 보기
          </a>
        ) : null}
      </div>
    </article>
  );
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

function categoryPath(item: ShoppingSnapshotItem) {
  const values = [item.category1, item.category2, item.category3, item.category4]
    .filter((value): value is string => Boolean(value?.trim()));
  return values.length > 0 ? values.join(' > ') : '-';
}

function stripTags(value: string) {
  return value.replace(/<[^>]*>/g, '');
}
