<route lang="yaml">
meta:
  title: userResident
  layout: default
  breadcrumb:
    - residentServicesGroup
    - userResident
</route>

<script setup lang="ts">
const router = useRouter()

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-2xl font-semibold mb-2">
      居民工作台
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">
      您可以在这里发布需求、查看办理进度、评价志愿服务，并接收社区公告与通知。
    </p>

    <!-- 顶部概览：仅保留欢迎卡，独占一行 -->
    <div class="flex flex-wrap w-full mb-4">
      <div class="h-auto flex flex-col justify-end relative w-full p-2">
        <WelcomeCard />
      </div>
    </div>

    <!-- 居民常用功能 -->
    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <Card title="发布服务需求" class="cursor-pointer" @click="go('/request/create')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          填写服务类型、时间和地点，由社区审核后发布给志愿者认领。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" type="primary" @click.stop="go('/request/create')">
            去发布
          </n-button>
        </div>
      </Card>

      <Card title="我的需求进度" class="cursor-pointer" @click="go('/request/my')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          查看自己提交的所有需求，跟踪审核、认领与完成状态。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/request/my')">
            查看列表
          </n-button>
        </div>
      </Card>

      <Card title="服务评价与反馈" class="cursor-pointer" @click="go('/request/evaluations')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          对已经完成的服务进行星级评价和文字反馈，帮助提升服务质量。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/request/evaluations')">
            去评价
          </n-button>
        </div>
      </Card>

      <Card title="社区公告与通知" class="cursor-pointer" @click="go('/user/announcements')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          浏览社区发布的活动预告、政策宣导及紧急提醒信息。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/user/announcements')">
            查看公告
          </n-button>
        </div>
      </Card>

      <Card title="个人资料与偏好" class="cursor-pointer" @click="go('/account/profile')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          管理个人基本信息、联系方式等，保障服务对接的准确性。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/account/profile')">
            编辑资料
          </n-button>
        </div>
      </Card>
    </div>
  </div>
</template>

