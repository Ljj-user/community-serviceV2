<script setup lang="ts">
import { storeToRefs } from 'pinia'

interface Props {
  title?: string
  titleSize?: 'small' | 'normal' | 'large'
  stretchHeight?: boolean
  noShadow?: boolean
  bottomBorder?: boolean
  padded?: boolean
}

withDefaults(defineProps<Props>(), {
  titleSize: 'normal',
  stretchHeight: false,
  noShadow: false,
  bottomBorder: false,
  padded: true,
})
const slots = useSlots()
const layout = useLayoutStore()
const { flatDesign } = storeToRefs(layout)
</script>

<template>
  <div :class="{ 'h-full': stretchHeight }">
    <div v-if="slots.header" class="py-2">
      <slot name="header" />
    </div>
    <div class="card-container" :class="{ 'h-full': stretchHeight }">
      <div
        class="card-content dark:bg-slate-900 rounded-md border-solid border-color-default relative z-10"
        :class="{
          'p-4': padded,
          'border-1': flatDesign,
          'h-full': stretchHeight,
          'shadow-lg': !flatDesign && !noShadow,
          'drop-shadow-md': !flatDesign && !noShadow,
          'card-bottom-border': bottomBorder,
        }"
      >
        <div v-if="slots.title" class="mix-blend-difference">
          <slot name="title" />
        </div>
        <div v-else-if="title">
          <h3 class="title pb-2 text-dark-400 dark:text-light-800 mix-blend-difference" :class="`title-${titleSize}`">
            {{ title }}
          </h3>
        </div>
        <div v-if="slots.subtitle">
          <slot name="subtitle" />
        </div>
        <slot />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.card-container {
  position: relative;

  .card-content {
    --un-bg-opacity: .7;
  }

  .card-bottom-border {
    border-bottom-width: 2px;
  }
  .title {
    font-size: 1rem;
    font-weight: 500;
  }

  .title-small {
    font-size: .9rem;
    padding-bottom: .2rem;
  }

  .title-large {
    font-size: 1.4rem;
    font-weight: 700;
  }
}
</style>
