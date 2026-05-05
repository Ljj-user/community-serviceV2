<script setup lang="ts">
import { toast } from 'vue-sonner'
import aiIcon from '@/assets/mobile/Ai.svg'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  showVoice?: boolean
}>(), {
  placeholder: '直接说需求，AI 帮你整理',
  showVoice: true,
})

const emit = defineEmits<{
  'update:modelValue': [string]
  send: []
}>()

const listening = ref(false)
let recognition: any = null

function toggleVoice() {
  const win = window as any
  const SpeechRecognition = win.SpeechRecognition || win.webkitSpeechRecognition
  if (!SpeechRecognition) {
    toast.error('当前浏览器不支持语音输入')
    return
  }

  if (!recognition) {
    recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.interimResults = false
    recognition.maxAlternatives = 1
    recognition.onresult = (event: any) => {
      const transcript = event?.results?.[0]?.[0]?.transcript || ''
      emit('update:modelValue', String(transcript).trim())
    }
    recognition.onend = () => {
      listening.value = false
    }
    recognition.onerror = () => {
      listening.value = false
      toast.error('语音识别失败')
    }
  }

  if (listening.value) {
    recognition.stop()
    listening.value = false
    return
  }

  listening.value = true
  recognition.start()
}
</script>

<template>
  <div class="hero-input">
    <div class="hero-pill" role="search">
      <div class="hero-icon" aria-hidden="true">
        <img :src="aiIcon" alt="" class="hero-ai-image">
      </div>
      <input
        :value="props.modelValue"
        class="hero-field"
        type="text"
        inputmode="text"
        :placeholder="props.placeholder"
        @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        @keyup.enter="emit('send')"
      >
      <button
        v-if="props.showVoice"
        type="button"
        class="hero-mic"
        :class="{ active: listening }"
        :aria-label="listening ? '停止语音输入' : '语音输入'"
        @click="toggleVoice"
      >
        <FmIcon name="i-carbon:microphone-filled" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.hero-input {
  padding: 10px 0 6px;
}

.hero-pill {
  height: 54px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e5ece8;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.82) inset,
    0 6px 14px rgba(15, 23, 42, 0.05);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 8px 10px 8px 10px;
}

.hero-icon {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-ai-image {
  width: 100%;
  height: 100%;
  border-radius: 11px;
  display: block;
}

.hero-field {
  border: 0;
  outline: none;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  min-width: 0;
}

.hero-field::placeholder {
  color: #94a3b8;
  font-weight: 600;
}

.hero-mic {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  border: 0;
  background: rgba(22, 163, 74, 0.1);
  color: #15803d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 4px 10px rgba(2, 6, 23, 0.05);
}

.hero-mic.active {
  color: #fff;
  background: #dc2626;
  box-shadow:
    0 12px 22px rgba(220, 38, 38, 0.24),
    0 1px 0 rgba(255, 255, 255, 0.18) inset;
}

:global(.dark) .hero-pill {
  background: rgba(17, 24, 39, 0.86);
  border-color: rgba(148, 163, 184, 0.24);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.06) inset,
    0 8px 16px rgba(0, 0, 0, 0.3);
}
:global(.dark) .hero-field {
  color: rgba(243, 244, 246, 0.92);
}
:global(.dark) .hero-field::placeholder {
  color: rgba(156, 163, 175, 0.92);
}
:global(.dark) .hero-mic {
  background: rgba(16, 185, 129, 0.12);
  color: rgba(167, 243, 208, 0.88);
}
</style>
