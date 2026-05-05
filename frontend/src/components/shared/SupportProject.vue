<script setup lang="ts">
import { storeToRefs } from 'pinia'

const store = useLayoutStore()
const { collapsed, forceCollapsed } = storeToRefs(store)

onMounted(() => {
  store.setSupportEnabled()
  initLikeCount()
})

const likeCount = ref<number>(0)
const likeAnimating = ref(false)
const sidebarOpen = computed(() => !collapsed.value && !forceCollapsed.value)
const { t } = useI18n()

import { fetchLikeCount, addLikeOnce } from '~/api/supportLikes'

async function initLikeCount() {
  try {
    likeCount.value = await fetchLikeCount()
  } catch {
    likeCount.value = 0
  }
}

async function handleLike() {
  try {
    likeAnimating.value = true
    likeCount.value = await addLikeOnce()
  } finally {
    // 动画持续一小段时间
    setTimeout(() => {
      likeAnimating.value = false
    }, 400)
  }
}
</script>

<template>
  <div class="support-button ltr" v-if="sidebarOpen">
    <!-- 点赞器 -->
    <button
      type="button"
      class="coffee-button my-2 flex overflow-hidden items-center text-sm font-medium focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 text-black shadow hover:bg-yellow/90 h-9 px-4 py-2 max-w-52 whitespace-pre md:flex group relative w-full justify-center gap-2 rounded-md transition-all duration-300 ease-out hover:ring-2 hover:ring-yellow hover:ring-offset-2"
      :class="{ 'like-pulse': likeAnimating }"
      @click="handleLike"
    >
      <span
        class="absolute right-0 -mt-12 h-32 w-8 translate-x-12 rotate-12 bg-white opacity-10 transition-all duration-1000 ease-out group-hover:-translate-x-40"
      ></span>
      <div class="flex items-center">
        <BuyMeCoffeeIcon />
        <span class="ms-1 text-black">
          为社区点个赞（{{ likeCount }}）
        </span>
      </div>
      <span v-if="likeAnimating" class="like-float">+1</span>
    </button>

    <!-- GitHub 按钮，跳转官网 -->
    <a
      href="https://github.com"
      target="_blank"
      class="github-button flex overflow-hidden items-center text-sm font-medium focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 bg-black text-white shadow hover:bg-black/90 h-9 px-4 py-2 max-w-52 whitespace-pre md:flex group relative w-full justify-center gap-2 rounded-md transition-all duration-300 ease-out hover:ring-2 hover:ring-black hover:ring-offset-2"
    >
      <span
        class="absolute right-0 -mt-12 h-32 w-8 translate-x-12 rotate-12 bg-white opacity-10 transition-all duration-1000 ease-out group-hover:-translate-x-40"
      ></span>
      <div class="flex items-center">
        <GithubIcon />
        <span class="ms-1 text-white">GitHub</span>
      </div>
    </a>
  </div>


</template>

<style lang="scss">
.support-button {
  position: absolute;
  bottom: -100px;
  z-index: 100;
  font-family: 'Inter var',
    'ui-sans-serif',
    'system-ui',
    'sans-serif';
}

.github-button,
.coffee-button {
  left: 30px;
  animation-duration: 0.8s;
  animation-delay: 1.5s;
  animation-name: slideUp;
  animation-timing-function: ease-in-out;
  animation-fill-mode: forwards;
}

.coffee-button {
  background-color: #5f7fff;
}

.coffee-button.like-pulse {
  transform: scale(1.05);
}

.like-float {
  position: absolute;
  right: 16px;
  top: -8px;
  color: #f97316;
  font-weight: 600;
  animation: likeFloatUp 0.4s ease-out forwards;
}

@keyframes likeFloatUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(-6px);
  }
}

.rtl {
    .support-button {
      font-family:Shabnam;
    }
  .github-button,
  .coffee-button {
    left: auto;
    right: 30px;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(100px);
  }

  to {
    transform: translateY(-125px);
  }
}
</style>
