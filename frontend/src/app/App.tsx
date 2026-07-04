import { type ReactNode, useEffect, useState } from 'react';
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
import { EmptyState, MetricCard } from '../components/ui';

const navigationItems = [
  { label: '대시보드', href: '/' },
  { label: '키워드 레이더', href: '/keywords' },
  { label: '상품 후보', href: '/candidates' },
  { label: '도매 업로드', href: '/wholesale/uploads' },
  { label: '내 상품 마진', href: '/store/margins' },
  { label: '마진 계산기', href: '/margin' },
  { label: '알림', href: '/alerts' }
];

interface RouteView {
  content: ReactNode;
  activeNavHref: string | null;
}

export function App() {
  const [path, setPath] = useState(() => normalizePath(window.location.pathname));
  const route = resolveRoute(path);

  useEffect(() => {
    const normalizedPath = normalizePath(window.location.pathname);
    if (normalizedPath !== window.location.pathname) {
      window.history.replaceState(null, '', buildHref(normalizedPath, window.location.search, window.location.hash));
      setPath(normalizedPath);
    }

    function handlePopState() {
      setPath(normalizePath(window.location.pathname));
    }

    function handleDocumentClick(event: MouseEvent) {
      if (event.defaultPrevented || event.button !== 0 || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) {
        return;
      }
      if (!(event.target instanceof Element)) {
        return;
      }

      const anchor = event.target.closest('a[href]');
      if (!(anchor instanceof HTMLAnchorElement)) {
        return;
      }
      if (anchor.target && anchor.target !== '_self') {
        return;
      }
      if (anchor.hasAttribute('download')) {
        return;
      }

      const url = new URL(anchor.href);
      if (url.origin !== window.location.origin) {
        return;
      }

      const nextHref = buildHref(url.pathname, url.search, url.hash);
      const currentHref = buildHref(window.location.pathname, window.location.search, window.location.hash);
      event.preventDefault();
      if (nextHref === currentHref) {
        return;
      }

      window.history.pushState(null, '', nextHref);
      setPath(normalizePath(url.pathname));
      window.scrollTo({ top: 0, left: 0 });
    }

    window.addEventListener('popstate', handlePopState);
    document.addEventListener('click', handleDocumentClick);
    return () => {
      window.removeEventListener('popstate', handlePopState);
      document.removeEventListener('click', handleDocumentClick);
    };
  }, []);

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
              className={`nav-item ${route.activeNavHref === item.href ? 'nav-item-active' : ''}`}
              aria-current={route.activeNavHref === item.href ? 'page' : undefined}
            >
              {item.label}
            </a>
          ))}
        </nav>
      </aside>

      <main className="content">
        {route.content}
      </main>
    </div>
  );
}

function resolveRoute(path: string): RouteView {
  const keywordDetailMatch = path.match(/^\/keywords\/(\d+)$/);
  const candidateDetailMatch = path.match(/^\/candidates\/(\d+)$/);

  if (path === '/') {
    return { content: <Dashboard />, activeNavHref: '/' };
  }
  if (path === '/keywords') {
    return { content: <KeywordsPage />, activeNavHref: '/keywords' };
  }
  if (keywordDetailMatch) {
    return { content: <KeywordDetailPage keywordId={Number(keywordDetailMatch[1])} />, activeNavHref: '/keywords' };
  }
  if (path === '/candidates') {
    return { content: <CandidatesPage />, activeNavHref: '/candidates' };
  }
  if (candidateDetailMatch) {
    return { content: <CandidateDetailPage candidateId={Number(candidateDetailMatch[1])} />, activeNavHref: '/candidates' };
  }
  if (path === '/wholesale/uploads') {
    return { content: <WholesaleUploadPage />, activeNavHref: '/wholesale/uploads' };
  }
  if (path === '/wholesale') {
    return { content: <WholesalePage />, activeNavHref: '/wholesale/uploads' };
  }
  if (path === '/store/margins') {
    return { content: <StoreMarginsPage />, activeNavHref: '/store/margins' };
  }
  if (path === '/margin') {
    return { content: <MarginCalculatorPage />, activeNavHref: '/margin' };
  }
  if (path === '/alerts') {
    return { content: <AlertsPage mode="list" />, activeNavHref: '/alerts' };
  }
  if (path === '/alert-rules') {
    return { content: <AlertsPage mode="rules" />, activeNavHref: '/alerts' };
  }

  return { content: <NotFoundPage path={path} />, activeNavHref: null };
}

function normalizePath(pathname: string) {
  if (!pathname || pathname === '/') {
    return '/';
  }
  return pathname.replace(/\/+$/, '') || '/';
}

function buildHref(pathname: string, search = '', hash = '') {
  return `${normalizePath(pathname)}${search}${hash}`;
}

function NotFoundPage({ path }: { path: string }) {
  return (
    <section className="not-found-page" aria-labelledby="not-found-title">
      <div>
        <p className="eyebrow">Not Found</p>
        <h1 id="not-found-title">페이지를 찾을 수 없습니다</h1>
      </div>
      <EmptyState>
        요청한 경로 <code>{path}</code>는 셀러레이더에서 제공하지 않는 화면입니다.
      </EmptyState>
      <div className="button-row">
        <a className="primary-button" href="/">
          대시보드
        </a>
        <a className="secondary-button" href="/keywords">
          키워드 레이더
        </a>
      </div>
    </section>
  );
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
