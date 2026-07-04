export type HelpContentItem = {
  title: string;
  description: string;
  formula?: string;
  tip?: string;
  caution?: string;
  source?: string;
};

const helpContentItems = {
  candidateCount: {
    title: '검토 후보',
    description: '수집한 키워드, 쇼핑 스냅샷, 도매 업로드 데이터를 바탕으로 검토할 수 있는 상품 후보 수입니다.',
    tip: '후보 수는 판매 보장이 아니라 추가 검토가 필요한 작업량을 보여주는 지표입니다.'
  },
  searchResultCount: {
    title: '검색 결과 수',
    description: '네이버 쇼핑 검색 API가 해당 키워드에 대해 반환한 전체 검색 결과 수입니다.',
    tip: '검색 결과 수가 많으면 시장 규모가 클 수 있지만 경쟁도 함께 높을 수 있습니다.',
    caution: 'API 제공 기준에 따라 실제 판매량과는 다를 수 있습니다.'
  },
  minPrice: {
    title: '최저가',
    description: '수집된 상위 상품 중 가장 낮은 판매가입니다.',
    tip: '최저가는 가격 경쟁 하한선을 보는 데 사용합니다.',
    caution: '비정상적으로 낮은 가격이나 옵션 상품은 별도 확인이 필요합니다.'
  },
  avgPrice: {
    title: '평균가',
    description: '수집된 상품 가격의 평균값입니다.',
    formula: '수집 상품 가격 합계 / 유효 가격 상품 수',
    tip: '권장 판매가와 마진 계산의 1차 참고값으로 사용합니다.'
  },
  maxPrice: {
    title: '최고가',
    description: '수집된 상위 상품 중 가장 높은 판매가입니다.',
    tip: '프리미엄 가격대가 존재하는지 확인할 때 참고합니다.'
  },
  competitionLevel: {
    title: '경쟁도',
    description: '해당 키워드의 검색 결과 수를 기준으로 산출한 MVP 단계의 경쟁 강도입니다.',
    formula: '10,000건 이상 HIGH, 3,000건 이상 MEDIUM, 그 외 LOW',
    caution: '현재 경쟁도는 단순 기준이며 리뷰 수, 판매자 수, 광고 경쟁도는 아직 반영하지 않습니다.'
  },
  expectedMargin: {
    title: '예상 마진',
    description: '판매가에서 원가와 배송비 등 직접 비용을 제외한 예상 수익입니다.',
    formula: '판매가 - 원가 - 배송비 - 기타 비용',
    caution: '정산 수수료, 반품, 광고비가 빠져 있을 수 있으므로 확정 수익이 아닙니다.'
  },
  totalCost: {
    title: '총 원가',
    description: '공급가와 배송비를 합산한 1차 원가입니다.',
    formula: '공급가 + 배송비',
    caution: '플랫폼 수수료, 포장비, 반품 비용은 이 기본 계산에 포함되지 않습니다.'
  },
  marginRate: {
    title: '마진율',
    description: '판매가 대비 예상 마진의 비율입니다.',
    formula: '예상 마진 / 판매가 * 100',
    tip: '목표 마진율보다 낮으면 가격 조정이나 원가 재검토가 필요합니다.'
  },
  supplyPrice: {
    title: '공급가',
    description: '도매처 또는 공급사에서 제공받는 상품 매입 가격입니다.',
    caution: '부가세, 배송비, 옵션 추가 비용 포함 여부를 확인해야 합니다.'
  },
  shippingFee: {
    title: '배송비',
    description: '상품 1개 판매 시 발생하는 예상 배송 비용입니다.',
    tip: '무료배송 정책을 쓰는 경우 판매가에 배송비를 반영해야 합니다.'
  },
  salePrice: {
    title: '판매가',
    description: '스마트스토어 또는 판매 채널에 노출할 상품 가격입니다.',
    tip: '최저가와 평균가를 함께 비교해 무리한 가격 경쟁을 피합니다.'
  },
  recommendedSalePrice: {
    title: '권장 판매가',
    description: '입력한 원가와 목표 마진율을 기준으로 계산한 검토용 판매가입니다.',
    formula: '총 원가 / (1 - 목표 마진율)',
    caution: '시장 가격, 수수료, 반품 비용을 반영한 최종 판매가는 별도 검토가 필요합니다.'
  },
  targetMarginRate: {
    title: '목표 마진율',
    description: '상품을 검토할 때 기준으로 삼는 최소 목표 수익률입니다.',
    tip: '초기 검토 단계에서는 보수적인 목표 마진율을 권장합니다.'
  },
  productScore: {
    title: '상품 검토 점수',
    description: '마진, 경쟁도, 위험 카테고리, 데이터 품질 등을 종합해 계산한 검토용 점수입니다.',
    caution: '점수는 검토 우선순위이며 판매 가능성을 보장하지 않습니다.'
  },
  riskLevel: {
    title: '위험도',
    description: '낮은 마진, 원가 미설정, 위험 카테고리 등 운영 리스크를 요약한 상태입니다.',
    tip: '위험 상품은 원가, 판매가, 공급처 정보를 먼저 확인하세요.'
  },
  smartstoreSync: {
    title: '스마트스토어 동기화',
    description: '스마트스토어 상품 목록과 판매가를 셀러레이더에 가져오는 작업입니다.',
    caution: '현재는 skeleton/mock 중심이며 실제 API 인증과 토큰 갱신은 별도 확인이 필요합니다.'
  },
  lastSyncedAt: {
    title: '마지막 동기화 시각',
    description: '스마트스토어 상품 정보가 마지막으로 갱신된 시각입니다.',
    tip: '오래된 데이터는 판매가와 마진 판단이 부정확할 수 있습니다.'
  },
  uploadFileFormat: {
    title: '파일 형식',
    description: '도매 상품 업로드는 CSV 또는 XLSX 파일을 지원합니다.',
    tip: 'CSV 한글이 깨지면 encoding을 CP949로 바꿔 preview를 다시 실행하세요.'
  },
  requiredColumns: {
    title: '필수 컬럼',
    description: '상품명과 공급가는 후보 생성과 마진 계산에 반드시 필요한 컬럼입니다.',
    caution: '필수 컬럼이 비어 있으면 해당 row는 저장되지 않을 수 있습니다.'
  },
  uploadEncoding: {
    title: '인코딩',
    description: 'CSV 파일의 문자 인코딩입니다. AUTO, UTF-8, CP949를 선택할 수 있습니다.',
    tip: '엑셀에서 저장한 한글 CSV는 CP949인 경우가 많습니다.'
  },
  errorRows: {
    title: '오류 행',
    description: '필수값 누락, 숫자 파싱 실패, 잘못된 가격 등으로 저장되지 않은 row입니다.',
    tip: '오류 메시지의 행 번호와 원인을 확인한 뒤 원본 파일을 수정하세요.'
  },
  alertRule: {
    title: '알림 조건',
    description: '상품 점수, 마진율, 카테고리, 위험 제외 여부를 기준으로 알림을 생성하는 규칙입니다.',
    tip: '처음에는 조건을 좁게 잡아 과도한 알림을 줄이는 편이 좋습니다.'
  },
  batchStatus: {
    title: '배치 상태',
    description: '데이터 수집, 트렌드 갱신, 알림 생성 같은 백그라운드 작업의 처리 상태입니다.',
    caution: '외부 API 제한이나 인증 실패가 있으면 batch가 부분 실패할 수 있습니다.'
  },
  dataBaseDate: {
    title: '데이터 기준일',
    description: '스냅샷 또는 분석 결과가 어느 날짜의 데이터를 기준으로 만들어졌는지 나타냅니다.',
    tip: '같은 키워드라도 기준일이 다르면 가격과 경쟁도가 달라질 수 있습니다.'
  }
} satisfies Record<string, HelpContentItem>;

export type HelpContentKey = keyof typeof helpContentItems;

export const helpContent: Record<HelpContentKey, HelpContentItem> = helpContentItems;
