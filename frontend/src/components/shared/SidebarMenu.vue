<script setup lang="ts">
import type { MenuInst, MenuOption } from 'naive-ui/es/components'

defineModel<string>()
export interface SidebarMenuOption {
  type?: string
  label?: string
  key?: string
  icon?: any
  activeIcon?: any
  selectedIcon?: any
  isNew?: boolean
  showBadge?: boolean
  route?: string
  children?: SidebarMenuOption[]
  props?: any
}

export interface Props {
  options: SidebarMenuOption[]
}

const props = defineProps<Props>()
const route = useRoute()
const selectedMenuKey = ref('dashboard')
const menuRef = ref<MenuInst | null>(null)
const { renderIcon, renderLabel } = useRender()

onMounted(() => activateCurrentRoute())

function normalizePath(p: string) {
  if (!p || p === '/')
    return '/'
  return p.replace(/\/+$/, '') || '/'
}

function flattenMenuOptions(options: SidebarMenuOption[]): SidebarMenuOption[] {
  const out: SidebarMenuOption[] = []
  for (const o of options) {
    if (o.route)
      out.push(o)
    if (o.children?.length)
      out.push(...flattenMenuOptions(o.children))
  }
  return out
}

function activateCurrentRoute() {
  setTimeout(() => {
    const flat = flattenMenuOptions(props.options)
    const path = normalizePath(route.path)

    const byPath = flat.find((s) => {
      if (!s.route)
        return false
      const r = normalizePath(s.route)
      return path === r || path.startsWith(`${r}/`)
    })

    const byName = route.name
      ? flat.find(
          s =>
            s.key
            && String(route.name).toLowerCase() === String(s.key).toLowerCase(),
        )
      : undefined

    selectedMenuKey.value =
      byPath?.key
      ?? byName?.key
      ?? flat[0]?.key
      ?? 'dashboard-home'

    menuRef.value?.showOption(selectedMenuKey.value)
  }, 20)
}

watch(
  () => [route.name, route.path],
  () => {
    setTimeout(() => activateCurrentRoute(), 200)
  },
)

const items = computed(() =>
  props.options.map((o: SidebarMenuOption) => convertToMenuOption(o)),
)

function convertToMenuOption(item: SidebarMenuOption): MenuOption {
  return {
    type: item.type,
    props: item.props,
    label: item.route
      ? () =>
          item.label !== undefined
            ? renderLabel(item.label, item.route!, item.isNew ?? false)
            : () => null
      : () => item.label,
    icon:
      item.type === 'group'
        ? () => null
        : renderIcon(
            isActiveRoute(item) && item.activeIcon
              ? item.activeIcon
              : item.icon,
            item.showBadge,
          ),
    key: item.key,
    children: item.children?.map((i) => convertToMenuOption(i)),
  }
}

function isActiveRoute(item: SidebarMenuOption) {
  return selectedMenuKey.value.toLowerCase() === item.key?.toLowerCase()
}
</script>

<template>
  <!-- accordion=false：可同时展开「业务管理 / 系统管理 / 应用程序 / 设置」等多级菜单 -->
  <n-menu ref="menuRef" v-bind="$attrs" :accordion="false" v-model:value="selectedMenuKey" :options="items" />
</template>


<style scoped></style>