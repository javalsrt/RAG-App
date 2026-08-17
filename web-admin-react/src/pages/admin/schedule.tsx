import { useEffect, useMemo, useState } from 'react'
import {
  CalendarClock,
  Play,
  RotateCcw,
  Plus,
  Trash2,
  AlertCircle,
  CheckCircle2,
  Clock,
  Building2,
  BookOpen,
  Loader2,
  X,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useDialog } from '@/hooks/use-dialog'
import {
  autoGenerateSchedule,
  batchImportTasks,
  clearTeachingTasks,
  deleteTeachingTask,
  getClassrooms,
  getScheduleStats,
  getTeachingTasks,
  type ScheduleResult,
  type ScheduleStats,
  type TeachingTask,
} from '@/api/schedule'
import { getCurrentSemester } from '@/api/semester'
import { getTeacherCourses } from '@/api/courses'
import type { CourseItem } from '@/types'

const ROOM_TYPE_LABELS: Record<string, string> = {
  normal: '普通教室',
  lab: '专业实训室',
  computer: '计算机机房',
  music: '琴房',
  dance: '舞蹈室',
  art: '美术室',
  sports: '运动场',
}

const PERIOD_LABELS: Record<string, string> = {
  any: '不限',
  morning: '上午',
  afternoon: '下午',
}

const STATUS_LABELS: Record<string, { text: string; variant: 'default' | 'success' | 'danger' | 'warning' | 'secondary' }> = {
  pending: { text: '待排', variant: 'default' },
  scheduled: { text: '已排', variant: 'success' },
  failed: { text: '失败', variant: 'danger' },
  locked: { text: '锁定', variant: 'warning' },
}

export function SchedulePage() {
  const { alert, confirm, DialogComponent } = useDialog()
  const [activeTab, setActiveTab] = useState('overview')
  const [loading, setLoading] = useState(false)
  const [semester, setSemester] = useState('')
  const [stats, setStats] = useState<ScheduleStats | null>(null)
  const [tasks, setTasks] = useState<TeachingTask[]>([])
  const [classrooms, setClassrooms] = useState<Array<Record<string, any>>>([])
  const [courses, setCourses] = useState<CourseItem[]>([])
  const [lastResult, setLastResult] = useState<ScheduleResult | null>(null)

  const fetchSemester = async (): Promise<{ name: string } | null> => {
    try {
      const data = await getCurrentSemester()
      if (data?.name) {
        setSemester(data.name)
        return { name: data.name }
      }
      return null
    } catch {
      return null
    }
  }

  const fetchStats = async () => {
    if (!semester) return
    try {
      const data = await getScheduleStats(semester)
      setStats(data)
    } catch (err: any) {
      console.error('stats error', err)
    }
  }

  const fetchTasks = async () => {
    if (!semester) return
    try {
      const data = await getTeachingTasks({ semester })
      setTasks(data || [])
    } catch (err: any) {
      console.error('tasks error', err)
    }
  }

  const fetchClassrooms = async () => {
    try {
      const data = await getClassrooms()
      setClassrooms(data || [])
    } catch (err: any) {
      console.error('classrooms error', err)
    }
  }

  const fetchCourses = async (targetSemester?: string) => {
    const sem = targetSemester || semester
    try {
      const data = await getTeacherCourses()
      setCourses((data || []).filter((c: CourseItem) => c.semester === sem))
    } catch (err: any) {
      console.error('courses error', err)
    }
  }

  const refreshAll = async (targetSemester?: string) => {
    const sem = targetSemester || semester
    await Promise.all([
      fetchStats(),
      fetchTasks(),
      fetchClassrooms(),
    ])
  }

  useEffect(() => {
    fetchSemester().then((data) => {
      const sem = data?.name || ''
      refreshAll(sem)
      fetchCourses(sem)
    })
  }, [])

  useEffect(() => {
    if (semester) {
      refreshAll(semester)
      fetchCourses(semester)
    }
  }, [semester])

  /**
   * 根据当前学期已导入课程生成教学任务，返回生成结果数量信息
   */
  const generateTasksFromCourses = async (): Promise<{ success: boolean; count: number; message?: string }> => {
    const pendingTasks: TeachingTask[] = []
    courses.forEach((c) => {
      const classes = (c as any).classes || []
      const baseHours = c.hours ?? (c.credit ? Number(c.credit) : 8)
      // 默认每周2课时、2连堂；总课时<=4则按总课时一次排完
      const weeklyHours = baseHours <= 4 ? baseHours : 2
      const consecutive = weeklyHours >= 2 ? 2 : 1
      // 根据课程名智能判断教室类型
      const name = (c.courseName || '').toLowerCase()
      const preferredRoomType =
        name.includes('python') || name.includes('计算机') || name.includes('机器学习') || name.includes('深度学习')
          ? 'computer'
          : 'normal'

      if (classes.length > 0) {
        classes.forEach((cls: any) => {
          pendingTasks.push({
            semester: c.semester || semester,
            classId: cls.classId || 0,
            courseId: c.courseId || 0,
            teacherId: c.teacherId,
            weeklyHours,
            consecutive,
            preferredRoomType,
            preferredPeriod: 'any',
            priority: 5,
          })
        })
      } else if (c.classId) {
        pendingTasks.push({
          semester: c.semester || semester,
          classId: c.classId,
          courseId: c.courseId || 0,
          teacherId: c.teacherId,
          weeklyHours,
          consecutive,
          preferredRoomType,
          preferredPeriod: 'any',
          priority: 5,
        })
      }
    })

    if (pendingTasks.length === 0) {
      return { success: false, count: 0, message: '当前学期没有可用课程，请先到课程导入页导入课程' }
    }

    const result = await batchImportTasks(pendingTasks)
    await refreshAll()
    return {
      success: true,
      count: pendingTasks.length,
      message: `${result.message}${result.errors.length > 0 ? '\n失败：' + result.errors.join('；') : ''}`,
    }
  }

  const handleAutoSchedule = async () => {
    const confirmed = await confirm({
      title: '一键自动排课',
      description: `将对 ${semester || '当前学期'} 执行自动排课，已锁定的课不会被覆盖。是否继续？`,
      confirmText: '开始排课',
      cancelText: '取消',
    })
    if (!confirmed) return

    setLoading(true)
    setLastResult(null)
    try {
      // 如果没有待排/失败任务，自动从课程生成任务，实现“一键”闭环
      const actionableTasks = tasks.filter((t) => t.status === 'pending' || t.status === 'failed')
      if (actionableTasks.length === 0) {
        const generated = await generateTasksFromCourses()
        if (!generated.success) {
          await alert({ description: generated.message || '没有可用课程' })
          setLoading(false)
          return
        }
        // 生成成功后 tasks 状态已刷新，但当前 tasks 变量未更新，重新获取一次确保后续排课使用最新数据
        await fetchTasks()
      }

      const result = await autoGenerateSchedule(semester || undefined, true)
      setLastResult(result)
      await alert({
        title: result.failed > 0 ? '排课完成（含失败）' : '排课成功',
        description: result.message,
      })
      await refreshAll()
    } catch (err: any) {
      await alert({
        title: '排课失败',
        description: err?.response?.data?.message || err?.response?.data?.error || err.message || '未知错误',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateTasks = async () => {
    const confirmed = await confirm({
      title: '从课程生成教学任务',
      description: `将根据 ${semester || '当前学期'} 的已导入课程自动生成教学任务。是否继续？`,
      confirmText: '生成',
      cancelText: '取消',
    })
    if (!confirmed) return

    setLoading(true)
    try {
      const result = await generateTasksFromCourses()
      if (!result.success) {
        await alert({ description: result.message || '生成失败' })
        return
      }
      await alert({
        title: '生成完成',
        description: result.message,
      })
    } catch (err: any) {
      await alert({
        description: err?.response?.data?.message || err?.response?.data?.error || err.message || '生成失败',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleClearTasks = async () => {
    const confirmed = await confirm({
      title: '清空教学任务',
      description: `确定清空 ${semester || '当前学期'} 的所有教学任务吗？此操作不可恢复。`,
      confirmText: '清空',
      cancelText: '取消',
      variant: 'danger',
    })
    if (!confirmed) return

    setLoading(true)
    try {
      await clearTeachingTasks(semester || undefined)
      await alert({ description: '教学任务已清空' })
      await refreshAll()
    } catch (err: any) {
      await alert({
        description: err?.response?.data?.message || err?.response?.data?.error || err.message || '清空失败',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteTask = async (task: TeachingTask) => {
    if (!task.id) return
    const confirmed = await confirm({
      title: '删除教学任务',
      description: `确定删除「${task.courseName || ''}」的教学任务吗？`,
      confirmText: '删除',
      cancelText: '取消',
      variant: 'danger',
    })
    if (!confirmed) return

    setLoading(true)
    try {
      await deleteTeachingTask(task.id)
      await refreshAll()
    } catch (err: any) {
      await alert({
        description: err?.response?.data?.message || err?.response?.data?.error || err.message || '删除失败',
      })
    } finally {
      setLoading(false)
    }
  }

  const pendingCount = useMemo(() => tasks.filter((t) => t.status === 'pending').length, [tasks])
  const failedCount = useMemo(() => tasks.filter((t) => t.status === 'failed').length, [tasks])

  return (
    <div className="space-y-6">
      {DialogComponent}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">排课管理</h1>
          <p className="text-neutral-500 mt-1 text-sm">
            管理员录入教学任务，系统自动排课，师生按角色查看课表
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => refreshAll()}
            disabled={loading}
          >
            <RotateCcw className="w-4 h-4" />
            刷新
          </Button>
          <Button
            variant="default"
            className="gap-2"
            onClick={handleAutoSchedule}
            disabled={loading || !semester}
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
            一键自动排课
          </Button>
        </div>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-primary-50 flex items-center justify-center text-primary-600">
              <BookOpen className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-neutral-900">{stats?.total ?? '-'}</div>
              <div className="text-sm text-neutral-500">教学任务总数</div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-success/10 flex items-center justify-center text-success">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-neutral-900">{stats?.scheduled ?? '-'}</div>
              <div className="text-sm text-neutral-500">已排课</div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-warning/10 flex items-center justify-center text-warning">
              <Clock className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-neutral-900">{stats?.pending ?? '-'}</div>
              <div className="text-sm text-neutral-500">待排课</div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-danger/10 flex items-center justify-center text-danger">
              <AlertCircle className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl font-bold text-neutral-900">{stats?.failed ?? '-'}</div>
              <div className="text-sm text-neutral-500">排课失败</div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 学期提示 */}
      <div className="flex items-center gap-2 text-sm text-neutral-600 bg-white border border-neutral-200 rounded-xl px-4 py-3">
        <CalendarClock className="w-4 h-4 text-primary-500" />
        当前操作学期：
        <span className="font-medium text-neutral-900">{semester || '未设置'}</span>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="overview">任务与结果</TabsTrigger>
          <TabsTrigger value="classrooms">教室资源</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6 space-y-6">
          {/* 操作区 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">教学任务维护</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-3">
                <Button
                  variant="outline"
                  className="gap-2"
                  onClick={handleGenerateTasks}
                  disabled={loading || !semester}
                >
                  <Plus className="w-4 h-4" />
                  从课程生成任务
                </Button>
                <Button
                  variant="outline"
                  className="gap-2 text-danger hover:text-danger"
                  onClick={handleClearTasks}
                  disabled={loading || tasks.length === 0}
                >
                  <Trash2 className="w-4 h-4" />
                  清空任务
                </Button>
              </div>
              <p className="text-xs text-neutral-500">
                提示：点击"从课程生成任务"会读取当前学期已导入的课程，按默认规则生成教学任务；您也可以在后端直接维护 teaching_task 表后刷新查看。
              </p>
            </CardContent>
          </Card>

          {/* 排课结果 */}
          {lastResult && (
            <Card className={lastResult.failed > 0 ? 'border-warning' : 'border-success'}>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle className="text-base">最近一次排课结果</CardTitle>
                <Button variant="ghost" size="icon" className="w-8 h-8" onClick={() => setLastResult(null)}>
                  <X className="w-4 h-4" />
                </Button>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="text-sm text-neutral-700">{lastResult.message}</div>
                {lastResult.failures && lastResult.failures.length > 0 && (
                  <div className="space-y-2">
                    <div className="text-sm font-medium text-neutral-900">失败详情</div>
                    <div className="bg-neutral-50 rounded-lg border border-neutral-200 divide-y divide-neutral-200 max-h-60 overflow-auto">
                      {lastResult.failures.map((f, idx) => (
                        <div key={idx} className="px-4 py-3 text-sm">
                          <div className="font-medium text-danger">{f.courseName}</div>
                          <div className="text-neutral-500">{f.reason}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* 任务列表 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">
                教学任务列表
                <span className="ml-2 text-sm font-normal text-neutral-500">({tasks.length})</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-neutral-200">
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">课程</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">班级</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">教师</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">周课时</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">连堂</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">教室偏好</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">时段偏好</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">状态</th>
                      <th className="text-right px-4 py-3 text-sm font-medium text-neutral-500">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tasks.map((task) => {
                      const status = STATUS_LABELS[task.status || 'pending']
                      return (
                        <tr key={task.id} className="border-b border-neutral-100 hover:bg-neutral-50">
                          <td className="px-4 py-3 text-sm text-neutral-900">{task.courseName || '-'}</td>
                          <td className="px-4 py-3 text-sm text-neutral-700">{task.classId || '-'}</td>
                          <td className="px-4 py-3 text-sm text-neutral-700">{task.teacherName || '-'}</td>
                          <td className="px-4 py-3 text-sm text-neutral-700">{task.weeklyHours} 节</td>
                          <td className="px-4 py-3 text-sm text-neutral-700">{task.consecutive} 节</td>
                          <td className="px-4 py-3 text-sm text-neutral-700">
                            {ROOM_TYPE_LABELS[task.preferredRoomType || 'normal'] || task.preferredRoomType}
                          </td>
                          <td className="px-4 py-3 text-sm text-neutral-700">
                            {PERIOD_LABELS[task.preferredPeriod || 'any'] || task.preferredPeriod}
                          </td>
                          <td className="px-4 py-3">
                            <Badge variant={status.variant}>{status.text}</Badge>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="w-8 h-8 text-danger"
                              onClick={() => handleDeleteTask(task)}
                              disabled={loading}
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </td>
                        </tr>
                      )
                    })}
                    {tasks.length === 0 && (
                      <tr>
                        <td colSpan={9} className="px-4 py-12 text-center text-neutral-400">
                          暂无教学任务，点击上方"从课程生成任务"或手动录入
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="classrooms" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Building2 className="w-5 h-5" />
                教室资源
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-neutral-200">
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">教室名称</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">类型</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">容量</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">楼宇</th>
                      <th className="text-left px-4 py-3 text-sm font-medium text-neutral-500">楼层</th>
                    </tr>
                  </thead>
                  <tbody>
                    {classrooms.map((room) => (
                      <tr key={room.id} className="border-b border-neutral-100 hover:bg-neutral-50">
                        <td className="px-4 py-3 text-sm text-neutral-900">{room.name}</td>
                        <td className="px-4 py-3 text-sm text-neutral-700">
                          {ROOM_TYPE_LABELS[room.type] || room.type}
                        </td>
                        <td className="px-4 py-3 text-sm text-neutral-700">{room.capacity} 人</td>
                        <td className="px-4 py-3 text-sm text-neutral-700">{room.building || '-'}</td>
                        <td className="px-4 py-3 text-sm text-neutral-700">{room.floor ?? '-'}</td>
                      </tr>
                    ))}
                    {classrooms.length === 0 && (
                      <tr>
                        <td colSpan={5} className="px-4 py-12 text-center text-neutral-400">
                          暂无教室数据
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
