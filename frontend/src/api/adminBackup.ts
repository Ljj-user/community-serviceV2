import HttpClient from '~/common/api/http-client'

export type BackupStatus = 'SUCCESS' | 'FAILED' | 'RUNNING'

export interface BackupRecordVO {
  id: number
  recordType: 'BACKUP' | 'RESTORE' | 'EXPORT'
  module?: string
  format?: string
  filename?: string
  filePath?: string
  fileSizeMb?: number | string
  status: BackupStatus
  note?: string
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface BackupScheduleDTO {
  enabled: boolean
  cycle: 'daily' | 'weekly' | 'monthly'
  time: string // HH:mm
  keepDays: number
}

const client = HttpClient()

export async function backupRun() {
  const res = await client.post('admin/backup/run', null)
  return res.data as BackendResult<BackupRecordVO>
}

export async function backupHistory(params?: { page?: number; size?: number }) {
  const res = await client.get('admin/backup/history', { params })
  return res.data as BackendResult<PageResult<BackupRecordVO>>
}

export async function backupDelete(id: number) {
  const res = await client.delete(`admin/backup/${id}`)
  return res.data as BackendResult<null>
}

export async function backupGetSchedule() {
  const res = await client.get('admin/backup/schedule')
  return res.data as BackendResult<BackupScheduleDTO>
}

export async function backupSaveSchedule(dto: BackupScheduleDTO) {
  const res = await client.put('admin/backup/schedule', dto)
  return res.data as BackendResult<null>
}

export async function backupRestore(file: File) {
  const form = new FormData()
  form.append('file', file)
  const res = await client.post('admin/backup/restore', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data as BackendResult<BackupRecordVO>
}

export async function backupDownload(id: number) {
  return client.get(`admin/backup/download/${id}`, {
    responseType: 'blob',
  })
}
