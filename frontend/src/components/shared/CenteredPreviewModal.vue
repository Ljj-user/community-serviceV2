<script setup lang="ts">
defineProps<{
  show: boolean
  title: string
  subtitle?: string
  width?: number
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :style="{ width: `min(${width || 920}px, 92vw)` }"
    class="centered-preview-modal"
    :bordered="false"
    closable
    @update:show="(value) => emit('update:show', value)"
  >
    <template #header>
      <div class="preview-header">
        <div class="preview-title">{{ title }}</div>
        <div v-if="subtitle" class="preview-subtitle">{{ subtitle }}</div>
        <div v-if="$slots.meta" class="preview-meta">
          <slot name="meta" />
        </div>
      </div>
    </template>

    <div class="preview-body">
      <slot />
    </div>

    <template v-if="$slots.footer" #footer>
      <div class="preview-footer">
        <slot name="footer" />
      </div>
    </template>
  </n-modal>
</template>

<style scoped>
.preview-header {
  display: grid;
  gap: 10px;
  padding-top: 2px;
}

.preview-title {
  font-size: 24px;
  line-height: 1.2;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0;
}

.preview-subtitle {
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-body {
  display: grid;
  gap: 18px;
  max-height: min(72vh, 820px);
  overflow-y: auto;
  padding-right: 4px;
}

.preview-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:global(.centered-preview-modal .n-card) {
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 248, 0.98));
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.95) inset,
    0 24px 60px rgba(15, 23, 42, 0.14);
}

:global(.dark .centered-preview-modal .n-card) {
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(18, 30, 25, 0.98));
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.04) inset,
    0 26px 62px rgba(0, 0, 0, 0.42);
}

:global(.dark) .preview-title {
  color: #f8fafc;
}

:global(.dark) .preview-subtitle {
  color: #94a3b8;
}
</style>
