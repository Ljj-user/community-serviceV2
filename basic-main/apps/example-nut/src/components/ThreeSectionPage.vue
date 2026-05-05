<script setup lang="ts">
defineProps<{
  /** 页面根容器 class */
  pageClass?: string
  /** 内容区内边距 class */
  contentClass?: string
}>()
</script>

<template>
  <div class="three-page" :class="pageClass">
    <header class="three-header">
      <slot name="header" />
    </header>
    <main class="three-content" :class="contentClass">
      <slot />
    </main>
    <footer v-if="$slots.footer" class="three-footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<style scoped>
.three-page {
  min-height: 100%;
  display: grid;
  grid-template-rows: auto 1fr auto;
  background: var(--m-color-bg);
}

.three-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--m-color-bg);
}

.three-content {
  min-height: 0;
  overflow: auto;
  padding: var(--m-space-page);
  padding-bottom: calc(var(--m-space-page) + env(safe-area-inset-bottom, 0px));
}

.three-footer {
  position: sticky;
  bottom: 0;
  z-index: 10;
  background: var(--m-color-bg);
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

:global(.dark) .three-page,
:global(.dark) .three-header,
:global(.dark) .three-footer {
  background: #111827;
}
:global(.dark) .three-footer {
  border-top-color: rgba(148, 163, 184, 0.16);
}
</style>

