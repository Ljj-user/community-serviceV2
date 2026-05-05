<route lang="yaml">
meta:
  title: admin-dashboard
  layout: default
  breadcrumb:
    - adminOpsGroup
    - admin-dashboard
</route>

<script setup lang="ts">
const router = useRouter()
const { t } = useI18n()
const accountStore = useAccountStore()

const isSuperAdmin = computed(() => accountStore.user?.role === 1)

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-2xl font-semibold mb-2">
      {{ t('community.dashboard.title') }}
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">
      {{ t('community.dashboard.intro') }}
    </p>

    <div class="dashboard-grid">
      <Card :title="t('community.dashboard.cardRequestsTitle')" class="cursor-pointer" @click="go('/community/requests')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          {{ t('community.dashboard.cardRequestsDesc') }}
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" type="primary" @click.stop="go('/community/requests')">
            {{ t('community.dashboard.cardRequestsBtn') }}
          </n-button>
        </div>
      </Card>

      <Card title="历史需求记录" class="cursor-pointer" @click="go('/community/request-history')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          看已完成、已驳回和处理中记录。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/community/request-history')">
            打开记录
          </n-button>
        </div>
      </Card>

      <Card :title="t('community.dashboard.cardVolunteersTitle')" class="cursor-pointer" @click="go('/community/volunteers')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          {{ t('community.dashboard.cardVolunteersDesc') }}
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" type="primary" @click.stop="go('/community/volunteers')">
            {{ t('community.dashboard.cardVolunteersBtn') }}
          </n-button>
        </div>
      </Card>

      <Card title="社区加入审核" class="cursor-pointer" @click="go('/community/join-applications')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          处理居民绑定申请，避免数据串社区。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" type="primary" @click.stop="go('/community/join-applications')">
            去审核
          </n-button>
        </div>
      </Card>

      <Card :title="t('community.dashboard.cardMonitorTitle')" class="cursor-pointer" @click="go('/community/monitor')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          {{ t('community.dashboard.cardMonitorDesc') }}
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/community/monitor')">
            {{ t('community.dashboard.cardMonitorBtn') }}
          </n-button>
        </div>
      </Card>

      <Card :title="t('community.dashboard.cardNewsTitle')" class="cursor-pointer" @click="go('/community/announcements')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          {{ t('community.dashboard.cardNewsDesc') }}
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/community/announcements')">
            {{ t('community.dashboard.cardNewsBtn') }}
          </n-button>
        </div>
      </Card>

      <Card title="重点关怀对象" class="cursor-pointer" @click="go('/community/care-subjects')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          管理独居老人、残障居民等重点对象。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/community/care-subjects')">
            打开名单
          </n-button>
        </div>
      </Card>

      <Card title="便民信息" class="cursor-pointer" @click="go('/community/convenience-info')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          维护药店、医院、维修电话这些常用信息。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" @click.stop="go('/community/convenience-info')">
            去维护
          </n-button>
        </div>
      </Card>

      <Card title="异常预警" class="cursor-pointer" @click="go('/community/alerts')">
        <p class="text-sm text-slate-600 dark:text-slate-300">
          跟进多日未登录、求助骤增等风险提示。
        </p>
        <div class="mt-3 flex gap-2">
          <n-button size="small" type="warning" @click.stop="go('/community/alerts')">
            看预警
          </n-button>
        </div>
      </Card>
    </div>

    <div class="mt-6">
      <h2 class="text-lg font-semibold mb-3">
        {{ isSuperAdmin ? t('community.dashboard.sectionSystemTitle') : t('menu.communityScopedUsers') }}
      </h2>
      <div class="dashboard-grid dashboard-grid-small">
        <Card :title="t('community.dashboard.cardUsersTitle')" class="cursor-pointer" @click="go('/admin/users')">
          <p class="text-sm text-slate-600 dark:text-slate-300">
            {{ t('community.dashboard.cardUsersDesc') }}
          </p>
          <div class="mt-3 flex gap-2">
            <n-button size="small" type="primary" @click.stop="go('/admin/users')">
              {{ t('community.dashboard.cardUsersBtn') }}
            </n-button>
          </div>
        </Card>

        <Card v-if="isSuperAdmin" :title="t('community.dashboard.cardAuditTitle')" class="cursor-pointer" @click="go('/admin/audit')">
          <p class="text-sm text-slate-600 dark:text-slate-300">
            {{ t('community.dashboard.cardAuditDesc') }}
          </p>
          <div class="mt-3 flex gap-2">
            <n-button size="small" @click.stop="go('/admin/audit')">
              {{ t('community.dashboard.cardAuditBtn') }}
            </n-button>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.86fr) minmax(0, 1fr);
  gap: 16px;
}

.dashboard-grid > :nth-child(4n + 1) {
  min-height: 156px;
}

.dashboard-grid > :nth-child(4n + 2) {
  transform: translateY(10px);
}

.dashboard-grid-small {
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
}

@media (max-width: 1180px) {
  .dashboard-grid,
  .dashboard-grid-small {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
}

@media (max-width: 760px) {
  .dashboard-grid,
  .dashboard-grid-small {
    grid-template-columns: 1fr;
  }

  .dashboard-grid > :nth-child(4n + 2) {
    transform: none;
  }
}
</style>
