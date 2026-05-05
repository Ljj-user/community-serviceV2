<script lang="ts" setup>
/** 侧边栏分组 key：面包屑中间级可点击回到该分组下的入口页 */
const GROUP_KEYS = new Set([
  'residentServicesGroup',
  'volunteerServicesGroup',
  'adminOpsGroup',
  'adminSystemGroup',
])

/** 非分组菜单 key 的上一级：可点击回到该模块默认页 */
const PARENT_FALLBACK_ROUTE: Record<string, string> = {
  settings: '/account/profile',
}

const route = useRoute()
const { t } = useI18n()
const accountStore = useAccountStore()

const userRole = computed(() => accountStore.user?.role)

/** 登录后「首页」统一为 /dashboard（多角色看板） */
const homeDashboardPath = computed(() => '/dashboard')

const crumbs = computed(() => {
  const raw = route.meta.breadcrumb
  if (!raw || !Array.isArray(raw))
    return [] as string[]
  return raw as string[]
})

function groupSectionPath(key: string): string | null {
  if (key === 'residentServicesGroup')
    return '/user/resident'
  if (key === 'volunteerServicesGroup')
    return '/user/volunteer'
  if (key === 'adminOpsGroup' || key === 'adminSystemGroup')
    return '/dashboard'
  return null
}

function labelFor(key: string) {
  return t(`menu.${key}`)
}
</script>

<template>
  <n-breadcrumb class="hidden md:block app-breadcrumb">
    <n-breadcrumb-item>
      <RouterLink :to="homeDashboardPath">
        {{ t('home') }}
      </RouterLink>
    </n-breadcrumb-item>
    <n-breadcrumb-item
      v-for="(key, index) in crumbs"
      :key="`${key}-${index}`"
    >
      <template v-if="index < crumbs.length - 1">
        <RouterLink
          v-if="GROUP_KEYS.has(key) && groupSectionPath(key)"
          :to="groupSectionPath(key)!"
        >
          {{ labelFor(key) }}
        </RouterLink>
        <RouterLink
          v-else-if="PARENT_FALLBACK_ROUTE[key]"
          :to="PARENT_FALLBACK_ROUTE[key]"
        >
          {{ labelFor(key) }}
        </RouterLink>
        <span v-else>
          {{ labelFor(key) }}
        </span>
      </template>
      <span v-else class="text-slate-600 dark:text-slate-300 font-medium">
        {{ labelFor(key) }}
      </span>
    </n-breadcrumb-item>
  </n-breadcrumb>
</template>

<style scoped>
.app-breadcrumb :deep(.n-breadcrumb-item) {
  font-size: 13px;
}
</style>
