export const useAppPrefsStore = defineStore(
  'appPrefs',
  () => {
    // 暂停大字模式：先固定为 false，避免布局挤压问题
    const largeText = ref(false)
    const reduceMotion = ref(false)
    const dataSaver = ref(false)
    const demoMock = ref(true)
    const showDevTools = ref(false)

    const notifyAll = ref(true)
    const notifyChat = ref(true)
    const notifyTask = ref(true)
    const dndEnabled = ref(false)
    const dndStart = ref('22:00')
    const dndEnd = ref('08:00')

    watch(largeText, (v) => {
      if (v) largeText.value = false
      document.documentElement.classList.remove('m-a11y-large')
    }, { immediate: true })

    watch(reduceMotion, (v) => {
      document.documentElement.classList.toggle('m-reduce-motion', v)
    }, { immediate: true })

    return {
      largeText,
      reduceMotion,
      dataSaver,
      demoMock,
      showDevTools,
      notifyAll,
      notifyChat,
      notifyTask,
      dndEnabled,
      dndStart,
      dndEnd,
    }
  },
  {
    persist: true,
  },
)

