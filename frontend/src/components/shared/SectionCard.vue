<script setup lang="ts">
import Card from './Card.vue'

interface Props {
  title: string
  description?: string
}

defineProps<Props>()
</script>

<template>
  <Card padded class="glass-surface glass-shadow">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0">
        <h3 class="text-base font-semibold leading-6">
          {{ title }}
        </h3>
        <p v-if="description" class="mt-1 text-sm text-slate-500 dark:text-slate-400">
          {{ description }}
        </p>
      </div>
      <div v-if="$slots.actions" class="shrink-0">
        <slot name="actions" />
      </div>
    </div>
    <div class="mt-4">
      <slot />
    </div>
  </Card>
</template>

