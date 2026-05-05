<script setup lang="ts">
const props = defineProps<{
  label: string
}>()
</script>

<template>
  <div class="field-block">
    <label class="field-label">{{ props.label }}</label>
    <slot />
  </div>
</template>

<style scoped>
.field-block {
  display: grid;
  gap: 6px;
}
.field-label {
  font-size: 12px;
  color: #4b5563;
  font-weight: 800;
}
</style>
