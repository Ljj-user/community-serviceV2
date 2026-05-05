<script setup lang="ts">
const emit = defineEmits<{
  improve: []
  volunteer: []
  task: []
  convenience: []
  edit: []
  mall: []
}>()
</script>

<template>
  <section class="menu-card">
    <button type="button" class="menu-item" @click="emit('improve')">
      <span class="left">
        <span class="icon-wrap">
          <FmIcon name="mdi:account-edit-outline" />
        </span>
        <span class="label">完善信息</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" class="arrow" />
    </button>
    <button type="button" class="menu-item" @click="emit('task')">
      <span class="left">
        <span class="icon-wrap">
          <FmIcon name="mdi:clipboard-list-outline" />
        </span>
        <span class="label">我的任务</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" class="arrow" />
    </button>
    <button type="button" class="menu-item" @click="emit('volunteer')">
      <span class="left">
        <span class="icon-wrap">
          <FmIcon name="mdi:shield-check-outline" />
        </span>
        <span class="label">志愿者认证</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" class="arrow" />
    </button>
    <button type="button" class="menu-item" @click="emit('mall')">
      <span class="left">
        <span class="icon-wrap">
          <FmIcon name="mdi:storefront-outline" />
        </span>
        <span class="label">爱心商城</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" class="arrow" />
    </button>
    <button type="button" class="menu-item" @click="emit('convenience')">
      <span class="left">
        <span class="icon-wrap">
          <FmIcon name="mdi:phone-in-talk-outline" />
        </span>
        <span class="label">便民信息</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" class="arrow" />
    </button>
    <button type="button" class="menu-item" @click="emit('edit')">
      <span class="left">
        <span class="icon-wrap">
          <FmIcon name="mdi:cog-outline" />
        </span>
        <span class="label">设置中心</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" class="arrow" />
    </button>
  </section>
</template>

<style scoped>
.menu-card {
  border-radius: 20px;
  background: color-mix(in srgb, #ffffff 86%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(10px) saturate(160%);
  -webkit-backdrop-filter: blur(10px) saturate(160%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 10px 22px rgba(15, 23, 42, 0.08);
  padding: 12px;
  display: grid;
  gap: 10px;
}

.menu-item {
  width: 100%;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: color-mix(in srgb, #ffffff 92%, transparent);
  padding: 13px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  border-radius: 14px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.05);
}

.left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-wrap {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: #eefbf2;
  color: #16a34a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.label {
  font-size: 14px;
  font-weight: 800;
  color: #111827;
}

.arrow {
  font-size: 16px;
  color: #9ca3af;
}

:global(.dark) .menu-card {
  background: rgba(17, 24, 39, 0.58);
  border-color: rgba(148, 163, 184, 0.26);
}
:global(.dark) .menu-item {
  background: rgba(31, 41, 55, 0.62);
  border-color: rgba(148, 163, 184, 0.2);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.28);
}
:global(.dark) .label {
  color: #f3f4f6;
}
:global(.dark) .arrow {
  color: #94a3b8;
}
:global(.dark) .icon-wrap {
  background: rgba(16, 185, 129, 0.18);
  color: #86efac;
}

:global(.m-a11y-large) .menu-item {
  padding: 15px 14px;
}
:global(.m-a11y-large) .label {
  font-size: 16px;
}
:global(.m-a11y-large) .icon-wrap {
  width: 34px;
  height: 34px;
  font-size: 18px;
}
</style>
