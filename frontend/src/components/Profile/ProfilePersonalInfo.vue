<script lang="ts" setup>
import { storeToRefs } from 'pinia'
import type { UploadFileInfo } from 'naive-ui'
import type { Profile } from '~/models/Profile'
import ProfileService from '~/services/profile.service'
import tokenService from '~/common/api/token.service'

const { t } = useI18n()
const message = useMessage()
const store = useProfileStore()
const { userProfile, isLoading } = storeToRefs(store)

const editing = ref(false)
const draft = ref<Profile>({} as Profile)

const isVolunteer = computed(
  () => Number(userProfile.value?.identityType) === 2,
)
const isAdminAccount = computed(() => {
  const r = Number(userProfile.value?.role ?? 0)
  return r === 1 || r === 2
})

function cloneProfile(p: Profile): Profile {
  return JSON.parse(JSON.stringify(p))
}

watch(
  userProfile,
  (p) => {
    if (!editing.value && p?.id)
      draft.value = cloneProfile(p)
  },
  { deep: true, immediate: true },
)

function startEdit() {
  draft.value = cloneProfile(userProfile.value)
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  draft.value = cloneProfile(userProfile.value)
}

async function saveEdit() {
  isLoading.value = true
  try {
    const updated = await ProfileService.updateUserProfile(draft.value)
    userProfile.value = updated
    editing.value = false
  } finally {
    isLoading.value = false
  }
}

const display = computed(() => (editing.value ? draft.value : userProfile.value))

const uploadHeaders = computed(() => {
  const token = tokenService.getLocalAccessToken()
  return token
    ? {
        Authorization: `Bearer ${token}`,
      }
    : {}
})

async function onAvatarUploaded(options: { event?: ProgressEvent }) {
  let applied = false
  const text = options.event?.target ? (options.event.target as XMLHttpRequest).responseText : ''
  if (text) {
    try {
      const parsed = JSON.parse(text) as { code?: number, message?: string, data?: { avatarUrl?: string } }
      if (parsed.code && parsed.code !== 200) {
        message.error(parsed.message || '头像上传失败')
        return
      }
      const rawAvatar = parsed.data?.avatarUrl
      if (rawAvatar) {
        let normalized = rawAvatar
        if (rawAvatar.startsWith('/static/'))
          normalized = `/api${rawAvatar}`
        else if (/^https?:\/\/localhost:8080\/static\//i.test(rawAvatar))
          normalized = rawAvatar.replace(/^https?:\/\/localhost:8080/i, '/api')
        else if (/^https?:\/\/127\.0\.0\.1:8080\/static\//i.test(rawAvatar))
          normalized = rawAvatar.replace(/^https?:\/\/127\.0\.0\.1:8080/i, '/api')
        userProfile.value.avatar = normalized
        store.bumpAvatarNonce()
        applied = true
      }
    }
    catch {}
  }
  // 某些浏览器上传组件不一定回传可解析 responseText，兜底再拉一次 profile。
  await store.loadUserProfile()
  if (!applied && !userProfile.value.avatar) {
    const latest = await ProfileService.getUserProfile()
    userProfile.value = latest
  }
  store.bumpAvatarNonce()
  if (editing.value)
    draft.value = cloneProfile(userProfile.value)
  message.success('头像上传成功')
}

function onAvatarUploadError(options: { file: UploadFileInfo, event?: ProgressEvent }) {
  const text = options.event?.target ? (options.event.target as XMLHttpRequest).responseText : ''
  if (text) {
    try {
      const parsed = JSON.parse(text) as { message?: string }
      message.error(parsed.message || '头像上传失败')
      return
    }
    catch {}
  }
  message.error('头像上传失败，请检查文件大小/格式或登录状态')
}

const genderSelectOptions = computed(() => [
  { label: t('profile.genderUnknown'), value: 0 },
  { label: t('profile.genderMale'), value: 1 },
  { label: t('profile.genderFemale'), value: 2 },
])
</script>

<template>
  <section>
    <div class="p-8 w-full md:w-3/4 mx-auto">
      <p class="text-base text-slate-500 dark:text-slate-400 mb-4">
        {{ t('profile.readOnlyHint') }}
      </p>
      <n-alert v-if="isAdminAccount" type="info" class="mb-4">
        管理员/超管账号的用户名、身份标签、所属社区等关键字段为固定项，不能在此页面修改。
      </n-alert>

      <n-form size="large" :model="display" @submit.prevent>
        <n-grid :span="24" :x-gap="42" :y-gap="18">
          <n-form-item-gi :span="24" class="mb-1" path="username" :label="t('profile.username')">
            <n-input
              v-model:value="draft.username"
              :disabled="!editing || isAdminAccount"
              :placeholder="t('profile.username')"
            />
          </n-form-item-gi>

          <n-form-item-gi :span="24" class="mb-1" path="avatar" :label="t('profile.avatarLabel')">
            <div class="flex items-center gap-4">
              <NAvatar round :size="56" :src="display.avatar">
                <span v-if="!display.avatar">
                  {{ display.username?.charAt(0)?.toUpperCase() || '?' }}
                </span>
              </NAvatar>
              <n-upload
                v-if="editing"
                accept="image/*"
                :max="1"
                :show-file-list="false"
                :headers="uploadHeaders"
                action="/api/user/avatar"
                name="file"
                @finish="onAvatarUploaded"
                @error="onAvatarUploadError"
              >
                <n-button type="primary" dashed>
                  {{ t('profile.uploadAvatar') }}
                </n-button>
              </n-upload>
            </div>
          </n-form-item-gi>

          <n-form-item-gi :span="12" class="mb-1" path="realName" :label="t('profile.realName')">
            <n-input
              v-model:value="draft.realName"
              :disabled="!editing"
              :placeholder="t('profile.realName')"
            />
          </n-form-item-gi>

          <n-form-item-gi :span="12" class="mb-1" path="phone" :label="t('profile.phone')">
            <n-input
              v-model:value="draft.phone"
              :disabled="!editing"
              type="text"
              :placeholder="t('profile.phone')"
            />
          </n-form-item-gi>

          <n-form-item-gi :span="12" class="mb-1" path="email" :label="t('profile.email')">
            <n-input
              v-model:value="draft.email"
              :disabled="!editing"
              type="text"
              :placeholder="t('profile.email')"
            />
          </n-form-item-gi>

          <n-form-item-gi :span="24" class="mb-1" path="location" :label="t('profile.homeAddress')">
            <n-input
              v-model:value="draft.location"
              :disabled="!editing"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              :placeholder="t('profile.homeAddress')"
            />
          </n-form-item-gi>

          <n-form-item-gi :span="12" class="mb-1" path="gender" :label="t('profile.gender')">
            <n-select
              v-model:value="draft.gender"
              :disabled="!editing"
              :options="genderSelectOptions"
              :placeholder="t('profile.gender')"
            />
          </n-form-item-gi>

          <n-form-item-gi
            v-if="isVolunteer"
            :span="24"
            class="mb-1"
            path="skillTags"
            :label="t('profile.skillTags')"
          >
            <VolunteerSkillTagEditor
              :model-value="draft.skillTags ?? []"
              :disabled="!editing"
              @update:model-value="(v) => { draft.skillTags = v }"
            />
          </n-form-item-gi>

          <n-gi :span="24">
            <div flex justify-start gap-3>
              <template v-if="!editing">
                <n-button size="large" type="primary" @click="startEdit">
                  {{ t('profile.edit') }}
                </n-button>
              </template>
              <template v-else>
                <n-button size="large" @click="cancelEdit">
                  {{ t('profile.cancelEdit') }}
                </n-button>
                <n-button
                  size="large"
                  type="primary"
                  :loading="isLoading"
                  :disabled="!editing"
                  @click="saveEdit"
                >
                  {{ t('profile.saveChanges') }}
                </n-button>
              </template>
            </div>
          </n-gi>
        </n-grid>
      </n-form>
    </div>
  </section>
</template>
