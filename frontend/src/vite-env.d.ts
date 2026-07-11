/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SMARTSTORE_MOCK_ENABLED?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
