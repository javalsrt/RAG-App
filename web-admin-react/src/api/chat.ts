import request from './request'

/** 聊天消息（与后端 ChatMessageDTO 对应） */
export interface ChatMessage {
  id: number
  courseName: string
  userId: number
  senderName?: string
  senderRole: 'student' | 'teacher' | 'ai' | 'system'
  content?: string
  createdAt?: string
  bizType?: string
  bizId?: number
}

/** 课程下的学生（用于 @ 点名） */
export interface CourseStudent {
  id: number
  realName: string
}

/** 教师未读聊天通知 */
export interface TeacherChatNotification {
  id: number
  courseName: string
  senderName?: string
  senderRole: 'student' | 'teacher' | 'ai' | 'system'
  content?: string
  preview?: string
  createdAt?: string
}

/** 获取教师未读聊天通知 */
export const getTeacherUnreadNotifications = () => {
  return request.get<any, TeacherChatNotification[]>('/chat/teacher/unread')
}

/** 教师进入课程聊天时，将该课程下学生和 AI 消息标记为已读 */
export const markTeacherRead = (courseName: string) => {
  return request.post<any, { msg: string; rows: string }>('/chat/teacher/read', { courseName })
}

/** 获取课程公开聊天（教师端群聊，可看到全员消息） */
export const getPublicMessages = (courseName: string) => {
  return request.get<any, ChatMessage[]>(`/chat/${encodeURIComponent(courseName)}/public`)
}

/** 发送消息（senderRole='teacher' 表示教师身份，全体学生可见） */
export const sendChatMessage = (courseName: string, content: string, senderRole = 'teacher') => {
  return request.post<any, ChatMessage>('/chat/send', { courseName, content, senderRole })
}

/** 上传聊天文件（图片/文档），返回 URL */
export const uploadChatFile = (courseName: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  form.append('courseName', courseName)
  return request.post<any, { url: string; fileName: string }>('/chat/upload-file', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 获取该课程的学生列表（用于 @ 点名） */
export const getCourseStudents = (courseName: string) => {
  return request.get<any, CourseStudent[]>(`/chat/${encodeURIComponent(courseName)}/students`)
}