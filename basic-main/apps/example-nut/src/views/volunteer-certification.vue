<script setup lang="ts">
import { toast } from 'vue-sonner'
import {
  applyVolunteerProfile,
  getMyVolunteerProfile,
  type VolunteerProfileResponse,
} from '@/api/modules/profile'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'

definePage({
  name: 'volunteer-certification',
  meta: {
    title: '志愿者认证',
    auth: true,
  },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()

const loading = ref(false)
const saving = ref(false)
const profile = ref<VolunteerProfileResponse | null>(null)
const selectedSkills = ref<string[]>([])
const customSkill = ref('')
const form = reactive({
  idCardNo: '',
  serviceRadiusKm: 3,
  availableTime: '工作日晚上、周末白天',
})

const skillTemplates = ['陪诊', '助老照护', '跑腿代办', '家务清洁', '心理陪伴', '家电维修']

const statusMeta = computed(() => {
  const s = Number(profile.value?.certStatus ?? 0)
  if (s === 2) return { text: '已认证', tone: 'ok', desc: '你可以接取公益服务单。' }
  if (s === 1) return { text: '审核中', tone: 'wait', desc: '社区管理员会尽快审核。' }
  if (s === 3) return { text: '已驳回', tone: 'bad', desc: profile.value?.rejectReason || '请补充资料后重新提交。' }
  return { text: '未认证', tone: 'idle', desc: '提交后才能接单。' }
})

const canApply = computed(() => !!appAuthStore.user?.communityId)

function parseSkillTags(raw?: string[] | string) {
  if (Array.isArray(raw)) return raw.map(x => String(x).trim()).filter(Boolean)
  if (!raw) return [] as string[]
  try {
    const arr = JSON.parse(String(raw))
    if (Array.isArray(arr)) return arr.map(x => String(x).trim()).filter(Boolean)
  }
  catch {}
  return String(raw).split(',').map(x => x.trim()).filter(Boolean)
}

function toggleSkill(skill: string) {
  if (selectedSkills.value.includes(skill))
    selectedSkills.value = selectedSkills.value.filter(x => x !== skill)
  else
    selectedSkills.value = [...selectedSkills.value, skill]
}

function mergedSkills() {
  const extra = customSkill.value.trim()
  return Array.from(new Set(extra ? [...selectedSkills.value, extra] : selectedSkills.value))
}

async function load() {
  loading.value = true
  try {
    await appAuthStore.hydrateUser()
    const res = await getMyVolunteerProfile()
    profile.value = res.data
    if (res.data) {
      form.idCardNo = res.data.idCardNo || ''
      form.availableTime = res.data.availableTime || form.availableTime
      form.serviceRadiusKm = Number(res.data.serviceRadiusKm || 3)
      const tags = parseSkillTags(res.data.skillTags)
      selectedSkills.value = tags.filter(x => skillTemplates.includes(x))
      customSkill.value = tags.find(x => !skillTemplates.includes(x)) || ''
    }
  }
  catch (e: any) {
    toast.error(e?.message || '加载失败')
  }
  finally {
    loading.value = false
  }
}

async function submit() {
  if (!canApply.value) {
    toast.error('请先加入社区')
    router.push('/join-community')
    return
  }
  const skills = mergedSkills()
  if (!skills.length) return toast.error('至少选择一项技能')
  if (!form.availableTime.trim()) return toast.error('请填写可服务时间')

  saving.value = true
  try {
    await applyVolunteerProfile({
      idCardNo: form.idCardNo.trim() || undefined,
      skillTags: skills,
      serviceRadiusKm: Number(form.serviceRadiusKm || 3),
      availableTime: form.availableTime.trim(),
    })
    toast.success('认证申请已提交')
    await load()
  }
  catch (e: any) {
    toast.error(e?.message || '提交失败')
  }
  finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <ThreeSectionPage page-class="cert-page m-mobile-page-bg" content-class="cert-content">
      <template #header>
        <header class="top">
          <button type="button" class="back" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <h2>志愿者认证</h2>
          <div class="right" />
        </header>
      </template>

      <div v-if="loading" class="status">加载中…</div>
      <template v-else>
        <section class="hero-card">
          <img
            src="https://images.pexels.com/photos/6646918/pexels-photo-6646918.jpeg?auto=compress&cs=tinysrgb&w=1200"
            alt="社区志愿服务"
          >
          <div class="hero-mask" />
          <div class="hero-copy">
            <span class="pill" :class="statusMeta.tone">{{ statusMeta.text }}</span>
            <h3>{{ appAuthStore.user?.realName || appAuthStore.user?.username || '你好' }}</h3>
            <p>{{ statusMeta.desc }}</p>
          </div>
        </section>

        <section v-if="!canApply" class="notice-card">
          <FmIcon name="mdi:map-marker-alert-outline" />
          <div>
            <b>先绑定社区</b>
            <p>认证要归到社区下。</p>
          </div>
          <button type="button" @click="router.push('/join-community')">去绑定</button>
        </section>

        <section class="card">
          <div class="section-head">
            <h3>服务技能</h3>
            <span>选你真会的</span>
          </div>
          <div class="skill-grid">
            <button
              v-for="skill in skillTemplates"
              :key="skill"
              type="button"
              class="skill"
              :class="{ active: selectedSkills.includes(skill) }"
              @click="toggleSkill(skill)"
            >
              {{ skill }}
            </button>
          </div>
          <FmInput v-model="customSkill" class="input mt" placeholder="其他技能，例如：理发" />
        </section>

        <section class="card">
          <div class="section-head">
            <h3>认证资料</h3>
            <span>方便管理员审核</span>
          </div>
          <div class="row">
            <div class="label">证件号</div>
            <FmInput v-model="form.idCardNo" class="input" placeholder="可选，演示可不填" />
          </div>
          <div class="row">
            <div class="label">服务半径</div>
            <FmInput v-model="form.serviceRadiusKm" class="input" inputmode="decimal" placeholder="例如 3" />
          </div>
          <div class="row vertical">
            <div class="label">可服务时间</div>
            <textarea v-model="form.availableTime" class="textarea" rows="3" placeholder="例如：周末白天、工作日晚上" />
          </div>
        </section>

        <button type="button" class="submit-btn" :disabled="saving" @click="submit">
          {{ saving ? '提交中…' : '提交认证' }}
        </button>
      </template>
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.cert-page {
  min-height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #fbfcf8 0%, #f3f7f0 58%, #edf3ef 100%);
}
.cert-content {
  padding: 10px 12px 28px;
  display: grid;
  gap: 14px;
}
.top {
  height: 52px;
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  padding: 0 8px;
}
.back {
  border: 0;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #111827;
}
.top h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}
.hero-card {
  position: relative;
  height: 154px;
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
}
.hero-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(115deg, rgba(15, 23, 42, 0.72), rgba(15, 23, 42, 0.08));
}
.hero-copy {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 16px;
  color: #fff;
}
.pill {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
}
.pill.ok { background: rgba(22, 163, 74, 0.78); }
.pill.wait { background: rgba(217, 119, 6, 0.82); }
.pill.bad { background: rgba(220, 38, 38, 0.8); }
.hero-copy h3 {
  margin: 10px 0 4px;
  font-size: 22px;
  font-weight: 900;
}
.hero-copy p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.88);
}
.notice-card,
.card {
  border-radius: 20px;
  background: color-mix(in srgb, #ffffff 86%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px) saturate(170%);
  -webkit-backdrop-filter: blur(12px) saturate(170%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.86) inset,
    0 10px 22px rgba(15, 23, 42, 0.08);
  padding: 14px;
}
.notice-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
}
.notice-card :deep(svg) {
  font-size: 24px;
  color: #d97706;
}
.notice-card b {
  display: block;
  font-size: 14px;
}
.notice-card p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}
.notice-card button,
.submit-btn {
  border: 0;
  border-radius: 14px;
  background: #15803d;
  color: #fff;
  font-weight: 900;
}
.notice-card button {
  padding: 8px 12px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-head h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 900;
}
.section-head span,
.label {
  font-size: 12px;
  color: #64748b;
}
.skill-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.skill {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  padding: 9px 12px;
  background: rgba(255, 255, 255, 0.74);
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}
.skill.active {
  border-color: rgba(21, 128, 61, 0.3);
  background: #ecfdf3;
  color: #15803d;
}
.mt { margin-top: 10px; }
.row {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.row.vertical {
  grid-template-columns: 1fr;
  align-items: start;
}
.input {
  width: 100%;
}
.textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  padding: 11px 12px;
  background: rgba(255, 255, 255, 0.78);
  color: #111827;
  font-size: 14px;
  outline: none;
  resize: vertical;
}
.submit-btn {
  width: 100%;
  min-height: 46px;
  font-size: 16px;
  box-shadow: 0 12px 24px rgba(21, 128, 61, 0.18);
}
.submit-btn:disabled {
  opacity: 0.62;
}
.status {
  padding: 20px 0;
  text-align: center;
  color: #64748b;
}
:global(.dark) .cert-page {
  background: #111827;
}
:global(.dark) .card,
:global(.dark) .notice-card {
  background: rgba(31, 41, 55, 0.68);
  border-color: rgba(148, 163, 184, 0.22);
}
:global(.dark) .top h2,
:global(.dark) .section-head h3,
:global(.dark) .notice-card b {
  color: #f8fafc;
}
:global(.dark) .label,
:global(.dark) .section-head span,
:global(.dark) .notice-card p {
  color: #94a3b8;
}
:global(.dark) .skill,
:global(.dark) .textarea,
:global(.dark) .back {
  background: rgba(17, 24, 39, 0.72);
  border-color: rgba(148, 163, 184, 0.22);
  color: #e5e7eb;
}
</style>
