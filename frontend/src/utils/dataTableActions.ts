import { NButton, NIcon, NPopconfirm } from 'naive-ui'
import type { Component } from 'vue'
import { h } from 'vue'

/** 与「用户管理」操作列一致的容器样式 */
export const dtActionRowClass = 'flex flex-wrap items-center gap-2'

/**
 * 表格操作列按钮：small + tertiary + 语义色 + 可选 Fluent 图标
 */
export function dtActionBtn(
  label: string,
  opts: {
    type?: 'default' | 'info' | 'success' | 'warning' | 'error' | 'primary'
    onClick?: () => void
  },
  icon?: Component,
) {
  return h(
    NButton,
    {
      size: 'small',
      tertiary: true,
      type: opts.type ?? 'info',
      onClick: (e: MouseEvent) => {
        e.stopPropagation()
        opts.onClick?.()
      },
    },
    icon
      ? {
          default: () => label,
          icon: () => h(NIcon, { size: 16 }, { default: () => h(icon) }),
        }
      : { default: () => label },
  )
}

/**
 * 删除类操作：NPopconfirm + 错误色按钮；onPositiveClick 返回 false 可阻止关闭
 */
export function dtActionDelete(
  label: string,
  confirmText: string,
  onPositiveClick: () => boolean | undefined | Promise<boolean | undefined>,
  opts: { positiveText: string; negativeText: string },
  icon?: Component,
) {
  return h(
    NPopconfirm,
    {
      positiveText: opts.positiveText,
      negativeText: opts.negativeText,
      onPositiveClick,
    },
    {
      trigger: () =>
        h(
          NButton,
          {
            size: 'small',
            type: 'error',
            tertiary: true,
            onClick: (e: MouseEvent) => e.stopPropagation(),
          },
          icon
            ? {
                default: () => label,
                icon: () => h(NIcon, { size: 16 }, { default: () => h(icon) }),
              }
            : { default: () => label },
        ),
      default: () => confirmText,
    },
  )
}
