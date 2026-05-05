<script setup lang="ts">
const props = withDefaults(defineProps<{
  helpTitle?: string
  helpSub?: string
  publishTitle?: string
  publishSub?: string
  requestTitle?: string
  requestSub?: string
  serviceTitle?: string
  serviceSub?: string
  volunteerTitle?: string
  volunteerSub?: string
  convenienceTitle?: string
  convenienceSub?: string
  showVolunteer?: boolean
  showRequest?: boolean
  showService?: boolean
  showConvenience?: boolean
}>(), {
  helpTitle: '我去帮助别人',
  helpSub: '按技能接单',
  publishTitle: '我要发布求助',
  publishSub: '填表就能发',
  requestTitle: '我的求助',
  requestSub: '看进度和评价',
  serviceTitle: '我的服务',
  serviceSub: '跟进进行中',
  volunteerTitle: '志愿者认证',
  volunteerSub: '认证后更好接单',
  convenienceTitle: '便民信息',
  convenienceSub: '电话和地址',
  showVolunteer: false,
  showRequest: false,
  showService: false,
  showConvenience: false,
})

const emit = defineEmits<{
  help: []
  publish: []
  myRequest: []
  myService: []
  volunteer: []
  convenience: []
}>()

const route = useRoute()
const router = useRouter()
const isHomePage = computed(() => route.path === '/')
const shouldShowVolunteer = computed(() => props.showVolunteer || isHomePage.value)
const shouldShowRequest = computed(() => props.showRequest || isHomePage.value)
const shouldShowService = computed(() => props.showService || isHomePage.value)
const shouldShowConvenience = computed(() => props.showConvenience || isHomePage.value)
const homeCopy = {
  publishTitle: '我要发布求助',
  publishSub: '填表就能发',
  helpTitle: '我去帮助别人',
  helpSub: '按技能接单',
  requestTitle: '我的求助',
  requestSub: '看进度和评价',
  serviceTitle: '我的服务',
  serviceSub: '跟进进行中',
  volunteerTitle: '志愿者认证',
  volunteerSub: '认证后更好接单',
  convenienceTitle: '便民信息',
  convenienceSub: '药店、电话、地址',
}

function emitOrNavigate(type: 'publish' | 'help' | 'myRequest' | 'myService' | 'volunteer' | 'convenience') {
  if (type === 'publish') {
    if (isHomePage.value) return router.push('/hall-publish')
    emit('publish')
    return
  }
  if (type === 'help') {
    if (isHomePage.value) return router.push('/hall-take')
    emit('help')
    return
  }
  if (type === 'myRequest') {
    if (isHomePage.value) return router.push('/my-requests')
    emit('myRequest')
    return
  }
  if (type === 'myService') {
    if (isHomePage.value) return router.push('/my-services')
    emit('myService')
    return
  }
  if (type === 'volunteer') {
    if (isHomePage.value) return router.push('/volunteer-certification')
    emit('volunteer')
    return
  }
  if (isHomePage.value) return router.push('/convenience-info')
  emit('convenience')
}

const mainItems = computed(() => {
  const items = [
    {
      key: 'publish',
      title: isHomePage.value ? homeCopy.publishTitle : props.publishTitle,
      sub: isHomePage.value ? homeCopy.publishSub : props.publishSub,
      icon: 'mdi:account-voice',
      cardClass: 'publish',
      iconClass: 'subtle',
      titleClass: 'tone-ink',
      subClass: 'tone-muted',
      click: () => emitOrNavigate('publish'),
    },
    {
      key: 'help',
      title: isHomePage.value ? homeCopy.helpTitle : props.helpTitle,
      sub: isHomePage.value ? homeCopy.helpSub : props.helpSub,
      icon: 'mdi:hand-heart-outline',
      cardClass: 'help',
      iconClass: 'solid',
      titleClass: 'tone-light',
      subClass: 'tone-light',
      click: () => emitOrNavigate('help'),
    },
  ]

  if (shouldShowRequest.value) {
    items.push({
      key: 'request',
      title: homeCopy.requestTitle,
      sub: homeCopy.requestSub,
      icon: 'mdi:file-document-outline',
      cardClass: 'request',
      iconClass: 'soft',
      titleClass: 'tone-ink',
      subClass: 'tone-muted',
      click: () => emitOrNavigate('myRequest'),
    })
  }

  if (shouldShowService.value) {
    items.push({
      key: 'service',
      title: homeCopy.serviceTitle,
      sub: homeCopy.serviceSub,
      icon: 'mdi:clipboard-check-outline',
      cardClass: 'service',
      iconClass: 'ink',
      titleClass: 'tone-ink',
      subClass: 'tone-muted',
      click: () => emitOrNavigate('myService'),
    })
  }

  if (shouldShowVolunteer.value) {
    items.push({
      key: 'volunteer',
      title: isHomePage.value ? homeCopy.volunteerTitle : props.volunteerTitle,
      sub: isHomePage.value ? homeCopy.volunteerSub : props.volunteerSub,
      icon: 'mdi:shield-check-outline',
      cardClass: 'volunteer',
      iconClass: 'soft',
      titleClass: 'tone-ink',
      subClass: 'tone-muted',
      click: () => emitOrNavigate('volunteer'),
    })
  }

  return items
})
</script>

<template>
  <div class="quick-wrap">
    <div class="quick-grid">
      <button
        v-for="item in mainItems"
        :key="item.key"
        class="quick-item"
        :class="[item.cardClass, { wide: item.key === 'volunteer' }]"
        type="button"
        @click="item.click()"
      >
        <span class="q-ico" :class="item.iconClass" aria-hidden="true">
          <FmIcon :name="item.icon" />
        </span>
        <div class="qa-title" :class="item.titleClass">{{ item.title }}</div>
        <div class="qa-sub" :class="item.subClass">{{ item.sub }}</div>
      </button>
    </div>

    <button
      v-if="shouldShowConvenience"
      class="convenience-strip"
      type="button"
      @click="emitOrNavigate('convenience')"
    >
      <span class="strip-ico" aria-hidden="true">
        <FmIcon name="mdi:map-marker-radius-outline" />
      </span>
      <span class="strip-main">
        <b>{{ isHomePage ? homeCopy.convenienceTitle : convenienceTitle }}</b>
        <small>{{ isHomePage ? homeCopy.convenienceSub : convenienceSub }}</small>
      </span>
      <FmIcon name="mdi:chevron-right" class="strip-arrow" aria-hidden="true" />
    </button>
  </div>
</template>

<style scoped>
.quick-wrap {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-item {
  appearance: none;
  border-radius: 18px;
  min-height: 126px;
  padding: 14px 14px 16px;
  text-align: left;
  cursor: pointer;
  display: grid;
  align-content: start;
  gap: 6px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 8px 18px rgba(15, 23, 42, 0.07);
  transition:
    transform 180ms cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.quick-item:active {
  transform: translateY(1px);
}

.quick-item.wide {
  grid-column: 1 / -1;
  min-height: 108px;
}

.q-ico {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.q-ico.solid {
  color: rgba(255, 255, 255, 0.96);
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.26);
}

.q-ico.subtle {
  background: #eefbf2;
  border: 1px solid rgba(16, 185, 129, 0.25);
  color: #16a34a;
}

.q-ico.soft {
  background: #f2f7f3;
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #166534;
}

.q-ico.ink {
  background: #f4f6f8;
  border: 1px solid rgba(100, 116, 139, 0.18);
  color: #334155;
}

.qa-title {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 900;
  line-height: 1.22;
}

.qa-sub {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.45;
}

.tone-light { color: #ffffff; }
.tone-ink { color: #0f172a; }
.tone-muted { color: #475569; }

.quick-item.help {
  border: 0;
  background: linear-gradient(135deg, #16a34a 0%, #10b981 100%);
}

.quick-item.publish,
.quick-item.request,
.quick-item.service,
.quick-item.volunteer {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(247, 250, 248, 0.98) 100%);
}

.convenience-strip {
  appearance: none;
  width: 100%;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(246, 249, 247, 0.98) 100%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.52) inset,
    0 8px 18px rgba(15, 23, 42, 0.06);
  padding: 14px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  text-align: left;
}

.strip-ico {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f4f6f8;
  color: #334155;
  font-size: 21px;
}

.strip-main {
  display: grid;
  gap: 4px;
}

.strip-main b {
  color: #0f172a;
  font-size: 15px;
  line-height: 1.2;
}

.strip-main small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.strip-arrow {
  color: #94a3b8;
  font-size: 22px;
}

:global(.dark) .quick-item.publish,
:global(.dark) .quick-item.request,
:global(.dark) .quick-item.service,
:global(.dark) .quick-item.volunteer,
:global(.dark) .convenience-strip {
  background: #1f2937;
  border-color: #374151;
}

:global(.dark) .tone-ink,
:global(.dark) .strip-main b {
  color: #f3f4f6;
}

:global(.dark) .tone-muted,
:global(.dark) .strip-main small {
  color: #9ca3af;
}

:global(.dark) .q-ico.subtle {
  background: rgba(16, 185, 129, 0.18);
  border-color: rgba(16, 185, 129, 0.25);
  color: #86efac;
}

:global(.dark) .q-ico.soft {
  background: rgba(22, 101, 52, 0.22);
  border-color: rgba(134, 239, 172, 0.18);
  color: #bbf7d0;
}

:global(.dark) .q-ico.ink,
:global(.dark) .strip-ico {
  background: rgba(51, 65, 85, 0.34);
  border-color: rgba(148, 163, 184, 0.18);
  color: #cbd5e1;
}
</style>
