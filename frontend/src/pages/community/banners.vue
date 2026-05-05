<route lang="yaml">
meta:
  title: communityBanners
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityBanners
</route>

<script setup lang="ts">
import type { DataTableColumns, FormInst, UploadCustomRequestOptions } from 'naive-ui'
import { NButton, NPopconfirm } from 'naive-ui'
import { h } from 'vue'
import { bannerDelete, bannerList, bannerUploadImage, bannerUpsert, type BannerVO } from '~/api/adminBanners'
import { adminCommunityOptions } from '~/api/adminCommunity'
import { storeToRefs } from 'pinia'

const { t, locale } = useI18n()
const message = useMessage()
const accountStore = useAccountStore()
const { user } = storeToRefs(accountStore)

const isSuperAdmin = computed(() => user.value?.role === 1)

const loading = ref(false)
const rows = ref<BannerVO[]>([])
const communityOptions = ref<Array<{ label: string, value: number }>>([])

const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInst | null>(null)
const saving = ref(false)
const uploading = ref(false)

const form = reactive({
  id: null as null | number,
  communityId: null as null | number,
  title: '',
  subtitle: '',
  imageUrl: '',
  linkUrl: '',
  sortNo: 0,
  status: 1 as 0 | 1,
})

async function load() {
  loading.value = true
  try {
    const res = await bannerList(isSuperAdmin.value ? { communityId: form.communityId } : undefined)
    if (res.code !== 200)
      throw new Error(res.message || '加载失败')
    rows.value = res.data || []
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCommunities() {
  const res = await adminCommunityOptions()
  if (res.code !== 200) return
  communityOptions.value = (res.data || []).map(x => ({ label: `${x.name}（${x.id}）`, value: x.id }))
}

function openCreate() {
  modalMode.value = 'create'
  form.id = null
  form.title = ''
  form.subtitle = ''
  form.imageUrl = ''
  form.linkUrl = ''
  form.sortNo = 0
  form.status = 1
  showModal.value = true
}

function openEdit(r: BannerVO, idx: number) {
  modalMode.value = 'edit'
  form.id = r.id
  form.title = r.title
  form.subtitle = r.subtitle || ''
  form.imageUrl = r.imageUrl || ''
  form.linkUrl = r.linkUrl || ''
  form.sortNo = idx
  form.status = 1
  showModal.value = true
}

async function submit() {
  if (saving.value) return
  saving.value = true
  try {
    if (!form.title.trim())
      throw new Error('标题不能为空')
    if (isSuperAdmin.value && form.communityId === null) {
      // 超管默认管理全局 banner（communityId=null）
    }
    const res = await bannerUpsert({
      id: form.id ?? undefined,
      communityId: isSuperAdmin.value ? form.communityId : undefined,
      title: form.title,
      subtitle: form.subtitle || undefined,
      imageUrl: form.imageUrl || undefined,
      linkUrl: form.linkUrl || undefined,
      sortNo: form.sortNo,
      status: form.status,
    })
    if (res.code !== 200)
      throw new Error(res.message || '保存失败')
    message.success('保存成功')
    showModal.value = false
    await load()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleBannerUpload(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) {
    options.onError()
    return
  }
  uploading.value = true
  try {
    const res = await bannerUploadImage(file)
    if (res.code !== 200 || !res.data?.imageUrl) {
      throw new Error(res.message || '上传失败')
    }
    form.imageUrl = res.data.imageUrl
    message.success('图片上传成功')
    options.onFinish()
  } catch (e: any) {
    message.error(e?.message || '上传失败')
    options.onError()
  } finally {
    uploading.value = false
  }
}

async function remove(r: BannerVO) {
  try {
    const res = await bannerDelete(r.id, isSuperAdmin.value ? { communityId: form.communityId } : undefined)
    if (res.code !== 200)
      throw new Error(res.message || '删除失败')
    message.success('删除成功')
    await load()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

const columns = computed<DataTableColumns<BannerVO>>(() => {
  void locale.value
  return [
    { title: '所属社区', key: 'communityName', render: r => r.communityName ? `${r.communityName}（${r.communityId ?? '-'}）` : '全局默认' },
    { title: '标题', key: 'title' },
    { title: '副标题', key: 'subtitle' },
    { title: '图片', key: 'imageUrl', ellipsis: { tooltip: true } },
    { title: '跳转', key: 'linkUrl', ellipsis: { tooltip: true } },
    {
      title: '操作',
      key: 'actions',
      render: (r, idx) => {
        return h('div', { class: 'flex gap-2' }, [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: (e: MouseEvent) => {
                e.stopPropagation()
                openEdit(r, idx)
              },
            },
            { default: () => '编辑' },
          ),
          h(
            NPopconfirm,
            { onPositiveClick: () => remove(r) },
            {
              default: () => '确认删除该轮播？',
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', tertiary: true, type: 'error', onClick: (e: MouseEvent) => e.stopPropagation() },
                  { default: () => '删除' },
                ),
            },
          ),
        ])
      },
    },
  ]
})

onMounted(() => {
  load()
  loadCommunities()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">{{ t('menu.communityBanners') }}</h1>
        <p class="text-sm text-slate-500">
          移动端首页轮播图将按“用户所属社区”优先展示；没有配置则回退到全局默认。
        </p>
      </div>
      <div class="flex gap-2 items-center">
        <n-button :loading="loading" @click="load">刷新</n-button>
        <n-button type="primary" @click="openCreate">新增轮播</n-button>
      </div>
    </div>

    <n-card :bordered="false">
      <div v-if="isSuperAdmin" class="mb-3 flex items-center gap-2">
        <span class="text-sm text-slate-500">按社区筛选：</span>
        <n-select v-model:value="form.communityId" :options="communityOptions" clearable filterable placeholder="空=全局默认" class="w-64" />
        <n-button size="small" @click="load">切换</n-button>
      </div>
      <n-data-table :bordered="false" size="small" :columns="columns" :data="rows" :loading="loading" :row-props="(row, idx) => ({ onClick: () => openEdit(row, idx), style: 'cursor: pointer;' })" />
    </n-card>

    <n-modal v-model:show="showModal" preset="card" :title="modalMode==='create' ? '新增轮播' : '编辑轮播'" style="width: min(720px, 92vw);">
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item v-if="isSuperAdmin" label="所属社区">
          <n-select v-model:value="form.communityId" :options="communityOptions" clearable filterable placeholder="空=全局默认" />
        </n-form-item>
        <n-form-item label="标题">
          <n-input v-model:value="form.title" placeholder="例如：社区关怀日" />
        </n-form-item>
        <n-form-item label="副标题">
          <n-input v-model:value="form.subtitle" placeholder="例如：优先帮助独居老人…" />
        </n-form-item>
        <n-form-item label="轮播图片">
          <div class="w-full space-y-2">
            <n-upload
              :default-upload="false"
              :custom-request="handleBannerUpload"
              accept="image/*"
            >
              <n-button :loading="uploading">上传图片</n-button>
            </n-upload>
            <n-input v-model:value="form.imageUrl" placeholder="上传后自动回填，可手工微调" />
            <img v-if="form.imageUrl" :src="form.imageUrl" alt="banner preview" class="max-h-36 rounded border border-slate-200" />
          </div>
        </n-form-item>
        <n-form-item label="点击跳转URL（可空）">
          <n-input v-model:value="form.linkUrl" placeholder="https:// 或 #/ 路由" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="showModal=false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="submit">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

