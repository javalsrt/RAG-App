import request from './request'
import type {
  ExamHomeworkItem,
  ExamHomeworkListParams,
  ExamHomeworkListResult,
  ExamHomeworkPublishRequest,
  ClassInfo,
  CourseItem,
  QuestionPreview,
} from '@/types'

// 班级列表（复用教师班级接口）
export const getTeacherClasses = () => {
  return request.get<any, ClassInfo[]>('/teacher/class/list')
}

// 教师课程列表
export const getTeacherCoursesForSelect = () => {
  return request.get<any, CourseItem[]>('/schedule/teacher/courses')
}

// 考试作业列表
export const getExamHomeworkList = (params: ExamHomeworkListParams) => {
  return request.get<any, ExamHomeworkListResult>('/exam-homework/list', { params })
}

// 发布考试/作业
export const publishExamHomework = (data: ExamHomeworkPublishRequest) => {
  return request.post<any, { id: number }>('/exam-homework/publish', data)
}

export interface GenerateQuestionsResult {
  questions: QuestionPreview[]
  error?: string
  failedTypes?: string[]
}

// AI 按范围生成题目预览
export const generateQuestionsByRange = async (data: {
  courseId?: number
  chapterIds?: number[]
  questionTypes: string[]
  difficulty: string
  count: number
}) => {
  const res = await request.post<any, GenerateQuestionsResult>(
    '/exam-homework/generate-by-range',
    data
  )
  if (res.error && (!res.questions || res.questions.length === 0)) {
    throw new Error(res.error)
  }
  return res
}

// AI 识别文档生成题目预览
export const generateQuestionsByDocument = async (data: FormData) => {
  const res = await request.post<any, GenerateQuestionsResult>(
    '/exam-homework/generate-by-document',
    data,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    }
  )
  if (res.error && (!res.questions || res.questions.length === 0)) {
    throw new Error(res.error)
  }
  return res
}

// 删除考试/作业
export const deleteExamHomework = (id: number) => {
  return request.delete(`/exam-homework/${id}`)
}

// 上架/下架考试作业
export const toggleExamHomeworkStatus = (id: number, status: number) => {
  return request.put(`/exam-homework/${id}/status`, { status })
}
