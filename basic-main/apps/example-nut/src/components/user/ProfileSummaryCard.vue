<script setup lang="ts">
const props = defineProps<{
  name: string
  locationText: string
  creditScore: number
  helpCount: number | string
  receivedCount: number | string
  avatar?: string
}>()

const emit = defineEmits<{
  profile: []
  avatar: []
}>()
</script>

<template>
  <section class="profile-card">
    <div class="head-row">
      <div class="base-info" role="button" tabindex="0" @click="emit('profile')" @keydown.enter.prevent="emit('profile')">
        <button type="button" class="avatar-wrap" aria-label="修改头像" @click.stop="emit('avatar')">
          <img :src="props.avatar || 'https://picsum.photos/seed/profile-main/120/120'" alt="我的头像">
          <span class="verify-dot">
            <FmIcon name="i-carbon:checkmark-filled" />
          </span>
        </button>
        <span class="user-main">
          <span class="name">{{ props.name }}</span>
          <span class="meta-row">
            <span class="score-tag">时间币 {{ props.creditScore }}</span>
            <span class="loc">{{ props.locationText }}</span>
          </span>
        </span>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-item">
        <b>{{ props.helpCount }}</b>
        <span>帮助他人</span>
      </div>
      <div class="stat-item">
        <b>{{ props.receivedCount }}</b>
        <span>获得帮助</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-card {
  background: color-mix(in srgb, #ffffff 86%, transparent);
  border-radius: 20px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px) saturate(170%);
  -webkit-backdrop-filter: blur(12px) saturate(170%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 10px 24px rgba(15, 23, 42, 0.08);
}

.head-row {
  display: block;
}

.base-info {
  width: 100%;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  padding: 0;
  cursor: pointer;
}

.avatar-wrap {
  position: relative;
  width: 54px;
  height: 54px;
  border-radius: 999px;
  overflow: visible;
  flex-shrink: 0;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.avatar-wrap img {
  width: 100%;
  height: 100%;
  border-radius: 999px;
  object-fit: cover;
  display: block;
  border: 2px solid #e5e7eb;
}

.verify-dot {
  position: absolute;
  right: -2px;
  bottom: 3px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #22c55e;
  border: 2px solid #fff;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.user-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.name {
  font-size: 20px;
  font-weight: 900;
  line-height: 1;
  color: #111827;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.score-tag {
  background: #dcfce7;
  color: #15803d;
  font-size: 11px;
  font-weight: 800;
  border-radius: 999px;
  padding: 4px 10px;
}

.loc {
  font-size: 11px;
  color: #6b7280;
}

.stat-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat-item {
  border-radius: 16px;
  background: color-mix(in srgb, #ffffff 92%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.72);
  min-height: 70px;
  display: grid;
  align-content: center;
  justify-items: center;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.05);
}

.stat-item b {
  font-size: 28px;
  line-height: 1;
  color: #16a34a;
  font-weight: 900;
}

.stat-item span {
  margin-top: 6px;
  font-size: 12px;
  color: #111827;
  font-weight: 700;
}

:global(.dark) .profile-card {
  background: color-mix(in srgb, #1f2937 84%, transparent);
  border-color: #374151;
}
:global(.dark) .name,
:global(.dark) .stat-item span {
  color: #f3f4f6;
}
:global(.dark) .loc {
  color: #9ca3af;
}
:global(.dark) .stat-item {
  background: rgba(17, 24, 39, 0.76);
  border-color: rgba(148, 163, 184, 0.2);
}

:global(.dark) .avatar-wrap img {
  border-color: #4b5563;
}
:global(.dark) .score-tag {
  background: rgba(16, 185, 129, 0.2);
  color: #86efac;
}

:global(.m-a11y-large) .name {
  font-size: 22px;
}
:global(.m-a11y-large) .score-tag {
  font-size: 12px;
  padding: 5px 11px;
}
:global(.m-a11y-large) .loc {
  font-size: 12px;
}
:global(.m-a11y-large) .stat-item b {
  font-size: 32px;
}
:global(.m-a11y-large) .stat-item span {
  font-size: 14px;
}
</style>
