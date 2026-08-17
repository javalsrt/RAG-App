import request from './request'
import type { ImportPreviewResult, ImportConfirmResult, ImportPreviewItem, CourseImportRecord } from '@/types'

// 上传文件 → AI 提取课表 → 预览
export const previewImport = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ImportPreviewResult>('/schedule/import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 确认导入
export const confirmImport = (data: ImportPreviewItem[], fileName?: string) => {
  return request.post<any, ImportConfirmResult>('/schedule/import/confirm', { items: data, fileName })
}

// 查询导入记录
export const getImportRecords = () => {
  return request.get<any, CourseImportRecord[]>('/schedule/import/records')
}
