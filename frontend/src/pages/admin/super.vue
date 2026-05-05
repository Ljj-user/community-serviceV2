<route lang="yaml">
meta:
  title: super-admin-dashboard
  layout: default
  breadcrumb:
    - adminOpsGroup
    - super-admin-dashboard
</route>

<script setup lang="ts">
const { t } = useI18n()
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-2xl font-semibold">
      {{ t('community.super.title') }}
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400 max-w-2xl leading-relaxed">
      {{ t('community.super.subtitle') }}
    </p>
    <div
      class="rounded-lg border border-dashed border-slate-200 dark:border-slate-600 bg-slate-50/80 dark:bg-slate-900/40 px-6 py-12 text-center text-slate-500 dark:text-slate-400 text-sm"
    >
      {{ t('community.super.placeholder') }}
    </div>
  </div>
</template>
