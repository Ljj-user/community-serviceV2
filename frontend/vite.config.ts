import path from 'node:path'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import Vue from '@vitejs/plugin-vue'
import Unocss from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import VueMacros from 'vue-macros/vite'
import { VueRouterAutoImports } from 'unplugin-vue-router'
import { defineConfig } from 'vite'
import Pages from 'vite-plugin-pages'
import Layouts from 'vite-plugin-vue-layouts-next'

export default defineConfig({
  // Windows + 中文路径下，个别机器会在启动时写入 d.ts 发生 UNKNOWN open 错误。
  // 这里关闭运行期自动写入，改为使用仓库内已存在的类型声明文件，避免 dev server 启动失败。
  // 如需重新生成，可临时改回具体 d.ts 路径执行一次。
  server: {
    port: 7000,
    host: true,
    hmr: {
      host: 'localhost',
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  assetsInclude: ['**/*.yml', '**/*.yaml'],
  resolve: {
    alias: {
      '~/': `${path.resolve(__dirname, 'src')}/`,
      '@/': `${path.resolve(__dirname, 'src')}/`,
    },
  },
  build: {
    chunkSizeWarningLimit: 1600,
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
      },
    },
  },

  plugins: [
    // https://github.com/intlify/vite-plugin-vue-i18n
    VueI18nPlugin({
      runtimeOnly: false,
      fullInstall: true,
      include: [
        path.resolve(__dirname, 'src/locales/**/*.yml'),
        path.resolve(__dirname, 'src/locales/**/*.yaml'),
      ]
    }),
    VueMacros({
      plugins: {
        vue: Vue({
          include: [/\.vue$/],
        }),
      },
    }),

    Pages({
      extensions: ['vue'],
      importMode: 'async',
    }),

    // https://github.com/JohnCampionJr/vite-plugin-vue-layouts
    Layouts({
      layoutsDirs: 'src/layouts',
      pagesDirs: 'src/pages',
      defaultLayout: 'default',
    }),

    // https://github.com/antfu/unplugin-auto-import
    AutoImport({
      imports: [
        'vue',
        'vue/macros',
        'vue-i18n',
        '@vueuse/head',
        '@vueuse/core',
        VueRouterAutoImports,
        {
          // add any other imports you were relying on
          'vue-router/auto': ['useLink'],
        },
        {
          'naive-ui': [
            'useDialog',
            'useMessage',
            'useNotification',
            'useLoadingBar',
            'usePopover',
          ],
        },
      ],
      dts: false,
      dirs: ['src/composables', 'src/store'],
      vueTemplate: true,
    }),

    Components({
      extensions: ['vue'],
      resolvers: [NaiveUiResolver()],
      include: [/\.vue$/, /\.vue\?vue/],
      dts: false,
    }),

    // https://github.com/antfu/unocss
    // see uno.config.ts for config
    Unocss(),
    // https://github.com/webfansplz/vite-plugin-vue-devtools
    // VueDevTools(),
  ],
  define: { 'process.env': {} },
})
