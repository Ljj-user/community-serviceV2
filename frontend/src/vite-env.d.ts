/// <reference types="vite/client" />
/// <reference types="vite-plugin-pages/client" />
/// <reference types="vite-plugin-vue-layouts-next/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_API_MOCKING_ENABLED?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
