import request from './request'
import type { Semester, SemesterCurrent } from '@/types'

// 当前学期
export const getCurrentSemester = () => {
  return request.get<any, SemesterCurrent>('/semester/current')
}

// 学期列表
export const getSemesterList = () => {
  return request.get<any, Semester[]>('/semester/list')
}

// 新增学期
export const createSemester = (data: {
  name: string
  startDate: string
  endDate?: string
  weekCount?: number
  semesterType?: string
  classIds?: number[]
}) => {
  return request.post('/semester', data)
}

// 修改学期
export const updateSemester = (
  semesterId: number,
  data: {
    name: string
    startDate: string
    endDate?: string
    weekCount?: number
    semesterType?: string
    classIds?: number[]
  }
) => {
  return request.put(`/semester/${semesterId}`, data)
}

// 切换当前学期
export const switchSemester = (semesterId: number) => {
  return request.put(`/semester/switch/${semesterId}`)
}

// 删除学期
export const deleteSemester = (semesterId: number) => {
  return request.delete(`/semester/${semesterId}`)
}
