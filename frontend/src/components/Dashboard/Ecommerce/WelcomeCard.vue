<script setup lang="ts">
const { t } = useI18n()
const profileStore = useProfileStore()

const daysInCommunity = computed(() => {
  const createdAt = profileStore.userProfile?.createdAt
  if (!createdAt) return 1
  const created = new Date(createdAt)
  if (Number.isNaN(created.getTime())) return 1
  const now = new Date()
  const diffMs = now.getTime() - created.getTime()
  // 从注册当天按第 1 天开始计数（当天显示 1 天）
  return Math.max(1, Math.floor(diffMs / (1000 * 60 * 60 * 24)) + 1)
})

onMounted(() => {
  // 每次进入欢迎卡都刷新一次资料，确保 createdAt 可用
  profileStore.loadUserProfile()
})
</script>

<template>
  <div class="w-full">
    <Card class="welcome-card">
      <div class="flex flex-row justify-between">
        <div class="flex flex-row md:pt-4">
          <span class="text-12 me-2">👋 </span>
          <div>
            <h3 class="text-lg font-bold">
              {{ t('dashboard.welcome.title') }}
            </h3>

            <p class="description">
              {{ t('dashboard.welcome.greeting_message') }}
            </p>
            <p class="description pt-7">
              你已来到互助社区
              <b class="inline-block min-w-12 px-1 text-size-lg">
                <n-number-animation show-separator :from="0" :to="daysInCommunity" />
              </b>
              天
            </p>
          </div>
        </div>
        <img
          src="@/assets/images/3d-female-character-waving.png"
          width="120px"
          class="hidden md:block -mt-16 -mb-2 me-4 cursor-pointer"
        >
      </div>
    </Card>
  </div>
</template>

<style scoped>
.confetti-container
{
  position: absolute;
  left:0;
  right:0;
  bottom:0;
  z-index: 10;
  height: 250px;
  width: 100%;
}

.description{
  font-size: .99rem;
}

.welcome-card {
  min-height: 180px;
}
</style>
