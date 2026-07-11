import { defineConfig, loadEnv, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');
  const csp = contentSecurityPolicy(env.VITE_API_BASE_URL, env.VITE_CSP_REPORT_URI);

  return {
    plugins: [react(), staticSecurityHeaders(csp)],
    server: {
      port: 5173,
      headers: browserHeaders('Content-Security-Policy-Report-Only', csp, false),
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    preview: {
      headers: browserHeaders('Content-Security-Policy', csp, false)
    }
  };
});

function contentSecurityPolicy(apiBaseUrl?: string, reportUri?: string) {
  const apiOrigin = safeOrigin(apiBaseUrl);
  const directives = [
    "default-src 'self'",
    "base-uri 'self'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "form-action 'self'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' https: data:",
    "font-src 'self' data:",
    `connect-src 'self'${apiOrigin ? ` ${apiOrigin}` : ''}`
  ];
  if (reportUri?.trim()) {
    directives.push(`report-uri ${reportUri.trim()}`);
  }
  return directives.join('; ');
}

function safeOrigin(rawUrl?: string) {
  if (!rawUrl?.trim()) {
    return null;
  }
  try {
    const url = new URL(rawUrl);
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.origin : null;
  } catch {
    return null;
  }
}

function browserHeaders(cspHeader: string, csp: string, includeHsts: boolean) {
  return {
    [cspHeader]: csp,
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'no-referrer',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
    'Cross-Origin-Opener-Policy': 'same-origin',
    ...(includeHsts ? { 'Strict-Transport-Security': 'max-age=31536000; includeSubDomains' } : {})
  };
}

function staticSecurityHeaders(csp: string): Plugin {
  const headers = browserHeaders('Content-Security-Policy', csp, true);
  const source = ['/*', ...Object.entries(headers).map(([name, value]) => `  ${name}: ${value}`), ''].join('\n');
  return {
    name: 'seller-radar-static-security-headers',
    apply: 'build',
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: '_headers', source });
    }
  };
}
