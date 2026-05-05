<script setup lang="ts">
import type { ServiceClaimVO } from '@/api/modules/hall'
import type { ServiceRequestVO } from '@/api/modules/serviceRequests'
import RequestProgressCard from '@/components/hall/RequestProgressCard.vue'
import {
  claimStatusText,
  fmtTime,
  requestStatusCode,
  requestStatusText,
} from '@/composables/useHallTaskCenter'

const props = defineProps<{
  visible: boolean
  detailLoading: boolean
  claimDetail: ServiceRequestVO | null
  selectedClaim: ServiceClaimVO | null
  selectedPublished: ServiceRequestVO | null
  completeLoading: boolean
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  close: []
  completeClaim: []
  confirmComplete: []
}>()

function closePopup() {
  emit('update:visible', false)
  emit('close')
}
</script>

<template>
  <NutPopup
    v-if="visible"
    :visible="visible"
    position="bottom"
    round
    closeable
    :close-on-click-overlay="true"
    class="order-popup"
    @click-overlay="closePopup"
    @update:visible="(value) => !value && closePopup()"
  >
    <div class="claim-drawer">
      <div class="drawer-handle" />
      <h3>订单详情</h3>
      <div v-if="detailLoading" class="status">
        加载中...
      </div>
      <div v-else-if="claimDetail" class="detail-content">
        <div class="status-row">
          <span class="state-code">{{ requestStatusCode(claimDetail.status) }}</span>
          <span class="state-text">{{ requestStatusText(claimDetail.status) }}</span>
        </div>
        <RequestProgressCard
          :status="claimDetail.status"
          :claim-status="selectedClaim?.claimStatus || selectedPublished?.latestClaimStatus || claimDetail.latestClaimStatus"
        />
        <div class="info-grid">
          <p><b>服务类型</b><span>{{ claimDetail.serviceType }}</span></p>
          <p><b>求助人</b><span>{{ claimDetail.requesterName || claimDetail.emergencyContactName || '未实名用户' }}</span></p>
          <p><b>志愿者</b><span>{{ selectedClaim?.volunteerName || claimDetail.latestVolunteerName || '暂未认领' }}</span></p>
          <p><b>手机号</b><span>{{ claimDetail.emergencyContactPhone || selectedClaim?.requesterPhone || '-' }}</span></p>
          <p class="wide"><b>地址</b><span>{{ claimDetail.serviceAddress || '-' }}</span></p>
          <p><b>需求状态</b><span>{{ requestStatusText(claimDetail.status) }}</span></p>
          <p><b>认领状态</b><span>{{ claimStatusText(selectedClaim?.claimStatus || selectedPublished?.latestClaimStatus || claimDetail.latestClaimStatus) }}</span></p>
          <p class="wide"><b>认领时间</b><span>{{ fmtTime(selectedClaim?.claimAt || selectedClaim?.createdAt) }}</span></p>
          <p class="wide"><b>说明</b><span>{{ claimDetail.description || '暂无说明' }}</span></p>
        </div>
        <NutButton
          v-if="selectedClaim && Number(selectedClaim.claimStatus) === 1"
          block
          type="primary"
          :loading="completeLoading"
          @click="emit('completeClaim')"
        >
          {{ completeLoading ? '提交中...' : '完成服务' }}
        </NutButton>
        <NutButton
          v-else-if="!selectedClaim && (Number(selectedPublished?.latestClaimStatus) === 4 || Number(claimDetail.status) === 5)"
          block
          type="primary"
          :loading="completeLoading"
          @click="emit('confirmComplete')"
        >
          {{ completeLoading ? '确认中...' : '确认完成' }}
        </NutButton>
      </div>
    </div>
  </NutPopup>
</template>

<style scoped>
.claim-drawer {
  padding: 12px 16px calc(20px + env(safe-area-inset-bottom));
}

.drawer-handle {
  width: 42px;
  height: 4px;
  border-radius: 999px;
  margin: 0 auto 10px;
  background: rgba(148, 163, 184, 0.5);
}

.claim-drawer h3 {
  margin: 0 0 12px;
  color: var(--m-color-text);
  font-size: 18px;
  font-weight: 800;
  text-align: center;
}

.status {
  text-align: center;
  color: var(--m-color-subtext);
  font-size: 13px;
}

.detail-content {
  display: grid;
  gap: 14px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.state-code {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--m-color-subtext);
}

.state-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--m-color-text);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.info-grid p {
  margin: 0;
  padding: 12px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  display: grid;
  gap: 6px;
}

.info-grid p.wide {
  grid-column: 1 / -1;
}

.info-grid b {
  color: var(--m-color-subtext);
  font-size: 12px;
}

.info-grid span {
  color: var(--m-color-text);
  font-size: 13px;
  line-height: 1.5;
}
</style>
