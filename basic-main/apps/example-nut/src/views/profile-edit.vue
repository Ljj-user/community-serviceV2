<script setup lang="ts">
import { toast } from 'vue-sonner'
import { getMyProfile, updateMyProfile, uploadMyAvatar } from '@/api/modules/profile'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'

definePage({
  name: 'profile-edit',
  meta: {
    title: '完善资料',
    auth: true,
  },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const localAvatarPreview = ref<string>('')

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  gender: 0 as 0 | 1 | 2,
  identityTag: '',
  address: '',
})
const selectedSkills = ref<string[]>([])
const customSkill = ref('')

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

const identityTagOptions = [
  { label: '普通居民', value: '普通居民' },
  { label: '活力老人', value: '活力老人' },
  { label: '孤寡老人', value: '孤寡老人' },
  { label: '残疾人士', value: '残疾人士' },
  { label: '临时困难', value: '临时困难' },
]

const skillTemplates = ['陪诊', '助老照护', '跑腿代办', '家电维修', '心理陪伴']

const currentAvatarUrl = computed(() => localAvatarPreview.value || appAuthStore.user?.avatarUrl || '')
const communityText = computed(() => appAuthStore.user?.communityName || '未绑定社区')

function parseSkillTags(raw?: string) {
  if (!raw) return [] as string[]
  const s = raw.trim()
  if (!s) return [] as string[]
  try {
    const arr = JSON.parse(s)
    if (Array.isArray(arr)) return arr.map(x => String(x).trim()).filter(Boolean)
  }
  catch {}
  return s.split(',').map(x => x.trim()).filter(Boolean)
}

function skillTagsToPayload() {
  const fixed = selectedSkills.value.filter(x => skillTemplates.includes(x))
  const extra = customSkill.value.trim()
  const merged = Array.from(new Set(extra ? [...fixed, extra] : fixed))
  return JSON.stringify(merged)
}

async function load() {
  loading.value = true
  try {
    const res = await getMyProfile()
    form.username = res.data.username || ''
    form.realName = res.data.realName || ''
    form.phone = res.data.phone || ''
    form.email = res.data.email || ''
    form.gender = (res.data.gender ?? 0) as any
    form.identityTag = res.data.identityTag || ''
    form.address = res.data.address || ''
    const tags = parseSkillTags(res.data.skillTags)
    selectedSkills.value = tags.filter(x => skillTemplates.includes(x))
    customSkill.value = tags.find(x => !skillTemplates.includes(x)) || ''
    await appAuthStore.hydrateUser()
  }
  catch (e: any) {
    toast.error(e?.message || '加载失败')
  }
  finally {
    loading.value = false
  }
}

function onPickAvatar(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) return toast.error('请选择图片文件')
  if (localAvatarPreview.value) URL.revokeObjectURL(localAvatarPreview.value)
  localAvatarPreview.value = URL.createObjectURL(file)
  void uploadAvatar(file)
}

async function uploadAvatar(file: File) {
  uploading.value = true
  try {
    await uploadMyAvatar(file)
    toast.success('头像已更新')
    await appAuthStore.hydrateUser()
    if (localAvatarPreview.value) {
      URL.revokeObjectURL(localAvatarPreview.value)
      localAvatarPreview.value = ''
    }
  }
  catch (e: any) {
    toast.error(e?.message || '头像上传失败')
  }
  finally {
    uploading.value = false
  }
}

async function save() {
  if (saving.value) return
  saving.value = true
  try {
    await updateMyProfile({
      realName: form.realName || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      gender: form.gender,
      identityTag: form.identityTag || undefined,
      address: form.address || undefined,
      skillTags: skillTagsToPayload(),
    })
    toast.success('资料已保存')
    await appAuthStore.hydrateUser()
    router.back()
  }
  catch (e: any) {
    toast.error(e?.message || '保存失败')
  }
  finally {
    saving.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (localAvatarPreview.value) URL.revokeObjectURL(localAvatarPreview.value)
})
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <ThreeSectionPage page-class="page m-mobile-page-bg" content-class="content">
      <template #header>
        <header class="top">
          <button type="button" class="back" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <h2>完善资料</h2>
          <div class="right" />
        </header>
      </template>

      <div v-if="loading" class="status">加载中…</div>

      <template v-else>
        <section class="hero-card">
          <img
            class="hero-bg"
            src="https://images.pexels.com/photos/6647034/pexels-photo-6647034.jpeg?auto=compress&cs=tinysrgb&w=1200"
            alt="完善资料（亚洲人）"
          >
          <div class="hero-mask" />
          <div class="hero-inner">
            <div class="avatar-row">
              <div class="avatar">
                <img v-if="currentAvatarUrl" :src="currentAvatarUrl" alt="我的头像">
                <div v-else class="avatar-fallback">
                  <FmIcon name="i-carbon:user-avatar-filled-alt" />
                </div>
              </div>
              <div class="avatar-meta">
                <div class="name">{{ form.realName || form.username || '用户' }}</div>
                <div class="sub">{{ communityText }}</div>
              </div>
              <label class="upload-btn" :class="{ disabled: uploading }">
                <input class="hidden" type="file" accept="image/*" :disabled="uploading" @change="onPickAvatar">
                {{ uploading ? '上传中…' : '更换头像' }}
              </label>
            </div>
          </div>
        </section>

        <section class="card">
          <div class="section-head">
            <h3>基础信息</h3>
            <span>写到能联系到你</span>
          </div>

          <div class="row">
            <div class="label">用户名</div>
            <div class="value">{{ form.username || '—' }}</div>
          </div>
          <div class="row">
            <div class="label">真实姓名</div>
            <FmInput v-model="form.realName" class="input" placeholder="例如：张阿姨" />
          </div>
          <div class="row">
            <div class="label">性别</div>
            <NutRadioGroup v-model="form.gender" direction="horizontal" class="chips">
              <NutRadio v-for="g in genderOptions" :key="g.value" :label="g.value">{{ g.label }}</NutRadio>
            </NutRadioGroup>
          </div>
          <div class="row">
            <div class="label">手机号</div>
            <FmInput v-model="form.phone" class="input" placeholder="11 位手机号" />
          </div>
          <div class="row">
            <div class="label">邮箱</div>
            <FmInput v-model="form.email" class="input" placeholder="用于通知" />
          </div>
          <div class="row">
            <div class="label">常住地址</div>
            <FmInput v-model="form.address" class="input" placeholder="例如：幸福小区 1 栋 101" />
          </div>
          <div class="row">
            <div class="label">居民标签</div>
            <div class="tag-grid">
              <button
                v-for="t in identityTagOptions"
                :key="t.value"
                type="button"
                class="tag"
                :class="{ active: form.identityTag === t.value }"
                @click="form.identityTag = t.value"
              >
                {{ t.label }}
              </button>
            </div>
          </div>
        </section>

        <section class="card">
          <div class="section-head">
            <h3>志愿者信息</h3>
            <span>有技能就写</span>
          </div>

          <div class="row row-top">
            <div class="label">志愿者技能</div>
            <div class="skills">
              <label v-for="s in skillTemplates" :key="s" class="skill-item">
                <input v-model="selectedSkills" :value="s" type="checkbox">
                <span>{{ s }}</span>
              </label>
              <FmInput v-model="customSkill" class="input" placeholder="其他技能（可选）" />
            </div>
          </div>
        </section>

        <div class="actions">
          <NutButton block type="primary" :loading="saving" class="save-btn" @click="save">
            保存资料
          </NutButton>
        </div>
      </template>
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; width: min(100vw, var(--m-device-max-width)); margin: 0 auto; }
.content { padding: 10px 12px 0; display: grid; gap: 12px; align-content: start; }
.top { display: grid; grid-template-columns: 34px 1fr 34px; align-items: center; gap: 10px; padding: 10px 12px 0; }
.top h2 { margin: 0; text-align: center; font-size: 18px; font-weight: 900; color: var(--m-color-text); }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.status { font-size: 13px; color: var(--m-color-subtext); text-align: center; padding: 28px 0; }

.hero-card { position: relative; border-radius: 20px; overflow: hidden; border: 1px solid #bbf7d0; height: 118px; }
.hero-bg { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.12), rgba(0,0,0,.62)); }
.hero-inner { position: absolute; inset: 0; padding: 12px; display: flex; align-items: flex-end; }

.avatar-row { width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; }
.avatar { width: 54px; height: 54px; border-radius: 999px; overflow: hidden; background: rgba(255,255,255,.2); border: 1px solid rgba(255,255,255,.45); }
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar-fallback { width: 100%; height: 100%; display: inline-flex; align-items: center; justify-content: center; font-size: 28px; color: rgba(255,255,255,.85); }
.avatar-meta { min-width: 0; color: #fff; }
.name { font-weight: 1000; font-size: 16px; line-height: 1.1; }
.sub { margin-top: 4px; font-size: 12px; opacity: .92; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.upload-btn { height: 34px; padding: 0 10px; border-radius: 12px; border: 1px solid rgba(255,255,255,.45); background: rgba(255,255,255,.14); color: #fff; font-weight: 900; font-size: 12px; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; }
.upload-btn.disabled { opacity: .6; pointer-events: none; }
.hidden { display: none; }

.card {
  border-radius: 20px;
  background: color-mix(in srgb, #ffffff 86%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px) saturate(170%);
  -webkit-backdrop-filter: blur(12px) saturate(170%);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.9) inset, 0 10px 22px rgba(15, 23, 42, 0.08);
  padding: 12px;
  display: grid;
  gap: 10px;
}
.section-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
.section-head h3 { margin: 0; font-size: 18px; font-weight: 900; color: #111827; }
.section-head span { font-size: 12px; font-weight: 800; color: #16a34a; }

.row { display: grid; grid-template-columns: 74px 1fr; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px dashed #e5e7eb; }
.row:first-of-type { border-top: 0; padding-top: 6px; }
.row-top { align-items: start; }
.label { font-size: 12px; color: #64748b; font-weight: 900; }
.value { font-size: 13px; color: #111827; font-weight: 900; }
.input { width: 100%; }

.tag-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.tag {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}
.tag.active { border-color: #10b981; background: #ecfdf5; color: #047857; }

.skills { display: grid; gap: 10px; }
.skill-item { display: inline-flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 800; color: #334155; }

.actions { padding: 2px 2px 8px; }
.save-btn { border-radius: 16px !important; font-weight: 900 !important; }
</style>
