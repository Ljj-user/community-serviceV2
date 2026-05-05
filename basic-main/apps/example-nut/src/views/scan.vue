<script setup lang="ts">
import { toast } from 'vue-sonner'

definePage({
  meta: {
    title: '扫一扫',
    auth: true,
  },
})

const router = useRouter()
onMounted(() => {
  toast.info('暂未开放扫码功能，请在上一页输入邀请码加入社区')
})
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <div class="scan-page">
      <header class="top">
        <button type="button" class="back-btn" aria-label="返回" @click="router.back()">
          <FmIcon name="i-carbon:arrow-left" />
        </button>
        <h2>扫一扫</h2>
      </header>

      <p class="hint">
        暂未开放扫码功能，请返回后输入邀请码加入社区
      </p>

      <div class="viewport">
        <div class="camera-fallback">
          <div class="qr-placeholder" aria-hidden="true">
            <span />
            <span />
            <span />
            <span />
            <span />
          </div>
          <span>扫码功能暂未开放</span>
        </div>

        <div class="scan-frame" aria-hidden="true">
          <span class="c tl" />
          <span class="c tr" />
          <span class="c bl" />
          <span class="c br" />
          <span class="scan-line" />
        </div>

      </div>

      <NutButton block type="primary" class="album-btn" @click="router.replace('/join-community')">
        去输入邀请码
      </NutButton>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.scan-page { height: 100%; background: #f4f6f8; padding: 10px 12px 20px; display: flex; flex-direction: column; }
.top { display: flex; align-items: center; gap: 10px; padding: 6px 0 4px; flex-shrink: 0; }
.back-btn { border: 0; background: #fff; width: 34px; height: 34px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; color: #111827; }
.top h2 { margin: 0; font-size: 18px; font-weight: 900; flex: 1; text-align: center; padding-right: 34px; }
.hint { margin: 0 0 12px; font-size: 12px; color: #6b7280; line-height: 1.45; }

.viewport {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  max-height: min(64vh, 420px);
  margin: 0 auto 16px;
  border-radius: 16px;
  overflow: hidden;
  background: #0f172a;
  box-shadow: 0 8px 28px rgba(15, 23, 42, .18);
}
.camera { width: 100%; height: 100%; object-fit: cover; display: block; }
.camera-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
  padding: 16px;
}
.qr-placeholder {
  width: 72px;
  height: 72px;
  border: 2px solid #67e8f9;
  border-radius: 8px;
  position: relative;
  box-sizing: border-box;
}
.qr-placeholder span {
  position: absolute;
  width: 10px;
  height: 10px;
  background: #67e8f9;
  border-radius: 2px;
}
.qr-placeholder span:nth-child(1) { left: 8px; top: 8px; }
.qr-placeholder span:nth-child(2) { right: 8px; top: 8px; }
.qr-placeholder span:nth-child(3) { left: 8px; bottom: 8px; }
.qr-placeholder span:nth-child(4) { right: 8px; bottom: 8px; }
.qr-placeholder span:nth-child(5) { left: 31px; top: 31px; }

.scan-frame {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.scan-frame .c {
  position: absolute;
  width: 28px;
  height: 28px;
  border-color: #34d399;
  border-style: solid;
  border-width: 0;
  opacity: .95;
}
.scan-frame .tl { top: 14%; left: 14%; border-top-width: 3px; border-left-width: 3px; border-top-left-radius: 4px; }
.scan-frame .tr { top: 14%; right: 14%; border-top-width: 3px; border-right-width: 3px; border-top-right-radius: 4px; }
.scan-frame .bl { bottom: 14%; left: 14%; border-bottom-width: 3px; border-left-width: 3px; border-bottom-left-radius: 4px; }
.scan-frame .br { bottom: 14%; right: 14%; border-bottom-width: 3px; border-right-width: 3px; border-bottom-right-radius: 4px; }

.scan-line {
  position: absolute;
  left: 18%;
  right: 18%;
  height: 2px;
  top: 22%;
  background: linear-gradient(90deg, transparent, #6ee7b7, #34d399, #6ee7b7, transparent);
  border-radius: 2px;
  box-shadow: 0 0 12px rgba(52, 211, 153, .7);
  animation: scan-sweep 2.4s ease-in-out infinite;
}

@keyframes scan-sweep {
  0%, 100% { top: 22%; opacity: .6; }
  50% { top: 72%; opacity: 1; }
}

.scan-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, .55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}

.album-btn { flex-shrink: 0; }
.hidden-input { position: absolute; width: 0; height: 0; opacity: 0; pointer-events: none; }

:global(.dark) .scan-page { background: #111827; }
:global(.dark) .back-btn { background: #1f2937; color: #f3f4f6; border: 1px solid #374151; }
:global(.dark) .top h2 { color: #f3f4f6; }
:global(.dark) .hint { color: #9ca3af; }
</style>
