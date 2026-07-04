import { useEffect, useState } from 'react';
import { ApiRequestError, apiBaseUrl, getAccessToken } from '../api/httpClient';
import { listCandidates } from '../api/candidates';
import { AlertsPage } from '../routes/alerts/AlertsPage';
import { CandidateDetailPage } from '../routes/candidates/CandidateDetailPage';
import { CandidatesPage } from '../routes/candidates/CandidatesPage';
import { KeywordDetailPage } from '../routes/keywords/KeywordDetailPage';
import { KeywordsPage } from '../routes/keywords/KeywordsPage';
import { MarginCalculatorPage } from '../routes/margin/MarginCalculatorPage';
import { StoreMarginsPage } from '../routes/store/StoreMarginsPage';
import { WholesalePage } from '../routes/wholesale/WholesalePage';
import { WholesaleUploadPage } from '../routes/wholesale/WholesaleUploadPage';
import { MetricCard } from '../components/ui';

const navigationItems = [
  { label: '대시보드', href: '/' },
  { label: '키워드 레이더', href: '/keywords' },
  { label: '상품 후보', href: '/candidates' },
  { label: '도매 업로드', href: '/wholesale/uploads' },
  { label: '내 상품 마진', href: '/store/margins' },
  { label: '마진 계산기', href: '/margin' },
  { label: '알림', href: '/alerts' }
];

export function App() {
  const path = window.location.pathname;
  const keywordDetailMatch = path.match(/^\/keywords\/(\d+)$/);
  const candidateDetailMatch = path.match(/^\/candidates\/(\d+)$/);

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="주요 메뉴">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            SR
          </span>
          <span>셀러레이더</span>
        </div>
        <nav className="nav-list">
          {navigationItems.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className={`nav-item ${isActiveNavItem(path, item.href) ? 'nav-item-active' : ''}`}
            >
              {item.label}
            </a>
          ))}
        </nav>
      </aside>

      <main className="content">
        {path === '/keywords' ? <KeywordsPage /> : null}
        {keywordDetailMatch ? <KeywordDetailPage keywordId={Number(keywordDetailMatch[1])} /> : null}
        {candidateDetailMatch ? (
          <CandidateDetailPage candidateId={Number(candidateDetailMatch[1])} />
        ) : null}
        {path === '/candidates' ? <CandidatesPage /> : null}
        {path === '/wholesale/uploads' ? <WholesaleUploadPage /> : null}
        {path === '/wholesale' ? <WholesalePage /> : null}
        {path === '/store/margins' ? <StoreMarginsPage /> : null}
        {path === '/margin' ? <MarginCalculatorPage /> : null}
        {path === '/alerts' ? <AlertsPage mode="list" /> : null}
        {path === '/alert-rules' ? <AlertsPage mode="rules" /> : null}
        {path === '/' ? <Dashboard /> : null}
      </main>
    </div>
  );
}

function isActiveNavItem(path: string, href: string) {
  if (href === '/') {
    return path === '/';
  }
  return path === href || path.startsWith(`${href}/`);
}

function Dashboard() {
  const [statusItems, setStatusItems] = useState([
    { label: '추천 검토 후보', value: '0' },
    { label: '검토 후보', value: '0' },
    { label: '보류', value: '0' },
    { label: '분석 대기', value: '0' }
  ]);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let ignore = false;
    async function loadSummary() {
      if (!getAccessToken()) {
        setMessage('계정 연결 후 후보 요약을 불러옵니다.');
        return;
      }
      try {
        const response = await listCandidates({ page: 0, size: 100 });
        if (ignore) {
          return;
        }
        const recommended = response.items.filter((item) => item.grade === 'RECOMMENDED').length;
        const review = response.items.filter((item) => item.grade === 'REVIEW').length;
        const hold = response.items.filter((item) => item.grade === 'HOLD').length;
        setStatusItems([
          { label: '추천 검토 후보', value: String(recommended) },
          { label: '검토 후보', value: String(review) },
          { label: '보류', value: String(hold) },
          { label: '전체 후보', value: String(response.totalElements) }
        ]);
        setMessage('');
      } catch (error) {
        if (ignore) {
          return;
        }
        setMessage(error instanceof ApiRequestError ? error.message : 'API 서버 연결을 확인하세요.');
      }
    }
    void loadSummary();
    return () => {
      ignore = true;
    };
  }, []);

  return (
    <>
      <header className="page-header">
        <div>
          <p className="eyebrow">Seller Radar</p>
          <h1>오늘의 분석 요약</h1>
        </div>
        <div className="api-status">API {apiBaseUrl}</div>
      </header>

      {message ? <div className="notice">{message}</div> : null}

      <section className="summary-grid" aria-label="분석 상태 요약">
        {statusItems.map((item) => (
          <MetricCard
            key={item.label}
            label={item.label}
            value={item.value}
            helpKey={item.label.includes('후보') ? 'candidateCount' : undefined}
          />
        ))}
      </section>

      <section className="work-panel" aria-labelledby="next-work-title">
        <div>
          <p className="eyebrow">MVP</p>
          <h2 id="next-work-title">데이터 기반 검토 후보</h2>
          <p>
            키워드, 쇼핑 스냅샷, CSV 마진, 알림 조건을 연결해 후보를 검토하는
            운영 화면입니다.
          </p>
          <div className="button-row">
            <a className="primary-button" href="/keywords">
              키워드 등록
            </a>
            <a className="secondary-button" href="/wholesale/uploads">
              CSV 업로드
            </a>
            <a className="secondary-button" href="/candidates">
              후보 보기
            </a>
          </div>
        </div>
        <div className="empty-state">
          네이버 데이터랩 수치는 검색 클릭 추이 기반이며 실제 판매량을 의미하지 않습니다.
        </div>
      </section>
    </>
  );
}
