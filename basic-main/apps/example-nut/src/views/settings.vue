<script setup lang="ts">
import { toast } from 'vue-sonner'
import apiApp from '@/api/modules/app'

definePage({
  meta: {
    title: '设置',
    auth: true,
  },
})

const router = useRouter()
const appSettingsStore = useAppSettingsStore()
const prefs = useAppPrefsStore()
const runtime = ref<{ demoModeEnabled?: boolean; demoModeLabel?: string; demoDataHint?: string } | null>(null)

const themeMode = computed({
  get: () => appSettingsStore.settings.theme.colorScheme,
  set: (v: any) => {
    appSettingsStore.settings.theme.colorScheme = v
  },
})

function onClearCache() {
  toast.success('已清理缓存（演示）')
}

function resetDisplay() {
  prefs.largeText = false
  document.documentElement.classList.remove('m-a11y-large')
  toast.success('已恢复默认显示')
}

const versionText = computed(() => {
  const v = import.meta.env.VITE_APP_VERSION
  return v ? String(v) : '未配置'
})

async function loadRuntime() {
  try {
    const res = await apiApp.runtime()
    runtime.value = res.data
  } catch {
    runtime.value = null
  }
}

onMounted(loadRuntime)
</script>

<template>
  <AppPageLayout :navbar="false">
    <div class="settings-page m-mobile-page-bg">
      <t-navbar title="设置中心" left-arrow @left-click="router.back()" />

      <div class="content">
        <t-button theme="default" variant="outline" block @click="resetDisplay">
          恢复默认显示（修复大字异常）
        </t-button>

        <div class="group-title">
          外观与无障碍
        </div>
        <t-cell-group inset>
          <t-cell title="深色模式" description="跟随系统 / 浅色 / 深色" />
          <t-cell>
            <template #title>
              <t-radio-group v-model="themeMode" class="mode mode-block">
                <t-radio value="">
                  跟随
                </t-radio>
                <t-radio value="light">
                  浅色
                </t-radio>
                <t-radio value="dark">
                  深色
                </t-radio>
              </t-radio-group>
            </template>
          </t-cell>
          <t-cell title="大字模式" description="暂未开放（布局适配中）" disabled>
            <template #rightIcon>
              <t-switch :model-value="false" disabled />
            </template>
          </t-cell>
          <t-cell title="减少动效" description="低性能设备更顺滑">
            <template #rightIcon>
              <t-switch v-model="prefs.reduceMotion" />
            </template>
          </t-cell>
        </t-cell-group>

        <div class="group-title">
          通知
        </div>
        <t-cell-group inset>
          <t-cell title="消息通知（总开关）">
            <template #rightIcon>
              <t-switch v-model="prefs.notifyAll" />
            </template>
          </t-cell>
          <t-cell title="私信/会话" :disabled="!prefs.notifyAll">
            <template #rightIcon>
              <t-switch v-model="prefs.notifyChat" :disabled="!prefs.notifyAll" />
            </template>
          </t-cell>
          <t-cell title="任务进度/提醒" :disabled="!prefs.notifyAll">
            <template #rightIcon>
              <t-switch v-model="prefs.notifyTask" :disabled="!prefs.notifyAll" />
            </template>
          </t-cell>
          <t-cell title="勿扰模式（演示）">
            <template #rightIcon>
              <t-switch v-model="prefs.dndEnabled" />
            </template>
          </t-cell>
          <t-cell v-if="prefs.dndEnabled" title="勿扰时间（演示）" :note="`${prefs.dndStart} - ${prefs.dndEnd}`" />
        </t-cell-group>

        <div class="group-title">
          数据与存储
        </div>
        <t-cell-group inset>
          <t-cell title="省流模式（演示）" description="减少图片/自动加载">
            <template #rightIcon>
              <t-switch v-model="prefs.dataSaver" />
            </template>
          </t-cell>
          <t-cell title="清理缓存" note="演示" arrow @click="onClearCache" />
        </t-cell-group>

        <div class="group-title">
          开发与演示
        </div>
        <t-cell-group inset>
          <t-cell title="运行环境" :note="runtime?.demoModeLabel || '未读取到'">
            <template #description>
              {{ runtime?.demoDataHint || '当前环境会直接读取后端真实演示数据。' }}
            </template>
          </t-cell>
          <t-cell title="调试面板（演示）" description="开发时可打开 vconsole/eruda">
            <template #rightIcon>
              <t-switch v-model="prefs.showDevTools" />
            </template>
          </t-cell>
          <t-cell title="版本信息" :note="versionText" />
        </t-cell-group>
      </div>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.settings-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}
.content {
  padding: 12px;
  display: grid;
  gap: 14px;
  overflow-x: hidden;
}
.group-title {
  font-size: 13px;
  font-weight: 900;
  color: color-mix(in srgb, var(--m-color-text), #64748b 18%);
  padding: 0 4px;
}
.mode {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}
.mode-block {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
  row-gap: 8px;
  overflow-wrap: anywhere;
}

:global(.dark) :deep(.t-navbar) {
  background: #0f172a;
  color: #f3f4f6;
}
:global(.dark) :deep(.t-navbar__title) {
  color: #f3f4f6;
}
:global(.dark) :deep(.t-cell-group) {
  background: rgba(17, 24, 39, 0.58);
  border: 1px solid rgba(148, 163, 184, 0.18);
}
:global(.dark) :deep(.t-cell) {
  background: rgba(17, 24, 39, 0.58);
  color: #e5e7eb;
}
:global(.dark) :deep(.t-cell__description),
:global(.dark) :deep(.t-cell__note) {
  color: #9ca3af;
}
:global(.dark) :deep(.t-radio) {
  color: #e5e7eb;
}

:global(.m-a11y-large) .group-title {
  font-size: 15px;
}
:global(.m-a11y-large) :deep(.t-cell__title-text) {
  font-size: 16px;
}
:global(.m-a11y-large) :deep(.t-cell__description),
:global(.m-a11y-large) :deep(.t-cell__note) {
  font-size: 13px;
}
:global(.m-a11y-large) :deep(.t-navbar__title) {
  font-size: 19px;
}
:global(.m-a11y-large) .mode-block {
  gap: 10px;
}
:global(.m-a11y-large) :deep(.t-cell__title-text) {
  white-space: normal;
  line-height: 1.35;
}
:global(.m-a11y-large) :deep(.t-cell__right-icon) {
  flex-shrink: 0;
}
</style>

