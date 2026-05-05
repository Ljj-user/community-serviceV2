<script setup lang="ts">
interface Props {
  title: string
  subtitle?: string
}

defineProps<Props>()
</script>

<template>
  <div class="glass-surface glass-highlight glass-shadow p-3">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <h1 class="text-xl font-semibold leading-6">
          {{ title }}
        </h1>
        <p v-if="subtitle" class="mt-1 text-sm text-slate-500 dark:text-slate-400">
          {{ subtitle }}
        </p>
      </div>
      <div v-if="$slots.actions" class="shrink-0 pt-0.5">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.default" class="mt-3">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss"></style>

