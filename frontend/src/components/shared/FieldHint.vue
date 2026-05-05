<script setup lang="ts">
interface Props {
  text?: string
}

defineProps<Props>()
</script>

<template>
  <p v-if="text || $slots.default" class="field-hint text-xs text-slate-500 dark:text-slate-400 mt-1">
    <slot>
      {{ text }}
    </slot>
  </p>
</template>

<style scoped lang="scss">
.field-hint {
  line-height: 1.3;
}
</style>

