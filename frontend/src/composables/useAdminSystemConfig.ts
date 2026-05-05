import type { BackendResult } from '~/api/adminConfig'
import {
  configGetAlert,
  configGetAi,
  configGetBasic,
  configGetNotice,
  configGetRuntime,
  configSaveAlert,
  configSaveAi,
  configSaveBasic,
  configSaveNotice,
  configSaveRuntime,
  configTestAi,
} from '~/api/adminConfig'
import { isRecord } from '~/utils'

export interface AdminSystemBasicParams {
  pointsPerHour: number
  demandClassifyHours: number
  feedbackDays: number
  requireVideoWitness: boolean
  requireSmsVerify: boolean
  requireDoubleSign: boolean
  enableVoiceInput: boolean
  enableLargeText: boolean
  enablePhoneBooking: boolean
}

export interface AdminSystemNoticeTemplates {
  smsVerify: string
  demandApproved: string
  demandRejected: string
  volunteerApproved: string
  volunteerRejected: string
  demandMatched: string
}

export interface AdminSystemAlertRules {
  demandUnclaimedHours: number
  serviceUnfinishedHours: number
  fulfillmentRateThreshold: number
  complaintRateThreshold: number
  noShowFreezeCount: number
  enableUnclaimedAlert: boolean
  enableUnfinishedAlert: boolean
  enableFulfillmentAlert: boolean
  enableComplaintAlert: boolean
  careInactivityDays: number
  surge24hMinRequests: number
  surgeMultiplier: number
  enableCareInactivityAlert: boolean
  enableDemandSurgeAlert: boolean
}

export interface AdminSystemAiConfig {
  enabled: boolean
  baseUrl: string
  model: string
  hasApiKey?: boolean
  apiKey?: string
}

export interface AdminRuntimeConfig {
  demoModeEnabled: boolean
}

function applyIfRecord(target: Record<string, any>, res?: BackendResult<any>) {
  if (!res) return
  if (!isRecord(res.data)) return
  Object.assign(target, res.data)
}

export function useAdminSystemConfig() {
  const { t } = useI18n()
  const message = useMessage()

  const loading = ref(true)
  const saving = ref(false)

  const basicParams = reactive<AdminSystemBasicParams>({
    pointsPerHour: 10,
    demandClassifyHours: 2,
    feedbackDays: 3,
    requireVideoWitness: true,
    requireSmsVerify: true,
    requireDoubleSign: true,
    enableVoiceInput: true,
    enableLargeText: true,
    enablePhoneBooking: true,
  })

  const noticeTemplates = reactive<AdminSystemNoticeTemplates>({
    smsVerify: '【社区公益平台】您的验证码为：{code}，5分钟内有效，请勿泄露。',
    demandApproved:
      '【社区公益平台】您发布的需求「{title}」已通过审核，已进入志愿者匹配阶段。',
    demandRejected:
      '【社区公益平台】您发布的需求「{title}」未通过审核，原因：{reason}。',
    volunteerApproved:
      '【社区公益平台】您的志愿者申请已通过审核，可开始浏览并认领服务需求。',
    volunteerRejected:
      '【社区公益平台】您的志愿者申请未通过审核，如有疑问请联系社区管理员。',
    demandMatched:
      '【社区公益平台】您的需求「{title}」已匹配志愿者，请留意服务进度。',
  })

  const alertRules = reactive<AdminSystemAlertRules>({
    demandUnclaimedHours: 24,
    serviceUnfinishedHours: 48,
    fulfillmentRateThreshold: 0.8,
    complaintRateThreshold: 0.05,
    noShowFreezeCount: 3,
    enableUnclaimedAlert: true,
    enableUnfinishedAlert: true,
    enableFulfillmentAlert: true,
    enableComplaintAlert: true,
    careInactivityDays: 3,
    surge24hMinRequests: 5,
    surgeMultiplier: 2,
    enableCareInactivityAlert: true,
    enableDemandSurgeAlert: true,
  })

  const aiConfig = reactive<AdminSystemAiConfig>({
    enabled: false,
    baseUrl: 'https://api.deepseek.com',
    model: 'deepseek-v4-flash',
    hasApiKey: false,
    apiKey: '',
  })

  const runtimeConfig = reactive<AdminRuntimeConfig>({
    demoModeEnabled: true,
  })

  async function load() {
    loading.value = true
    try {
      const [basicRes, noticeRes, alertRes, aiRes, runtimeRes] = await Promise.all([
        configGetBasic(),
        configGetNotice(),
        configGetAlert(),
        configGetAi(),
        configGetRuntime(),
      ])

      applyIfRecord(basicParams as any, basicRes)
      applyIfRecord(noticeTemplates as any, noticeRes)
      applyIfRecord(alertRules as any, alertRes)
      applyIfRecord(aiConfig as any, aiRes)
      applyIfRecord(runtimeConfig as any, runtimeRes)
    } catch {
      message.error(t('community.systemConfig.loadFailed'))
    } finally {
      loading.value = false
    }
  }

  async function saveBasic() {
    saving.value = true
    try {
      const res = await configSaveBasic({ ...basicParams })
      message.success(res?.message || t('community.systemConfig.saveOkBasic'))
    } catch {
      message.error(t('community.systemConfig.saveFailed'))
    } finally {
      saving.value = false
    }
  }

  async function saveNotice() {
    saving.value = true
    try {
      const res = await configSaveNotice({ ...noticeTemplates })
      message.success(res?.message || t('community.systemConfig.saveOkNotice'))
    } catch {
      message.error(t('community.systemConfig.saveFailed'))
    } finally {
      saving.value = false
    }
  }

  async function saveAlert() {
    saving.value = true
    try {
      const res = await configSaveAlert({ ...alertRules })
      message.success(res?.message || t('community.systemConfig.saveOkAlert'))
    } catch {
      message.error(t('community.systemConfig.saveFailed'))
    } finally {
      saving.value = false
    }
  }

  async function saveAi() {
    saving.value = true
    try {
      const payload: Record<string, unknown> = {
        enabled: aiConfig.enabled,
        baseUrl: aiConfig.baseUrl,
        model: aiConfig.model,
      }
      const key = String(aiConfig.apiKey || '').trim()
      if (key) payload.apiKey = key
      const res = await configSaveAi(payload)
      aiConfig.apiKey = ''
      await load()
      message.success(res?.message || t('community.systemConfig.saveOkAi'))
    } catch {
      message.error(t('community.systemConfig.saveFailed'))
    } finally {
      saving.value = false
    }
  }

  async function saveRuntime() {
    saving.value = true
    try {
      const res = await configSaveRuntime({ ...runtimeConfig })
      message.success(res?.message || '运行态配置已保存')
    } catch {
      message.error(t('community.systemConfig.saveFailed'))
    } finally {
      saving.value = false
    }
  }

  async function testAi() {
    saving.value = true
    try {
      const payload: Record<string, unknown> = {
        baseUrl: aiConfig.baseUrl,
        model: aiConfig.model,
      }
      const key = String(aiConfig.apiKey || '').trim()
      if (key) payload.apiKey = key
      const res = await configTestAi(payload)
      return res?.data || {}
    } finally {
      saving.value = false
    }
  }

  onMounted(load)

  return {
    loading,
    saving,
    basicParams,
    noticeTemplates,
    alertRules,
    aiConfig,
    runtimeConfig,
    load,
    saveBasic,
    saveNotice,
    saveAlert,
    saveAi,
    saveRuntime,
    testAi,
  }
}
