import request from './request'

export interface Classroom {
  id: number
  name: string
  type: string
  capacity: number
  building?: string
  floor?: number
  equipment?: string
  isActive: number
}

export interface TeachingTask {
  id?: number
  semester: string
  classId: number
  courseId: number
  courseName?: string
  teacherId?: number
  teacherName?: string
  weeklyHours: number
  consecutive: number
  preferredRoomType?: string
  preferredPeriod?: string
  priority?: number
  status?: string
  failReason?: string
}

export interface ScheduleResult {
  totalTasks: number
  scheduled: number
  failed: number
  partialScheduled: number
  elapsedMs: number
  message: string
  failures: Array<{
    taskId: number
    courseName: string
    classId: number
    reason: string
  }>
}

export interface ScheduleStats {
  semester: string
  total: number
  scheduled: number
  failed: number
  pending: number
  locked: number
}

export interface BatchImportResult {
  created: number
  updated: number
  errors: string[]
  message: string
}

// 教室管理
export const getClassrooms = (type?: string) =>
  request.get<any, Classroom[]>('/admin/schedule/classrooms', { params: type ? { type } : undefined })

export const addClassroom = (data: Omit<Classroom, 'id'>) =>
  request.post<any, { id: number; message: string }>('/admin/schedule/classroom', data)

export const updateClassroom = (id: number, data: Partial<Classroom>) =>
  request.put<any, { message: string }>(`/admin/schedule/classroom/${id}`, data)

export const deleteClassroom = (id: number) =>
  request.delete<any, { message: string }>(`/admin/schedule/classroom/${id}`)

// 教学任务管理
export const getTeachingTasks = (params?: { semester?: string; classId?: number; status?: string }) =>
  request.get<any, TeachingTask[]>('/admin/schedule/tasks', { params })

export const batchImportTasks = (tasks: TeachingTask[]) =>
  request.post<any, BatchImportResult>('/admin/schedule/tasks/batch', tasks)

export const deleteTeachingTask = (id: number) =>
  request.delete<any, { message: string }>(`/admin/schedule/task/${id}`)

export const clearTeachingTasks = (semester?: string) =>
  request.delete<any, { message: string }>('/admin/schedule/tasks/clear', { params: semester ? { semester } : undefined })

// 一键自动排课
export const autoGenerateSchedule = (semester?: string, clearExisting = true) =>
  request.post<any, ScheduleResult>('/admin/schedule/auto-generate', null, {
    params: { semester, clearExisting },
  })

// 排课统计
export const getScheduleStats = (semester?: string) =>
  request.get<any, ScheduleStats>('/admin/schedule/stats', { params: semester ? { semester } : undefined })
