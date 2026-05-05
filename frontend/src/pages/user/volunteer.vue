<route lang="yaml">
meta:
  title: userVolunteer
  layout: default
  breadcrumb:
    - volunteerServicesGroup
    - userVolunteer
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
      志愿者工作台
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">
      您可以在这里浏览可认领的服务、管理个人服务记录，并查看居民对您的评价。
    </p>

    <!-- 顶部概览：仅保留欢迎卡，独占一行 -->
    <div class="flex flex-wrap w-full mb-4">
      <div class="h-auto flex flex-col justify-end relative w-full p-2">
        <WelcomeCard />
      </div>
    </div>

    <!-- 志愿者常用功能 -->
    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <Card title="可认领服务列表" class="cursor-pointer" @click="go('/service/market')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          按服务类型、时间和距离筛选合适的服务需求，自主选择并认领。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" type="primary" @click.stop="go('/service/market')">
            去认领
          </n-button>
        </div>
      </Card>

      <Card title="我的服务记录" class="cursor-pointer" @click="go('/service/my-records')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          查看自己认领和完成的服务记录，按时间与服务类型统计服务时长。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/service/my-records')">
            查看记录
          </n-button>
        </div>
      </Card>

      <Card title="居民评价与反馈" class="cursor-pointer" @click="go('/service/my-evaluations')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          查看居民对您服务的星级评价和文字反馈，了解改进方向。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/service/my-evaluations')">
            查看评价
          </n-button>
        </div>
      </Card>

      <Card title="社区公告与活动" class="cursor-pointer" @click="go('/user/announcements')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          浏览社区活动预告与重要通知，优先了解需要志愿者参与的事项。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/user/announcements')">
            查看公告
          </n-button>
        </div>
      </Card>

      <Card title="个人资料与服务意向" class="cursor-pointer" @click="go('/account/profile')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          完善服务特长、可服务时间等信息，帮助系统更好地为您匹配需求。
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
