import { useAuthStore } from '@/store/auth'

export function useRole() {
  const { user, roles, permissions, hasRole, hasPermission } = useAuthStore()

  const isAdmin = user?.role === 'admin' || roles.includes('admin')
  const isTeacher = user?.role === 'teacher' || roles.includes('teacher')
  const isStudent = user?.role === 'student' || roles.includes('student')

  return {
    user,
    role: user?.role || null,
    roles,
    permissions,
    isAdmin,
    isTeacher,
    isStudent,
    hasRole,
    hasPermission,
  }
}
