<script setup lang="ts">
import aiIcon from '@/assets/mobile/Ai.svg'
const props = withDefaults(defineProps<{
  communityName: string
  distanceText: string
  rightAction?: 'messages' | 'ai' | 'none'
}>(), {
  rightAction: 'messages',
})

const emit = defineEmits<{
  changeCommunity: []
  right: []
}>()
</script>

<template>
  <header class="biz-header">
    <button class="community-switch" @click="emit('changeCommunity')">
      <FmIcon name="i-carbon:location-filled" class="community-icon" />
      <span class="community-text">
        <span class="community-main">{{ communityName }}</span>
        <span class="distance">{{ distanceText }}</span>
      </span>
      <FmIcon name="i-carbon:chevron-right" />
    </button>
    <button
      v-if="props.rightAction !== 'none'"
      type="button"
      class="right-btn"
      :aria-label="props.rightAction === 'ai' ? 'AI 助手' : '消息'"
      @click="emit('right')"
    >
      <img v-if="props.rightAction === 'ai'" :src="aiIcon" alt="AI" class="right-ai-icon">
      <FmIcon v-else name="mdi:message-processing-outline" />
    </button>
  </header>
</template>

<style scoped>
.biz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin: 10px 12px 8px;
  border-radius: 16px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 35%);
  background: color-mix(in srgb, var(--m-color-card), transparent 18%);
  backdrop-filter: saturate(180%) blur(14px);
  -webkit-backdrop-filter: saturate(180%) blur(14px);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 10px 22px rgba(15, 23, 42, 0.10),
    0 2px 6px rgba(15, 23, 42, 0.08);
}
.community-switch { display: inline-flex; align-items: center; gap: 8px; border: 0; background: transparent; font-weight: 700; padding: 0; margin: 0; min-width: 0; flex: 1; }
.community-text { min-width: 0; display: grid; gap: 1px; text-align: left; }
.community-main { font-size: 15px; font-weight: 800; color: var(--m-color-text); max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.right-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--m-color-border), transparent 20%);
  background: color-mix(in srgb, var(--m-color-card), transparent 26%);
  color: color-mix(in srgb, var(--m-color-text), #16a34a 22%);
  font-weight: 900;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.25) inset,
    0 6px 14px rgba(15, 23, 42, 0.12),
    0 1px 3px rgba(15, 23, 42, 0.10);
}
.right-btn:active { transform: scale(0.97); }
.community-icon { color: #059669; font-size: 18px; }
.distance { font-size: 11px; color: var(--m-color-muted); line-height: 1.1; }
.right-ai-icon { width: 20px; height: 20px; border-radius: 7px; object-fit: cover; display: block; }

:global(.dark) .biz-header {
  border-color: rgba(148, 163, 184, 0.22);
  background: rgba(17, 24, 39, 0.55);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.06) inset,
    0 10px 22px rgba(0, 0, 0, 0.40),
    0 2px 6px rgba(0, 0, 0, 0.28);
}
:global(.dark) .community-switch { color: #f3f4f6; }
:global(.dark) .distance { color: #9ca3af; }
:global(.dark) .right-btn {
  border-color: rgba(148, 163, 184, 0.22);
  background: rgba(31, 41, 55, 0.45);
  color: #d1fae5;
}

:global(.m-a11y-large) .community-main {
  font-size: 16px;
  max-width: calc(100% - 30px);
}
:global(.m-a11y-large) .distance {
  font-size: 12px;
}
</style>

