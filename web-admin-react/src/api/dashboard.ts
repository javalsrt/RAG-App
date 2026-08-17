import request from './request'
import type { TeacherStats, TrendItem } from '@/types'

// 教师数据总览（管理员也用这个）
export const getTeacherStats = () => {
  return request.get<any, TeacherStats>('/teacher/stats')
}

// 近7天学习时长趋势
export const getTeacherTrend = () => {
  return request.get<any, TrendItem[]>('/teacher/trend')
}
