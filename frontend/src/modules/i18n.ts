import messages from '@intlify/unplugin-vue-i18n/messages'
import { createI18n } from 'vue-i18n'
import type { AppModule } from '~/types'

const storedValue = localStorage.getItem('layout')
// 默认语言改为中文（zn）
let locale = 'zn'
if (storedValue) {
  const parsed = JSON.parse(storedValue)
  if (parsed && Object.hasOwn(parsed, 'activeLanguage')) {
    const allowed = ['en', 'zn']
    if (allowed.includes(parsed.activeLanguage)) locale = parsed.activeLanguage
    else if (parsed.activeLanguage) locale = 'zn'
  }
}

const i18n = createI18n({
  legacy: false,
  locale,
  messages,
})

export const install: AppModule = (app) => {
  app.use(i18n)
}

export default i18n
