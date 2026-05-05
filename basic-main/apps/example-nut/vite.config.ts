import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { defineConfig, loadEnv } from 'vite'
import createVitePlugins from './vite/plugins'

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  // 全局 scss 资源
  const scssResources: string[] = []
  fs.readdirSync('src/assets/styles/resources').forEach((dirname) => {
    if (fs.statSync(`src/assets/styles/resources/${dirname}`).isFile()) {
      scssResources.push(`@use "/src/assets/styles/resources/${dirname}" as *;`)
    }
  })
  return {
    base: './',
    // 开发服务器选项 https://cn.vitejs.dev/config/server-options
    server: {
      open: true,
      host: true,
      port: 9000,
      proxy: {
        '/api': {
          // 迁移自原 `front_mobile`：开发期统一走 /api 代理到后端 8080
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, ''),
        },
      },
    },
    // 构建选项 https://cn.vitejs.dev/config/build-options
    build: {
      outDir: mode === 'production' ? 'dist' : `dist-${mode}`,
      sourcemap: env.VITE_BUILD_SOURCEMAP === 'true',
    },
    plugins: createVitePlugins(mode, command === 'build'),
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        '#': path.resolve(__dirname, 'src/types'),
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: scssResources.join(''),
        },
      },
    },
  }
})
