<script setup lang="ts">
import AccountService from '~/services/account.service'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const isVolunteer = computed(() => route.query.identity === 'volunteer')
const saving = ref(false)
const form = ref({
  skillTags: [] as string[],
  preferredFeatures: [] as string[],
  intentNote: '',
})
const skillOptions = ['助老陪伴', '家政清洁', '跑腿代办', '健康陪诊', '心理支持']
const featureOptions = ['发布求助', '接单服务', '社区发帖', '以物易物', '活动报名']

async function goProfile() {
  saving.value = true
  try {
    const res = await AccountService.submitOnboarding({
      skillTags: form.value.skillTags,
      preferredFeatures: form.value.preferredFeatures,
      intentNote: form.value.intentNote,
    })
    if (res.code !== 200) {
      message.error(res.message || '保存引导信息失败')
      return
    }
    message.success('引导信息已保存')
    router.push('/Account/profile')
  }
  catch (e: any) {
    message.error(e?.message || '保存失败')
  }
  finally {
    saving.value = false
  }
}
</script>

<route lang="yaml">
meta:
  title: registerWelcome
  layout: auth
  authRequired: true
</route>

<template>
  <div class="min-h-[70vh] flex flex-col items-center justify-center px-4 py-10">
    <div class="max-w-lg w-full rounded-2xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-800 shadow-lg p-8 md:p-10 text-center">
      <div class="text-2xl md:text-3xl font-semibold mb-4 leading-snug">
        {{ isVolunteer ? t('register.welcomeVolunteerTitle') : t('register.welcomeResidentTitle') }}
      </div>
      <p class="text-lg md:text-xl text-slate-600 dark:text-slate-300 mb-8 leading-relaxed">
        {{ isVolunteer ? t('register.welcomeVolunteerHint') : t('register.welcomeResidentHint') }}
      </p>
      <div class="space-y-4 text-left mb-8">
        <div>
          <div class="text-sm text-slate-500 mb-2">你具备哪些技能？（可多选）</div>
          <n-checkbox-group v-model:value="form.skillTags">
            <n-space>
              <n-checkbox v-for="item in skillOptions" :key="item" :value="item">
                {{ item }}
              </n-checkbox>
            </n-space>
          </n-checkbox-group>
        </div>
        <div>
          <div class="text-sm text-slate-500 mb-2">你主要想体验哪些功能？（可多选）</div>
          <n-checkbox-group v-model:value="form.preferredFeatures">
            <n-space>
              <n-checkbox v-for="item in featureOptions" :key="item" :value="item">
                {{ item }}
              </n-checkbox>
            </n-space>
          </n-checkbox-group>
        </div>
        <div>
          <div class="text-sm text-slate-500 mb-2">补充说明</div>
          <n-input v-model:value="form.intentNote" type="textarea" placeholder="比如希望重点做长期陪伴服务" />
        </div>
      </div>
      <n-button type="primary" size="large" class="!text-lg !px-10 !h-12" :loading="saving" @click="goProfile">
        保存并进入个人中心
      </n-button>
    </div>
  </div>
</template>
