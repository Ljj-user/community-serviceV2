<script setup lang="ts">
import { storeToRefs } from 'pinia'

const store = useProfileStore()
const { userProfile } = storeToRefs(store)
const { t } = useI18n()

const roleLabel = computed(() => {
  const r = Number(userProfile.value?.role)
  const id = Number(userProfile.value?.identityType)
  if (r === 1)
    return t('role.superAdmin')
  if (r === 2)
    return t('role.communityAdmin')
  if (r === 3) {
    if (id === 2)
      return t('role.volunteer')
    return t('role.resident')
  }
  return t('role.unknown')
})

const subtitle = computed(() => {
  const loc = (userProfile.value?.location || '').trim()
  return loc ? `${roleLabel.value} | ${loc}` : roleLabel.value
})
</script>

<template>
  <div class="profile-header flex flex-col items-center pb-8">
    <NAvatar class="my-3" round :size="75" :src="userProfile.avatar" alt="avatar">
      <span v-if="!userProfile.avatar">
        {{ userProfile.username?.charAt(0)?.toUpperCase() || '?' }}
      </span>
    </NAvatar>
    <div class="profile-section__info">
      <h4 text-center font-bold text-5>
        {{ userProfile.username }}
      </h4>
      <p font-light text-center>
        {{ subtitle }}
      </p>
    </div>
  </div>
</template>
