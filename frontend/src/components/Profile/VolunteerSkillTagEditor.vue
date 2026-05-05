<script setup lang="ts">
import { Add20Regular as AddIcon } from '@vicons/fluent'

const props = withDefaults(
  defineProps<{
    modelValue: string[]
    disabled?: boolean
  }>(),
  { disabled: false },
)

const emit = defineEmits<{ 'update:modelValue': [string[]] }>()

const { t } = useI18n()

const showModal = ref(false)
const selected = ref<string[]>([])
const otherText = ref('')

const OTHER = '__other__'

const presetOptions = computed(() => [
  { label: t('profile.skillPresetNursing'), value: t('profile.skillPresetNursing') },
  { label: t('profile.skillPresetRepair'), value: t('profile.skillPresetRepair') },
  { label: t('profile.skillPresetBath'), value: t('profile.skillPresetBath') },
  { label: t('profile.skillPresetCompanion'), value: t('profile.skillPresetCompanion') },
  { label: t('profile.skillPresetOther'), value: OTHER },
])

const showOtherInput = computed(() => selected.value.includes(OTHER))

watch(showModal, (open) => {
  if (open) {
    selected.value = []
    otherText.value = ''
  }
})

function removeTag(tag: string) {
  if (props.disabled)
    return
  emit('update:modelValue', (props.modelValue ?? []).filter(x => x !== tag))
}

function confirmAdd() {
  const merged = new Set(props.modelValue ?? [])
  for (const key of selected.value) {
    if (key === OTHER) {
      const o = otherText.value.trim()
      if (o)
        merged.add(o)
    } else {
      merged.add(key)
    }
  }
  emit('update:modelValue', [...merged])
  showModal.value = false
}
</script>

<template>
  <div class="skill-editor">
    <div class="skill-editor-tags">
      <n-tag
        v-for="tag in (modelValue ?? [])"
        :key="tag"
        size="small"
        round
        :closable="!disabled"
        @close="removeTag(tag)"
      >
        {{ tag }}
      </n-tag>
      <n-button
        v-if="!disabled"
        size="small"
        quaternary
        circle
        class="skill-add-btn"
        :focusable="false"
        @click="showModal = true"
      >
        <template #icon>
          <NIcon><AddIcon /></NIcon>
        </template>
      </n-button>
    </div>

    <n-modal
      v-model:show="showModal"
      preset="card"
      :title="t('profile.skillModalTitle')"
      :style="{ width: 'min(420px, 92vw)' }"
      :segmented="{ content: true, footer: 'soft' }"
    >
      <div class="skill-modal-body">
        <n-text depth="3" class="text-sm block mb-3">
          {{ t('profile.skillModalHint') }}
        </n-text>
        <n-checkbox-group v-model:value="selected">
          <n-space vertical :size="8">
            <n-checkbox
              v-for="opt in presetOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
        <transition name="skill-other">
          <div v-if="showOtherInput" class="skill-other-wrap">
            <n-input
              v-model:value="otherText"
              type="text"
              size="small"
              :placeholder="t('profile.skillOtherPlaceholder')"
            />
          </div>
        </transition>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showModal = false">
            {{ t('profile.cancelEdit') }}
          </n-button>
          <n-button size="small" type="primary" @click="confirmAdd">
            {{ t('profile.skillModalConfirm') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.skill-editor-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.skill-add-btn {
  flex-shrink: 0;
}

.skill-modal-body {
  padding-top: 4px;
}

.skill-other-wrap {
  margin-top: 12px;
  overflow: hidden;
}

.skill-other-enter-active,
.skill-other-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease, max-height 0.25s ease;
}

.skill-other-enter-from,
.skill-other-leave-to {
  opacity: 0;
  transform: translateY(-6px);
  max-height: 0;
  margin-top: 0;
}

.skill-other-enter-to,
.skill-other-leave-from {
  max-height: 48px;
}
</style>
