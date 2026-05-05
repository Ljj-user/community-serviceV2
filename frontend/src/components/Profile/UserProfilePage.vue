<script setup lang="ts">
const store = useProfileStore()
const { t } = useI18n()

onMounted(() => {
  // 加载当前用户资料和通知设置
  store.loadUserProfile()
  store.loadSettings()
})
</script>

<template>
  <div>
    <ProfileHeader />

    <n-tabs type="line" animated placement="top" justify-content="center" style="height: '240px'">
      <n-tab-pane name="PersonalInfo" :tab="t('profile.personalInfo')">
        <ProfilePersonalInfo />
      </n-tab-pane>
      <n-tab-pane name="Notifications" :tab="t('profile.notifications')">
        <ProfileNotifications />
      </n-tab-pane>
      <n-tab-pane name="Security" :tab="t('profile.security')">
        <ProfileSecurity />
      </n-tab-pane>
    </n-tabs>
  </div>
</template>
