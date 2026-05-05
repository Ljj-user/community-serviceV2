<route lang="yaml">
meta:
  title: request-create
  layout: default
  breadcrumb:
    - residentServicesGroup
    - request-create
</route>

<script setup lang="ts">
import type { FormInst } from 'naive-ui'
import { serviceRequestCreate, type ServiceRequestCreateDTO } from '~/api/serviceRequests'
import profileService from '~/services/profile.service'

const router = useRouter()
const message = useMessage()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const profileLoading = ref(false)
const currentCommunityText = ref('未绑定社区')

const serviceTypeOptions = [
  { label: '助老服务（陪护 / 陪诊）', value: '助老服务（陪护 / 陪诊）' },
  { label: '代办服务（买菜 / 取药）', value: '代办服务（买菜 / 取药）' },
  { label: '家政清洁', value: '家政清洁' },
  { label: '心理陪伴 / 聊天', value: '心理陪伴 / 聊天' },
  { label: '应急帮助（紧急求助）', value: '应急帮助（紧急求助）' },
  { label: '社区活动支持', value: '社区活动支持' },
]

const urgencyOptions = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '紧急', value: 4 },
]

const specialTagOptions = [
  { label: '独居老人', value: '独居老人' },
  { label: '残障', value: '残障' },
  { label: '行动不便', value: '行动不便' },
  { label: '高龄', value: '高龄' },
  { label: '失能', value: '失能' },
  { label: '其他', value: '其他' },
]

const relationOptions = [
  { label: '子女', value: '子女' },
  { label: '配偶', value: '配偶' },
  { label: '邻居', value: '邻居' },
  { label: '亲戚', value: '亲戚' },
  { label: '其他', value: '其他' },
]

const form = reactive<ServiceRequestCreateDTO & { expectedTime: number | null }>({
  serviceType: '',
  serviceAddress: '',
  description: '',
  expectedTime: null,
  urgencyLevel: 2,
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelation: '',
  specialTags: [],
})

const rules = {
  serviceType: { required: true, message: '请选择服务类型', trigger: ['change', 'blur'] },
  serviceAddress: { required: true, message: '请输入服务地址', trigger: ['input', 'blur'] },
  urgencyLevel: { required: true, type: 'number', message: '请选择紧急程度', trigger: ['change'] },
}

function toLocalDateTime(ts: number | null): string | undefined {
  if (!ts) return undefined
  const d = new Date(ts)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: ServiceRequestCreateDTO = {
      serviceType: form.serviceType,
      serviceAddress: form.serviceAddress,
      description: form.description || undefined,
      expectedTime: form.expectedTime ? toLocalDateTime(form.expectedTime) : undefined,
      urgencyLevel: form.urgencyLevel,
      emergencyContactName: form.emergencyContactName || undefined,
      emergencyContactPhone: form.emergencyContactPhone || undefined,
      emergencyContactRelation: form.emergencyContactRelation || undefined,
      specialTags: form.specialTags?.length ? form.specialTags : undefined,
    }
    const res = await serviceRequestCreate(payload)
    if (res.code !== 200) {
      message.error(res.message || '发布失败')
      return
    }
    message.success(res.message || '需求发布成功，等待社区审核')
    router.push('/request/my')
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function loadCommunityHint() {
  profileLoading.value = true
  try {
    const profile = await profileService.getUserProfile()
    if (!profile.communityId) {
      currentCommunityText.value = '未绑定社区（请先到个人设置选择社区）'
      return
    }
    currentCommunityText.value = `社区ID：${profile.communityId}`
  } catch {
    currentCommunityText.value = '社区信息加载失败'
  } finally {
    profileLoading.value = false
  }
}

function handleReset() {
  form.serviceType = ''
  form.serviceAddress = ''
  form.description = ''
  form.expectedTime = null
  form.urgencyLevel = 2
  form.emergencyContactName = ''
  form.emergencyContactPhone = ''
  form.emergencyContactRelation = ''
  form.specialTags = []
}

onMounted(() => {
  loadCommunityHint()
})
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-semibold">
      发布服务需求
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      填写服务类型、地址与期望时间，提交后由社区管理员审核，通过后将公开发布给志愿者认领。
    </p>

    <Card>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
        class="max-w-2xl"
      >
        <n-form-item label="服务类型" path="serviceType" required>
          <n-select
            v-model:value="form.serviceType"
            :options="serviceTypeOptions"
            placeholder="请选择服务类型"
          />
        </n-form-item>

        <n-form-item label="服务地址" path="serviceAddress" required>
          <n-input
            v-model:value="form.serviceAddress"
            type="text"
            placeholder="请输入详细服务地址"
            maxlength="255"
            show-count
          />
        </n-form-item>

        <n-form-item label="所属街区（自动绑定当前账号社区）">
          <n-input :value="profileLoading ? '社区信息加载中...' : currentCommunityText" readonly />
        </n-form-item>

        <n-form-item label="需求描述">
          <n-input
            v-model:value="form.description"
            type="textarea"
            placeholder="请补充需求说明，如服务内容、注意事项等"
            :rows="4"
            maxlength="500"
            show-count
          />
        </n-form-item>

        <n-form-item label="期望服务时间">
          <n-date-picker
            v-model:value="form.expectedTime"
            type="datetime"
            placeholder="请选择期望服务时间"
            clearable
            class="w-full"
          />
        </n-form-item>

        <n-form-item label="紧急程度" path="urgencyLevel" required>
          <n-select
            v-model:value="form.urgencyLevel"
            :options="urgencyOptions"
            placeholder="请选择紧急程度"
          />
        </n-form-item>

        <n-form-item label="特殊人群/需求标签">
          <n-select
            v-model:value="form.specialTags"
            :options="specialTagOptions"
            multiple
            placeholder="可多选，如独居老人、残障等"
          />
        </n-form-item>

        <n-divider title-placement="left">
          紧急联系人（选填）
        </n-divider>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <n-form-item label="联系人姓名">
            <n-input
              v-model:value="form.emergencyContactName"
              placeholder="紧急联系人姓名"
              clearable
            />
          </n-form-item>
          <n-form-item label="联系电话">
            <n-input
              v-model:value="form.emergencyContactPhone"
              placeholder="紧急联系人电话"
              clearable
            />
          </n-form-item>
          <n-form-item label="与接受服务者关系">
            <n-select
              v-model:value="form.emergencyContactRelation"
              :options="relationOptions"
              placeholder="如子女、邻居等"
              clearable
            />
          </n-form-item>
        </div>

        <div class="flex gap-3 pt-4">
          <n-button type="primary" :loading="submitting" @click="handleSubmit">
            提交需求
          </n-button>
          <n-button @click="handleReset">
            重置
          </n-button>
          <n-button quaternary @click="router.push('/request/my')">
            返回我的需求
          </n-button>
        </div>
      </n-form>
    </Card>
  </div>
</template>
