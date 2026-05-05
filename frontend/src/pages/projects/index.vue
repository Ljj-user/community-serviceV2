<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 dark:from-slate-900 dark:to-slate-950">
    <div class="w-full max-w-md px-6 py-8 rounded-2xl shadow-lg bg-white/80 dark:bg-slate-900/80 backdrop-blur">
      <div class="mb-6 text-center">
        <h1 class="text-2xl font-semibold mb-1">
          社区公益服务对接管理平台
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400">
          请先登录后访问项目管理功能
        </p>
      </div>

      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
        @submit.prevent="handleSubmit"
      >
        <n-form-item path="username" label="用户名">
          <n-input
            v-model:value="form.username"
            placeholder="请输入用户名"
            clearable
          />
        </n-form-item>

        <n-form-item path="password" label="密码">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="mousedown"
            placeholder="请输入密码"
            clearable
          />
        </n-form-item>

        <div class="flex items-center justify-between mb-4">
          <n-checkbox v-model:checked="form.remember">
            记住我
          </n-checkbox>
          <span class="text-xs text-slate-500 dark:text-slate-400">
            忘记密码请联系管理员
          </span>
        </div>

        <n-button
          type="primary"
          class="w-full"
          size="large"
          :loading="loading"
          @click="handleSubmit"
        >
          登录
        </n-button>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
const router = useRouter()
const message = useMessage()

const formRef = ref()

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules = {
  username: {
    required: true,
    message: '请输入用户名',
    trigger: ['blur', 'input'],
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: ['blur', 'input'],
  },
}

const loading = ref(false)

async function handleSubmit() {
  if (!formRef.value)
    return

  try {
    await formRef.value.validate()
  }
  catch {
    return
  }

  loading.value = true
  try {
    // 这里调用你的后端登录接口，例如 /api/auth/login
    // 根据你的实际接口调整 URL 和参数
    await axios.post('/api/auth/login', {
      username: form.username,
      password: form.password,
      rememberMe: form.remember,
    })

    message.success('登录成功')
    // 登录成功后跳转到项目列表或首页
    router.push('/projects')
  }
  catch (error: any) {
    message.error(error?.response?.data?.message || '登录失败，请检查用户名或密码')
  }
  finally {
    loading.value = false
  }
}
</script>