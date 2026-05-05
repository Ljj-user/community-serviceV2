<script setup lang="ts">
const props = defineProps<{
  status?: number
  claimStatus?: number
}>()

const steps = [
  { title: '发布', hint: '已提交' },
  { title: '审核', hint: '等通过' },
  { title: '进行中', hint: '已接单' },
  { title: '待确认', hint: '等核销' },
  { title: '完成', hint: '按时长结算' },
] as const

const currentIndex = computed(() => {
  if (props.status === 3 || props.claimStatus === 2) return 4
  if (props.status === 5 || props.claimStatus === 4) return 3
  if (props.status === 2 || props.claimStatus === 1) return 2
  if (props.status === 1) return 1
  return 0
})
</script>

<template>
  <section class="progress-card">
    <div class="progress-head">
      <span>订单进度</span>
      <b>{{ steps[currentIndex].title }}</b>
    </div>
    <div class="step-row">
      <div
        v-for="(step, idx) in steps"
        :key="step.title"
        class="step"
        :class="{ active: idx <= currentIndex, current: idx === currentIndex }"
      >
        <span class="dot">
          <FmIcon v-if="idx < currentIndex" name="i-carbon:checkmark" />
          <span v-else>{{ idx + 1 }}</span>
        </span>
        <span class="title">{{ step.title }}</span>
        <small>{{ step.hint }}</small>
      </div>
    </div>
  </section>
</template>

<style scoped>
.progress-card {
  border: 1px solid rgba(16, 185, 129, 0.22);
  border-radius: 18px;
  padding: 12px;
  background:
    radial-gradient(90% 80% at 10% 0%, rgba(220, 252, 231, 0.95), rgba(255, 255, 255, 0.86) 56%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(240, 253, 244, 0.88));
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.82) inset,
    0 10px 22px rgba(15, 23, 42, 0.07);
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-head span {
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.progress-head b {
  border-radius: 999px;
  padding: 3px 9px;
  background: #dcfce7;
  color: #047857;
  font-size: 11px;
}

.step-row {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 4px;
}

.step-row::before {
  content: "";
  position: absolute;
  left: 9%;
  right: 9%;
  top: 13px;
  height: 2px;
  border-radius: 999px;
  background: #d1d5db;
}

.step {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 3px;
  color: #94a3b8;
}

.dot {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 900;
}

.title {
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

small {
  font-size: 9px;
  color: currentColor;
  white-space: nowrap;
}

.step.active {
  color: #047857;
}

.step.active .dot {
  background: #10b981;
  border-color: #10b981;
  color: #fff;
}

.step.current .dot {
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.14);
}

:global(.dark) .progress-card {
  background:
    radial-gradient(90% 80% at 10% 0%, rgba(20, 83, 45, 0.6), rgba(17, 24, 39, 0.82) 56%),
    rgba(17, 24, 39, 0.82);
  border-color: rgba(52, 211, 153, 0.22);
}

:global(.dark) .progress-head span {
  color: #f3f4f6;
}

:global(.dark) .dot {
  background: #111827;
  border-color: #475569;
}
</style>
