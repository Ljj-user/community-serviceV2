import type { App } from 'vue'
import NutUI from '@nutui/nutui'
import '@nutui/nutui/dist/style.css'
import '@nutui/touch-emulator'
import TDesign from 'tdesign-mobile-vue'
import 'tdesign-mobile-vue/es/style/index.css'

function install(app: App) {
  app.use(NutUI)
  app.use(TDesign)
}

export default { install }
