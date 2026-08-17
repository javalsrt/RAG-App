import request from './request'

export interface ExamQuestion {
  id: number
  type: 'single_choice' | 'multiple_choice' | 'true_false' | 'fill_blank' | 'short_answer'
  content: string
  options?: string[]
  score: number
  difficulty?: string
}

export interface ExamInfo {
  id: number
  type: 'exam' | 'homework'
  title: string
  description: string
  startTime: string
  endTime: string
  timeLimit: number
  totalScore: number
  passScore: number
  questionCount: number
}

export interface ExamDetail {
  exam: ExamInfo
  questions: ExamQuestion[]
  existingSubmission?: {
    submissionId: number
    status: string
    totalScore?: number
    autoScore?: number
  }
}

/** 学生获取考试详情+题目 */
export const getStudentExam = (id: number | string) =>
  request.get<any, ExamDetail>(`/student/exam/${id}`)

/** 学生提交答案 */
export const submitStudentExam = (id: number | string, data: { answers: Record<string, any> }) =>
  request.post<any, { submissionId: number; totalScore: number; autoScore?: number; msg: string }>(
    `/student/exam/${id}/submit`,
    data
  )

/** 教师查看学生提交列表 */
export const getExamSubmissions = (examId: number | string) =>
  request.get<any, any[]>(`/exam-homework/${examId}/submissions`)

/** 教师调整简答题分数 */
export const adjustSubmissionAnswerScore = (
  answerId: number | string,
  data: { finalScore: number; teacherComment?: string }
) =>
  request.post<any, { msg: string; newScore: number; remainingAdjust: number }>(
    `/exam-homework/submission-answer/${answerId}/adjust-score`,
    data
  )
