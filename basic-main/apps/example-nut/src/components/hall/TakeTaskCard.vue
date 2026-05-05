<script setup lang="ts">
import type { ServiceRequestVO } from '@/api/modules/serviceRequests'

defineProps<{
  item: ServiceRequestVO
  taking: boolean
  urgencyText: (v: number) => string
  urgencyClass: (v: number) => string
}>()

const emit = defineEmits<{
  take: []
}>()
</script>

<template>
  <article class="task-card">
    <div class="task-main">
      <div class="task-head">
        <h4>{{ item.requesterName || '社区居民求助' }}</h4>
        <span class="urgency" :class="urgencyClass(item.urgencyLevel)">{{ urgencyText(item.urgencyLevel) }}</span>
      </div>
      <p class="desc">{{ item.description || '需要邻里协助处理，请查看详情后接取。' }}</p>
      <div class="meta">
        <span><FmIcon name="i-carbon:user" />{{ item.requesterName || '求助人待实名' }}</span>
        <span><FmIcon name="i-carbon:location" />{{ item.serviceAddress || '本社区' }}</span>
        <span v-if="item.emergencyContactPhone"><FmIcon name="i-carbon:phone" />{{ item.emergencyContactPhone }}</span>
        <span><FmIcon name="i-carbon:time" />{{ item.expectedTime?.slice(0, 16) || '尽快' }}</span>
      </div>
    </div>

    <button type="button" class="take-btn" :disabled="taking" @click="emit('take')">
      {{ taking ? '接取中...' : '立即接取' }}
    </button>
  </article>
</template>

<style scoped>
.task-card {
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 10px 10px 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  min-height: 0;
  height: auto;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
}
.task-main { min-width: 0; flex: 1; display: grid; gap: 4px; }
.task-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.task-head h4 { margin: 0; font-size: 15px; color: #111827; font-weight: 900; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.urgency { border-radius: 999px; font-size: 11px; font-weight: 800; padding: 2px 8px; color: #fff; flex-shrink: 0; }
.urgent-high { background: #dc2626; }
.urgent-mid { background: #d97706; }
.urgent-low { background: #059669; }
.desc {
  margin: 0;
  font-size: 12px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta { display: flex; gap: 10px; flex-wrap: wrap; font-size: 11px; color: #6b7280; }
.meta span { display: inline-flex; align-items: center; gap: 4px; }
.take-btn {
  width: 96px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 900;
  background: #ffffff;
  border: 1px solid #bbf7d0;
  color: #065f46;
}
.take-btn:active { background: #f0fdf4; }
.take-btn:disabled { opacity: .6; }

@media (max-width: 380px) {
  .task-card { gap: 8px; }
  .take-btn { width: 88px; }
}
</style>
