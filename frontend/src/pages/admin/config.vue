<route lang="yaml">
meta:
  title: admin-config
  layout: default
  breadcrumb:
    - adminSystemGroup
    - admin-config
</route>

<script setup lang="ts">
import PageHeader from '~/components/shared/PageHeader.vue'
import FieldHint from '~/components/shared/FieldHint.vue'
import { useAdminSystemConfig } from '~/composables/useAdminSystemConfig'

const { t } = useI18n()
const {
  loading,
  saving,
  basicParams,
  noticeTemplates,
  alertRules,
  aiConfig,
  runtimeConfig,
  saveBasic,
  saveNotice,
  saveAlert,
  saveAi,
  saveRuntime,
  testAi,
} = useAdminSystemConfig()

const message = useMessage()

async function onTestAi() {
  try {
    const res = await testAi()
    if (res?.ok) {
      message.success(t('community.systemConfig.aiTestOk', {
        ms: res.latencyMs ?? '-',
        status: res.status ?? '-',
      }))
    } else {
      message.error(t('community.systemConfig.aiTestFail', {
        status: res?.status ?? '-',
        err: res?.error ?? 'unknown',
      }))
    }
  } catch (e: any) {
    message.error(e?.message || t('community.systemConfig.aiTestFail', { status: '-', err: 'unknown' }))
  }
}
</script>

<template>
  <div class="space-y-4">
    <PageHeader
      :title="t('community.systemConfig.title')"
      :subtitle="t('community.systemConfig.subtitle')"
    />

    <n-spin :show="loading">
      <n-collapse default-expanded-names="basic">
      <!-- 一、基础参数 -->
      <n-collapse-item name="basic" :title="t('community.systemConfig.collapseBasic')">
        <template #header-extra>
          <n-button size="small" type="primary" :loading="saving" @click.stop="saveBasic">
            {{ t('community.systemConfig.btnSave') }}
          </n-button>
        </template>
        <Card :title="t('community.systemConfig.cardBusiness')">
          <div class="grid gap-x-8 gap-y-6 sm:grid-cols-2">
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.pointsPerHour')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="basicParams.pointsPerHour" :min="1" :max="100" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.pointsPerHourHint')" />
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.demandClassifyHours')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="basicParams.demandClassifyHours" :min="1" :max="72" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.demandClassifyHoursHint')" />
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.feedbackDays')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="basicParams.feedbackDays" :min="1" :max="30" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.feedbackDaysHint')" />
            </div>
          </div>
        </Card>
        <Card title="演示环境" class="mt-4">
          <div class="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-center">
            <div>
              <div class="text-sm font-semibold text-slate-800 dark:text-slate-100">统一演示模式</div>
              <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
                当前项目使用真实后端演示数据，不再依赖前端假列表开关。
              </p>
            </div>
            <div class="flex items-center gap-3">
              <n-tag :type="runtimeConfig.demoModeEnabled ? 'success' : 'default'" :bordered="false">
                {{ runtimeConfig.demoModeEnabled ? '已开启' : '已关闭' }}
              </n-tag>
              <n-switch v-model:value="runtimeConfig.demoModeEnabled" @update:value="() => saveRuntime()" />
            </div>
          </div>
        </Card>
        <Card :title="t('community.systemConfig.cardCompliance')" class="mt-4">
          <div class="space-y-5">
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.requireVideoWitness')" label-placement="top" :show-feedback="false">
                <n-switch v-model:value="basicParams.requireVideoWitness" />
              </n-form-item>
              <p class="text-xs text-slate-500 mt-1">
                {{ t('community.systemConfig.requireVideoWitnessHint') }}
              </p>
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.requireSmsVerify')" label-placement="top" :show-feedback="false">
                <n-switch v-model:value="basicParams.requireSmsVerify" />
              </n-form-item>
              <p class="text-xs text-slate-500 mt-1">
                {{ t('community.systemConfig.requireSmsVerifyHint') }}
              </p>
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.requireDoubleSign')" label-placement="top" :show-feedback="false">
                <n-switch v-model:value="basicParams.requireDoubleSign" />
              </n-form-item>
              <p class="text-xs text-slate-500 mt-1">
                {{ t('community.systemConfig.requireDoubleSignHint') }}
              </p>
            </div>
          </div>
        </Card>
        <Card :title="t('community.systemConfig.cardA11y')" class="mt-4">
          <div class="space-y-5">
            <n-form-item :label="t('community.systemConfig.enableVoiceInput')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="basicParams.enableVoiceInput" />
            </n-form-item>
            <n-form-item :label="t('community.systemConfig.enableLargeText')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="basicParams.enableLargeText" />
            </n-form-item>
            <n-form-item :label="t('community.systemConfig.enablePhoneBooking')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="basicParams.enablePhoneBooking" />
            </n-form-item>
          </div>
        </Card>
      </n-collapse-item>

      <!-- 二、通知模板 -->
      <n-collapse-item name="notice" :title="t('community.systemConfig.collapseNotice')">
        <template #header-extra>
          <n-button size="small" type="primary" :loading="saving" @click.stop="saveNotice">
            {{ t('community.systemConfig.btnSave') }}
          </n-button>
        </template>
        <n-alert type="info" class="mb-4" :bordered="false">
          {{ t('community.systemConfig.noticePlaceholderHint') }}
        </n-alert>
        <div class="space-y-5">
          <n-form-item :label="t('community.systemConfig.tplSmsVerify')" label-placement="top" :show-feedback="false">
            <n-input v-model:value="noticeTemplates.smsVerify" type="textarea" :rows="2" :placeholder="t('community.systemConfig.phSmsVerify')" />
          </n-form-item>
          <n-form-item :label="t('community.systemConfig.tplDemandApproved')" label-placement="top" :show-feedback="false">
            <n-input v-model:value="noticeTemplates.demandApproved" type="textarea" :rows="2" :placeholder="t('community.systemConfig.phDemandApproved')" />
          </n-form-item>
          <n-form-item :label="t('community.systemConfig.tplDemandRejected')" label-placement="top" :show-feedback="false">
            <n-input v-model:value="noticeTemplates.demandRejected" type="textarea" :rows="2" :placeholder="t('community.systemConfig.phDemandRejected')" />
          </n-form-item>
          <n-form-item :label="t('community.systemConfig.tplVolunteerApproved')" label-placement="top" :show-feedback="false">
            <n-input v-model:value="noticeTemplates.volunteerApproved" type="textarea" :rows="2" :placeholder="t('community.systemConfig.phVolunteerApproved')" />
          </n-form-item>
          <n-form-item :label="t('community.systemConfig.tplVolunteerRejected')" label-placement="top" :show-feedback="false">
            <n-input v-model:value="noticeTemplates.volunteerRejected" type="textarea" :rows="2" :placeholder="t('community.systemConfig.phVolunteerRejected')" />
          </n-form-item>
          <n-form-item :label="t('community.systemConfig.tplDemandMatched')" label-placement="top" :show-feedback="false">
            <n-input v-model:value="noticeTemplates.demandMatched" type="textarea" :rows="2" :placeholder="t('community.systemConfig.phDemandMatched')" />
          </n-form-item>
        </div>
      </n-collapse-item>

      <!-- 三、预警规则 -->
      <n-collapse-item name="alert" :title="t('community.systemConfig.collapseAlert')">
        <template #header-extra>
          <n-button size="small" type="primary" :loading="saving" @click.stop="saveAlert">
            {{ t('community.systemConfig.btnSave') }}
          </n-button>
        </template>
        <Card :title="t('community.systemConfig.cardTimeout')">
          <div class="grid gap-x-8 gap-y-6 sm:grid-cols-2">
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.demandUnclaimedHours')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="alertRules.demandUnclaimedHours" :min="1" :max="168" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.demandUnclaimedHoursHint')" />
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.serviceUnfinishedHours')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="alertRules.serviceUnfinishedHours" :min="1" :max="336" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.serviceUnfinishedHoursHint')" />
            </div>
            <n-form-item :label="t('community.systemConfig.enableUnclaimedAlert')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="alertRules.enableUnclaimedAlert" />
            </n-form-item>
            <n-form-item :label="t('community.systemConfig.enableUnfinishedAlert')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="alertRules.enableUnfinishedAlert" />
            </n-form-item>
          </div>
        </Card>
        <Card :title="t('community.systemConfig.cardKpi')" class="mt-4">
          <div class="grid gap-x-8 gap-y-6 sm:grid-cols-2">
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.fulfillmentRateThreshold')" label-placement="top" :show-feedback="false">
                <n-input-number
                  v-model:value="alertRules.fulfillmentRateThreshold"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  class="w-full"
                />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.fulfillmentRateHint')" />
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.complaintRateThreshold')" label-placement="top" :show-feedback="false">
                <n-input-number
                  v-model:value="alertRules.complaintRateThreshold"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  class="w-full"
                />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.complaintRateHint')" />
            </div>
            <n-form-item :label="t('community.systemConfig.enableFulfillmentAlert')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="alertRules.enableFulfillmentAlert" />
            </n-form-item>
            <n-form-item :label="t('community.systemConfig.enableComplaintAlert')" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="alertRules.enableComplaintAlert" />
            </n-form-item>
          </div>
        </Card>
        <Card :title="t('community.systemConfig.cardCredit')" class="mt-4">
          <div class="config-field max-w-xs">
            <n-form-item :label="t('community.systemConfig.noShowFreezeCount')" label-placement="top" :show-feedback="false">
              <n-input-number v-model:value="alertRules.noShowFreezeCount" :min="1" :max="10" class="w-full" />
            </n-form-item>
            <FieldHint :text="t('community.systemConfig.noShowFreezeHint')" />
          </div>
        </Card>
        <Card :title="t('community.systemConfig.cardAnomaly')" class="mt-4">
          <div class="grid gap-x-8 gap-y-6 sm:grid-cols-2">
            <n-form-item label="启用重点人群连续未登录预警" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="alertRules.enableCareInactivityAlert" />
            </n-form-item>
            <n-form-item label="启用社区求助骤增预警" label-placement="top" :show-feedback="false">
              <n-switch v-model:value="alertRules.enableDemandSurgeAlert" />
            </n-form-item>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.careInactivityDays')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="alertRules.careInactivityDays" :min="1" :max="30" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.careInactivityDaysHint')" />
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.surge24hMinRequests')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="alertRules.surge24hMinRequests" :min="1" :max="200" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.surge24hMinRequestsHint')" />
            </div>
            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.surgeMultiplier')" label-placement="top" :show-feedback="false">
                <n-input-number v-model:value="alertRules.surgeMultiplier" :min="1" :max="10" :step="0.1" :precision="1" class="w-full" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.surgeMultiplierHint')" />
            </div>
          </div>
        </Card>
      </n-collapse-item>

      <!-- 四、AI 配置 -->
      <n-collapse-item name="ai" :title="t('community.systemConfig.collapseAi')">
        <template #header-extra>
          <n-button size="small" :loading="saving" @click.stop="onTestAi">
            {{ t('community.systemConfig.btnTestAi') }}
          </n-button>
          <n-button size="small" type="primary" :loading="saving" @click.stop="saveAi">
            {{ t('community.systemConfig.btnSave') }}
          </n-button>
        </template>

        <Card :title="t('community.systemConfig.cardAi')">
          <div class="grid gap-x-8 gap-y-6 sm:grid-cols-2">
            <div class="config-field sm:col-span-2">
              <n-form-item :label="t('community.systemConfig.aiEnabled')" label-placement="top" :show-feedback="false">
                <div class="flex items-center gap-3">
                  <n-switch v-model:value="aiConfig.enabled" />
                  <n-tag v-if="aiConfig.hasApiKey" size="small" type="success">
                    {{ t('community.systemConfig.aiKeyConfigured') }}
                  </n-tag>
                  <n-tag v-else size="small" type="warning">
                    {{ t('community.systemConfig.aiKeyMissing') }}
                  </n-tag>
                </div>
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.aiEnabledHint')" />
            </div>

            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.aiBaseUrl')" label-placement="top" :show-feedback="false">
                <n-input v-model:value="aiConfig.baseUrl" placeholder="https://api.deepseek.com" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.aiBaseUrlHint')" />
            </div>

            <div class="config-field">
              <n-form-item :label="t('community.systemConfig.aiModel')" label-placement="top" :show-feedback="false">
                <n-input v-model:value="aiConfig.model" placeholder="deepseek-v4-flash" />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.aiModelHint')" />
            </div>

            <div class="config-field sm:col-span-2">
              <n-form-item :label="t('community.systemConfig.aiApiKey')" label-placement="top" :show-feedback="false">
                <n-input
                  v-model:value="aiConfig.apiKey"
                  type="password"
                  show-password-on="mousedown"
                  :placeholder="t('community.systemConfig.aiApiKeyPh')"
                />
              </n-form-item>
              <FieldHint :text="t('community.systemConfig.aiApiKeyHint')" />
            </div>
          </div>
        </Card>
      </n-collapse-item>
    </n-collapse>
    </n-spin>
  </div>
</template>

<style scoped>
.config-field {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.config-field :deep(.n-form-item) {
  margin-bottom: 0;
}
</style>
