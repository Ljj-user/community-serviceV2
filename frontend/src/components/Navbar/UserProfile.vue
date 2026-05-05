<script setup lang="ts">
import {
  DoorArrowRight20Regular as LogoutIcon,
  Settings20Regular as SettingsIcon,
} from '@vicons/fluent'
import { storeToRefs } from 'pinia'

const { renderLabel, renderIcon } = useRender()
const profileStore = useProfileStore()
const { userProfile, avatarNonce } = storeToRefs(profileStore)
const { t } = useI18n()
onMounted(() => profileStore.loadUserProfile())
watch(
  () => userProfile.value?.avatar,
  (v) => {
    // 兜底：如果顶部挂载时资料尚未就绪，后续再补拉一次，避免长期停留在首字母占位
    if (!v)
      profileStore.loadUserProfile()
  },
  { immediate: true },
)
const avatarSrc = computed(() => {
  const raw = userProfile.value?.avatar || ''
  if (!raw) return ''
  const sep = raw.includes('?') ? '&' : '?'
  return `${raw}${sep}v=${avatarNonce.value || Date.now()}`
})
const avatarRenderKey = computed(() => `${avatarSrc.value}-${userProfile.value?.id || ''}`)
const items: any[] = [
  {
    icon: renderIcon(SettingsIcon),
    label: () => renderLabel(t('userMenu.profile'), '/account/profile'),
    key: 'options',
  },
  {
    icon: renderIcon(LogoutIcon),
    label: () => renderLabel(t('userMenu.logout'), '/account/login'),
    key: 'login',
  },
]
</script>

<template>
  <div class="flex items-center" v-bind="$attrs">
    <n-dropdown :options="items">
      <div :key="avatarRenderKey" class="avatar avatar-shell">
        <img v-if="avatarSrc" :src="avatarSrc" alt="avatar" class="avatar-img">
        <span v-else>{{ userProfile.username?.charAt(0)?.toUpperCase() || '?' }}</span>
      </div>
    </n-dropdown>
  </div>
</template>

<style lang="scss">
.username {
  font-size: 0.8rem;
  font-weight: bold;
}

.avatar {
  width: 33px;
  height: 33px;
}
.avatar-shell {
  border-radius: 999px;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  background: #e2e8f0;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.role {
  font-size: 0.7rem;
}

.p-tieredmenu .p-menuitem-active>.p-submenu-list {
  right: 100%;
  left: auto;
}

.rtl {

  .p-tieredmenu .p-menuitem-active>.p-submenu-list {
    right: auto;
    left: 100%;
  }
}
</style>
