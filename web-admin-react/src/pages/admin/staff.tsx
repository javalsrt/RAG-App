import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  Search,
  Plus,
  MoreHorizontal,
  Upload,
  ChevronRight,
  Download,
  Loader2,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
} from '@/components/ui/sheet'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import {
  getUserList,
  getUserOverview,
  createUser,
  updateUser,
  resetPassword,
  deleteUser,
  getClasses,
  importStudents,
} from '@/api/staff'
import { Users, School, ChevronLeft, UserCheck, UserX } from 'lucide-react'
import type { UserListItem, ClassInfo } from '@/types'

const PAGE_SIZE = 10
const ROLE_STUDENT = 1
const ROLE_TEACHER = 2
const IMPORT_RESULT_KEY = 'admin_staff_import_result'

// 搜索维度配置：学生可用全部，教师仅适用姓名/账号/手机号
const STUDENT_SEARCH_FIELDS = [
  { value: 'all', label: '全部' },
  { value: 'realName', label: '姓名' },
  { value: 'username', label: '账号' },
  { value: 'studentNo', label: '学号' },
  { value: 'phone', label: '手机号' },
  { value: 'major', label: '专业' },
  { value: 'grade', label: '年级' },
  { value: 'className', label: '班级' },
]
const TEACHER_SEARCH_FIELDS = [
  { value: 'all', label: '全部' },
  { value: 'realName', label: '姓名' },
  { value: 'username', label: '账号' },
  { value: 'phone', label: '手机号' },
]
const SEARCH_PLACEHOLDER: Record<string, string> = {
  all: '搜索姓名/账号/学号/手机号...',
  realName: '搜索姓名...',
  username: '搜索账号...',
  studentNo: '搜索学号...',
  phone: '搜索手机号...',
  major: '搜索专业...',
  grade: '搜索年级...',
  className: '搜索班级名称...',
}

interface FormState {
  username: string
  password: string
  realName: string
  phone: string
  email: string
  studentNo: string
  classId: string
  major: string
  grade: string
}

const emptyForm: FormState = {
  username: '',
  password: '123456',
  realName: '',
  phone: '',
  email: '',
  studentNo: '',
  classId: '',
  major: '',
  grade: '',
}

function statusBadge(status: number) {
  return status === 1 ? (
    <Badge variant="success">启用</Badge>
  ) : (
    <Badge variant="secondary">禁用</Badge>
  )
}

export function StaffPage() {
  const location = useLocation()
  const initialTab = location.pathname === '/admin/students' ? 'students' : 'teachers'
  const [activeTab, setActiveTab] = useState<'teachers' | 'students'>(initialTab)
  const [searchInput, setSearchInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchField, setSearchField] = useState('all')
  const [overviewView, setOverviewView] = useState<'majors' | 'classes'>('majors')
  const [selectedMajor, setSelectedMajor] = useState<string | null>(null)
  const [overview, setOverview] = useState<{
    total: number
    enabled: number
    disabled: number
    majors: { name: string; count: number }[]
    classes: { id: number; name: string; major: string; count: number }[]
  } | null>(null)
  const [pageNum, setPageNum] = useState(1)
  const pageSize = PAGE_SIZE
  const [total, setTotal] = useState(0)
  const [list, setList] = useState<UserListItem[]>([])
  const [classes, setClasses] = useState<ClassInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  // Sheet state
  const [sheetOpen, setSheetOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserListItem | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  // Delete dialog
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deletingUser, setDeletingUser] = useState<UserListItem | null>(null)
  const [deleting, setDeleting] = useState(false)

  // Reset password dialog
  const [resetOpen, setResetOpen] = useState(false)
  const [resettingUser, setResettingUser] = useState<UserListItem | null>(null)
  const [newPassword, setNewPassword] = useState('123456')
  const [resetting, setResetting] = useState(false)

  // 更多菜单展开状态
  const [openMenuId, setOpenMenuId] = useState<number | null>(null)

  // Import
  const importInputRef = useRef<HTMLInputElement>(null)
  const [importing, setImporting] = useState(false)
  const [importResult, setImportResult] = useState<{
    success: boolean
    message: string
    total?: number
    imported?: number
    skipped?: number
    errors?: { row: number; studentNo: string; realName: string; errors: string[] }[]
  } | null>(null)

  const currentRole = activeTab === 'teachers' ? ROLE_TEACHER : ROLE_STUDENT
  const tabLabel = activeTab === 'teachers' ? '教师' : '学生'

  // Fetch classes once
  useEffect(() => {
    getClasses()
      .then((data) => setClasses(data || []))
      .catch(() => setClasses([]))
  }, [])

  // 恢复上次导入结果（离开页面再回来仍可看到，30 分钟内有效）
  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(IMPORT_RESULT_KEY)
      if (!raw) return
      const saved = JSON.parse(raw)
      if (
        saved &&
        typeof saved.success === 'boolean' &&
        Date.now() - (saved.ts || 0) < 30 * 60 * 1000
      ) {
        setImportResult(saved)
      } else {
        sessionStorage.removeItem(IMPORT_RESULT_KEY)
      }
    } catch {
      sessionStorage.removeItem(IMPORT_RESULT_KEY)
    }
  }, [])

  // 人员概览统计
  useEffect(() => {
    const fetchOverview = async () => {
      try {
        const data = await getUserOverview({ role: currentRole })
        setOverview(data)
      } catch {
        setOverview(null)
      }
    }
    fetchOverview()
  }, [currentRole])

  // Fetch list when tab / keyword / page changes
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      setError('')
      try {
        const res = await getUserList({
          role: currentRole,
          pageNum,
          pageSize,
          keyword: keyword || undefined,
          searchField: searchField || undefined,
        })
        setList(res?.list || [])
        setTotal(res?.total || 0)
      } catch (err: any) {
        setError(
          err.response?.data?.message ||
            err.response?.data?.error ||
            '数据加载失败'
        )
        setList([])
        setTotal(0)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [activeTab, keyword, pageNum, pageSize, currentRole, searchField])

  // Debounced search input -> keyword
  useEffect(() => {
    const t = setTimeout(() => {
      setKeyword(searchInput)
      setPageNum(1)
    }, 400)
    return () => clearTimeout(t)
  }, [searchInput])

  const totalPages = Math.max(1, Math.ceil(total / pageSize))

  const classNameById = (id?: number) => {
    if (!id) return '-'
    const c = classes.find((item) => item.id === id)
    return c ? c.className : '-'
  }

  const refreshList = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getUserList({
        role: currentRole,
        pageNum,
        pageSize,
        keyword: keyword || undefined,
        searchField: searchField || undefined,
      })
      setList(res?.list || [])
      setTotal(res?.total || 0)
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          '数据加载失败'
      )
    } finally {
      setLoading(false)
    }
  }

  const openCreateSheet = () => {
    setEditingUser(null)
    setForm({ ...emptyForm })
    setFormError('')
    setSheetOpen(true)
  }

  const openEditSheet = (user: UserListItem) => {
    setEditingUser(user)
    setForm({
      username: user.username || '',
      password: '',
      realName: user.realName || '',
      phone: user.phone || '',
      email: user.email || '',
      studentNo: user.studentNo || '',
      classId: user.classId ? String(user.classId) : '',
      major: user.major || '',
      grade: user.grade || '',
    })
    setFormError('')
    setSheetOpen(true)
  }

  const handleSubmit = async () => {
    setFormError('')
    if (!form.username.trim() || !form.realName.trim()) {
      setFormError('账号和姓名为必填项')
      return
    }
    if (!editingUser && !form.password.trim()) {
      setFormError('请填写初始密码')
      return
    }
    setSubmitting(true)
    try {
      const base = {
        username: form.username.trim(),
        realName: form.realName.trim(),
        phone: form.phone.trim() || undefined,
        email: form.email.trim() || undefined,
      }
      const studentFields =
        currentRole === ROLE_STUDENT
          ? {
              studentNo: form.studentNo.trim() || undefined,
              classId: form.classId ? Number(form.classId) : undefined,
              major: form.major.trim() || undefined,
              grade: form.grade.trim() || undefined,
            }
          : {}
      if (editingUser) {
        const payload: any = { ...base, ...studentFields }
        if (form.password) payload.password = form.password
        await updateUser(editingUser.id, payload)
      } else {
        const payload: any = {
          ...base,
          password: form.password.trim(),
          role: currentRole,
          ...studentFields,
        }
        await createUser(payload)
      }
      setSheetOpen(false)
      await refreshList()
    } catch (err: any) {
      setFormError(
        err.response?.data?.message || err.response?.data?.error || '操作失败'
      )
    } finally {
      setSubmitting(false)
    }
  }

  const openDeleteDialog = (user: UserListItem) => {
    setDeletingUser(user)
    setActionError('')
    setDeleteOpen(true)
  }

  const confirmDelete = async () => {
    if (!deletingUser) return
    setDeleting(true)
    setActionError('')
    try {
      await deleteUser(deletingUser.id)
      setDeleteOpen(false)
      setDeletingUser(null)
      if (list.length === 1 && pageNum > 1) {
        setPageNum(pageNum - 1)
      } else {
        await refreshList()
      }
    } catch (err: any) {
      setActionError(
        err.response?.data?.message || err.response?.data?.error || '删除失败'
      )
    } finally {
      setDeleting(false)
    }
  }

  const openResetDialog = (user: UserListItem) => {
    setResettingUser(user)
    setNewPassword('123456')
    setActionError('')
    setResetOpen(true)
  }

  const confirmResetPassword = async () => {
    if (!resettingUser) return
    if (!newPassword.trim()) {
      setActionError('请输入新密码')
      return
    }
    setResetting(true)
    setActionError('')
    try {
      await resetPassword(resettingUser.id, newPassword.trim())
      setResetOpen(false)
      setResettingUser(null)
    } catch (err: any) {
      setActionError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          '重置密码失败'
      )
    } finally {
      setResetting(false)
    }
  }

  const saveImportResult = (result: NonNullable<typeof importResult>) => {
    setImportResult(result)
    try {
      sessionStorage.setItem(
        IMPORT_RESULT_KEY,
        JSON.stringify({ ...result, ts: Date.now() })
      )
    } catch {
      // 忽略存储失败（如隐私模式禁用存储）
    }
  }

  const clearImportResult = () => {
    setImportResult(null)
    try {
      sessionStorage.removeItem(IMPORT_RESULT_KEY)
    } catch {
      // 忽略存储失败
    }
  }

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''
    setImporting(true)
    setActionError('')
    clearImportResult()
    try {
      const res: any = await importStudents(file)
      // 后端返回 { total, imported, skipped, errors, message }
      const success = res.imported > 0
      saveImportResult({
        success,
        message:
          res.message ||
          `导入完成：共${res.total}条，成功${res.imported}条，跳过${res.skipped}条`,
        total: res.total,
        imported: res.imported,
        skipped: res.skipped,
        errors: res.errors,
      })
      // 有成功导入的行才刷新列表
      if (res.imported > 0) {
        if (currentRole === ROLE_STUDENT) {
          await refreshList()
        } else {
          setActiveTab('students')
          setPageNum(1)
        }
      }
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        '导入失败，请检查文件后重试'
      saveImportResult({ success: false, message: msg })
    } finally {
      setImporting(false)
    }
  }

  const handleTabChange = (val: string) => {
    setActiveTab(val as 'teachers' | 'students')
    setPageNum(1)
    setSearchInput('')
    setKeyword('')
    setSearchField('all')
    setOverviewView('majors')
    setSelectedMajor(null)
    setActionError('')
  }

  const handleMajorClick = (major: string) => {
    setSelectedMajor(major)
    setOverviewView('classes')
  }

  const handleBackToMajors = () => {
    setOverviewView('majors')
    setSelectedMajor(null)
  }

  const handleClassClick = (className: string) => {
    // 自动下钻到学生列表：切换到学生 tab、搜索维度=班级、填入关键词
    setActiveTab('students')
    setSearchField('className')
    setSearchInput(className)
    setKeyword(className)
    setPageNum(1)
  }

  const goPrev = () => setPageNum((p) => Math.max(1, p - 1))
  const goNext = () => setPageNum((p) => Math.min(totalPages, p + 1))

  const updateField = (key: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  // 下载人员批量导入模板（Excel 格式，含填写说明）
  const downloadTemplate = () => {
    const link = document.createElement('a')
    link.href = '/template/student-import-template.xlsx'
    link.download = '人员批量导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="space-y-6">
      {/* 导入期间全屏遮罩：锁定页面操作，避免误点击打断 */}
      {importing && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-neutral-900/40 backdrop-blur-sm">
          <div className="flex items-center gap-4 rounded-2xl bg-white px-8 py-6 shadow-2xl">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <div>
              <div className="text-base font-semibold text-neutral-900">
                正在导入，请稍候...
              </div>
              <div className="text-sm text-neutral-500 mt-1">
                导入期间已锁定页面操作，请勿刷新或关闭页面
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Page header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">人员管理</h1>
          <p className="text-neutral-500 mt-1 text-sm">
            管理教师和学生账号信息
          </p>
        </div>
        <div className="flex items-center gap-2">
          <input
            ref={importInputRef}
            type="file"
            accept=".xlsx,.xls"
            className="hidden"
            onChange={handleImportFile}
          />
          <Button
            variant="outline"
            className="gap-2"
            onClick={downloadTemplate}
          >
            <Download className="w-4 h-4" />
            人员批量导入模板
          </Button>
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => importInputRef.current?.click()}
            disabled={importing}
            title="请使用标准模板上传 .xlsx/.xls 文件，仅支持导入学生名单，不要上传课程表、课表或其他非人员名单文件。"
          >
            {importing ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Upload className="w-4 h-4" />
            )}
            {importing ? '导入中...' : '批量导入'}
          </Button>
          <Button variant="default" className="gap-2" onClick={openCreateSheet}>
            <Plus className="w-4 h-4" />
            新增{tabLabel}
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-5">
          <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
            <div className="flex items-center gap-2 w-full md:w-auto">
              <select
                value={searchField}
                onChange={(e) => {
                  setSearchField(e.target.value)
                  setPageNum(1)
                }}
                className="h-10 px-2 rounded-md border border-neutral-200 bg-white text-sm text-neutral-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                {(activeTab === 'teachers' ? TEACHER_SEARCH_FIELDS : STUDENT_SEARCH_FIELDS).map(
                  (f) => (
                    <option key={f.value} value={f.value}>
                      {f.label}
                    </option>
                  )
                )}
              </select>
              <div className="relative flex-1 md:w-72">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
                <Input
                  placeholder={SEARCH_PLACEHOLDER[searchField]}
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  className="pl-11 h-10"
                />
              </div>
            </div>
            <div className="text-sm text-neutral-500">共 {total} 人</div>
          </div>
        </CardContent>
      </Card>

      {/* 人员概览卡片：专业 → 班级 → 列表 下钻 */}
      {overview && (
        <Card>
          <CardContent className="p-5">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                {overviewView === 'classes' ? (
                  <>
                    <button
                      onClick={handleBackToMajors}
                      className="flex items-center gap-1 text-sm text-neutral-500 hover:text-primary transition-colors"
                    >
                      <ChevronLeft className="w-4 h-4" />
                      返回专业
                    </button>
                    <span className="text-neutral-300">/</span>
                    <span className="text-sm font-medium text-neutral-900">
                      {selectedMajor}（班级分布）
                    </span>
                  </>
                ) : (
                  <span className="text-sm font-medium text-neutral-900">
                    专业分布
                  </span>
                )}
              </div>
              <div className="flex items-center gap-4 text-xs text-neutral-500">
                <span className="flex items-center gap-1">
                  <UserCheck className="w-3.5 h-3.5 text-success" />
                  启用 {overview.enabled}
                </span>
                <span className="flex items-center gap-1">
                  <UserX className="w-3.5 h-3.5 text-danger" />
                  禁用 {overview.disabled}
                </span>
              </div>
            </div>

            {overviewView === 'majors' ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
                {overview.majors.length > 0 ? (
                  overview.majors.map((m) => (
                    <button
                      key={m.name}
                      onClick={() => handleMajorClick(m.name)}
                      className="text-left p-3 rounded-lg border border-neutral-200 bg-neutral-50 hover:border-primary-300 hover:bg-primary-50 transition-colors group"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <div className="w-7 h-7 rounded-md bg-primary-100 text-primary flex items-center justify-center">
                          <School className="w-3.5 h-3.5" />
                        </div>
                        <span className="text-lg font-semibold text-neutral-900 group-hover:text-primary">
                          {m.count}
                        </span>
                      </div>
                      <div className="text-xs text-neutral-600 truncate" title={m.name}>
                        {m.name}
                      </div>
                    </button>
                  ))
                ) : (
                  <div className="col-span-full text-sm text-neutral-400 py-4 text-center">
                    暂无专业分布数据
                  </div>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
                {overview.classes
                  .filter((c) => c.major === selectedMajor)
                  .map((c) => (
                    <button
                      key={c.id}
                      onClick={() => handleClassClick(c.name)}
                      className="text-left p-3 rounded-lg border border-neutral-200 bg-neutral-50 hover:border-primary-300 hover:bg-primary-50 transition-colors group"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <div className="w-7 h-7 rounded-md bg-cyan-100 text-cyan-600 flex items-center justify-center">
                          <Users className="w-3.5 h-3.5" />
                        </div>
                        <span className="text-lg font-semibold text-neutral-900 group-hover:text-primary">
                          {c.count}
                        </span>
                      </div>
                      <div className="text-xs text-neutral-600 truncate" title={c.name}>
                        {c.name}
                      </div>
                    </button>
                  ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Tabs value={activeTab} onValueChange={handleTabChange}>
        <TabsList>
          <TabsTrigger value="teachers">
            教师管理{activeTab === 'teachers' ? ` (${total})` : ''}
          </TabsTrigger>
          <TabsTrigger value="students">
            学生管理{activeTab === 'students' ? ` (${total})` : ''}
          </TabsTrigger>
        </TabsList>

        {importResult && (
            <div
              className={`mt-4 p-4 rounded-lg border ${
                importResult.success
                  ? 'bg-success/5 border-success/30 text-success'
                  : 'bg-warning/5 border-warning/30 text-warning'
              }`}
            >
              <div className="flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 mt-0.5 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm">{importResult.message}</p>
                  {importResult.errors && importResult.errors.length > 0 && (
                    <details className="mt-2">
                      <summary className="text-xs cursor-pointer hover:opacity-80">
                        {importResult.errors.length} 条数据异常，点击查看详情
                      </summary>
                      <div className="mt-2 max-h-48 overflow-y-auto space-y-1">
                        {importResult.errors.map((err, i) => (
                          <div key={i} className="text-xs bg-white/50 rounded px-2 py-1">
                            <span className="font-medium">第{err.row}行</span>
                            {' · '}
                            {err.realName || err.studentNo}
                            {' · '}
                            <span className="text-danger">{err.errors.join('；')}</span>
                          </div>
                        ))}
                      </div>
                    </details>
                  )}
                </div>
                <button
                  className="text-xs opacity-60 hover:opacity-100 flex-shrink-0"
                  onClick={clearImportResult}
                >
                  关闭
                </button>
              </div>
            </div>
          )}

          {actionError && (
          <div className="mt-4 p-3 rounded-lg bg-danger/10 text-danger text-sm">
            {actionError}
          </div>
        )}

        {/* Teachers table */}
        <TabsContent value="teachers" className="mt-6">
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-neutral-200">
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      教师
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      手机号
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      邮箱
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      状态
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      注册时间
                    </th>
                    <th className="text-right px-6 py-4 text-sm font-medium text-neutral-500">
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-10 text-center text-neutral-400">
                        数据加载中...
                      </td>
                    </tr>
                  ) : error ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-10 text-center text-danger">
                        {error}
                      </td>
                    </tr>
                  ) : list.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-10 text-center text-neutral-400">
                        暂无数据
                      </td>
                    </tr>
                  ) : (
                    list.map((teacher) => (
                      <tr
                        key={teacher.id}
                        className="border-b border-neutral-100 hover:bg-neutral-50 transition-colors"
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white font-medium">
                              {(teacher.realName || teacher.username || '?').charAt(0)}
                            </div>
                            <div>
                              <div className="font-medium text-neutral-900">
                                {teacher.realName || '-'}
                              </div>
                              <div className="text-xs text-neutral-500">
                                @{teacher.username}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-neutral-700">
                          {teacher.phone || '-'}
                        </td>
                        <td className="px-6 py-4 text-sm text-neutral-700">
                          {teacher.email || '-'}
                        </td>
                        <td className="px-6 py-4">{statusBadge(teacher.status)}</td>
                        <td className="px-6 py-4 text-sm text-neutral-700">
                          {teacher.createdAt
                            ? new Date(teacher.createdAt).toLocaleDateString()
                            : '-'}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openEditSheet(teacher)}
                            >
                              编辑
                            </Button>
                            <DropdownMenu
                              open={openMenuId === teacher.id}
                              onOpenChange={(open) => setOpenMenuId(open ? teacher.id : null)}
                            >
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="w-8 h-8">
                                  <MoreHorizontal className="w-4 h-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-36">
                                <DropdownMenuItem
                                  onSelect={(e) => {
                                    e.preventDefault()
                                    setOpenMenuId(null)
                                    openEditSheet(teacher)
                                  }}
                                >
                                  查看详情
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  onSelect={(e) => {
                                    e.preventDefault()
                                    setOpenMenuId(null)
                                    openResetDialog(teacher)
                                  }}
                                >
                                  重置密码
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  className="text-danger"
                                  onSelect={(e) => {
                                    e.preventDefault()
                                    setOpenMenuId(null)
                                    openDeleteDialog(teacher)
                                  }}
                                >
                                  删除账号
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {!loading && !error && list.length > 0 && (
              <div className="flex items-center justify-between px-6 py-4 border-t border-neutral-100">
                <div className="text-sm text-neutral-500">
                  第 {pageNum} / {totalPages} 页 · 共 {total} 条
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={goPrev}
                    disabled={pageNum <= 1}
                    className="gap-1"
                  >
                    <ChevronLeft className="w-4 h-4" />
                    上一页
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={goNext}
                    disabled={pageNum >= totalPages}
                    className="gap-1"
                  >
                    下一页
                    <ChevronRight className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </TabsContent>

        {/* Students table */}
        <TabsContent value="students" className="mt-6">
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-neutral-200">
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      学生
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      学号
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      班级
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      专业
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      年级
                    </th>
                    <th className="text-left px-6 py-4 text-sm font-medium text-neutral-500">
                      状态
                    </th>
                    <th className="text-right px-6 py-4 text-sm font-medium text-neutral-500">
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={7} className="px-6 py-10 text-center text-neutral-400">
                        数据加载中...
                      </td>
                    </tr>
                  ) : error ? (
                    <tr>
                      <td colSpan={7} className="px-6 py-10 text-center text-danger">
                        {error}
                      </td>
                    </tr>
                  ) : list.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-6 py-10 text-center text-neutral-400">
                        暂无数据
                      </td>
                    </tr>
                  ) : (
                    list.map((student) => (
                      <tr
                        key={student.id}
                        className="border-b border-neutral-100 hover:bg-neutral-50 transition-colors"
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-cyan-400 to-cyan-600 flex items-center justify-center text-white font-medium">
                              {(student.realName || student.username || '?').charAt(0)}
                            </div>
                            <div>
                              <div className="font-medium text-neutral-900">
                                {student.realName || '-'}
                              </div>
                              <div className="text-xs text-neutral-500">
                                {student.phone || `@${student.username}`}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-neutral-700 font-mono">
                          {student.studentNo || '-'}
                        </td>
                        <td className="px-6 py-4 text-sm text-neutral-700">
                          {classNameById(student.classId)}
                        </td>
                        <td className="px-6 py-4 text-sm text-neutral-700">
                          {student.major || '-'}
                        </td>
                        <td className="px-6 py-4 text-sm text-neutral-700">
                          {student.grade || '-'}
                        </td>
                        <td className="px-6 py-4">{statusBadge(student.status)}</td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openEditSheet(student)}
                            >
                              编辑
                            </Button>
                            <DropdownMenu
                              open={openMenuId === student.id}
                              onOpenChange={(open) => setOpenMenuId(open ? student.id : null)}
                            >
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="w-8 h-8">
                                  <MoreHorizontal className="w-4 h-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-36">
                                <DropdownMenuItem
                                  onSelect={(e) => {
                                    e.preventDefault()
                                    setOpenMenuId(null)
                                    openEditSheet(student)
                                  }}
                                >
                                  查看详情
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  onSelect={(e) => {
                                    e.preventDefault()
                                    setOpenMenuId(null)
                                    openResetDialog(student)
                                  }}
                                >
                                  重置密码
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  className="text-danger"
                                  onSelect={(e) => {
                                    e.preventDefault()
                                    setOpenMenuId(null)
                                    openDeleteDialog(student)
                                  }}
                                >
                                  删除账号
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {!loading && !error && list.length > 0 && (
              <div className="flex items-center justify-between px-6 py-4 border-t border-neutral-100">
                <div className="text-sm text-neutral-500">
                  第 {pageNum} / {totalPages} 页 · 共 {total} 条
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={goPrev}
                    disabled={pageNum <= 1}
                    className="gap-1"
                  >
                    <ChevronLeft className="w-4 h-4" />
                    上一页
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={goNext}
                    disabled={pageNum >= totalPages}
                    className="gap-1"
                  >
                    下一页
                    <ChevronRight className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </TabsContent>
      </Tabs>

      {/* Create / Edit Sheet */}
      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="right">
          <SheetHeader>
            <SheetTitle>
              {editingUser ? '编辑' : '新增'}
              {tabLabel}
            </SheetTitle>
          </SheetHeader>
          <div className="py-6 space-y-5">
            <div className="space-y-2">
              <Label>姓名</Label>
              <Input
                placeholder="请输入姓名"
                value={form.realName}
                onChange={(e) => updateField('realName', e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>账号</Label>
              <Input
                placeholder="请输入登录账号"
                value={form.username}
                onChange={(e) => updateField('username', e.target.value)}
                disabled={!!editingUser}
              />
            </div>
            <div className="space-y-2">
              <Label>手机号</Label>
              <Input
                placeholder="请输入手机号"
                value={form.phone}
                onChange={(e) => updateField('phone', e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>邮箱</Label>
              <Input
                placeholder="请输入邮箱"
                value={form.email}
                onChange={(e) => updateField('email', e.target.value)}
              />
            </div>
            {currentRole === ROLE_STUDENT && (
              <>
                <div className="space-y-2">
                  <Label>学号</Label>
                  <Input
                    placeholder="请输入学号"
                    value={form.studentNo}
                    onChange={(e) => updateField('studentNo', e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label>所属班级</Label>
                  <select
                    className="w-full h-10 px-3 rounded-md border border-neutral-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                    value={form.classId}
                    onChange={(e) => updateField('classId', e.target.value)}
                  >
                    <option value="">请选择班级</option>
                    {classes.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.className}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>专业</Label>
                    <Input
                      placeholder="请输入专业"
                      value={form.major}
                      onChange={(e) => updateField('major', e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>年级</Label>
                    <Input
                      placeholder="如 2024级"
                      value={form.grade}
                      onChange={(e) => updateField('grade', e.target.value)}
                    />
                  </div>
                </div>
              </>
            )}
            <div className="space-y-2">
              <Label>{editingUser ? '重置密码（留空则不修改）' : '初始密码'}</Label>
              <Input
                placeholder="默认 123456"
                value={form.password}
                onChange={(e) => updateField('password', e.target.value)}
              />
            </div>
            {formError && (
              <div className="p-3 rounded-lg bg-danger/10 text-danger text-sm">
                {formError}
              </div>
            )}
          </div>
          <SheetFooter>
            <Button
              variant="outline"
              onClick={() => setSheetOpen(false)}
              disabled={submitting}
            >
              取消
            </Button>
            <Button variant="default" onClick={handleSubmit} disabled={submitting}>
              {submitting ? '提交中...' : '确定'}
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      {/* Delete confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除账号</DialogTitle>
            <DialogDescription>
              确定要删除「{deletingUser?.realName || deletingUser?.username}」吗？该操作为软删除，可在数据库中恢复。
            </DialogDescription>
          </DialogHeader>
          {actionError && (
            <div className="p-3 rounded-lg bg-danger/10 text-danger text-sm">
              {actionError}
            </div>
          )}
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteOpen(false)}
              disabled={deleting}
            >
              取消
            </Button>
            <Button
              variant="default"
              onClick={confirmDelete}
              disabled={deleting}
            >
              {deleting ? '删除中...' : '确认删除'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reset password */}
      <Dialog open={resetOpen} onOpenChange={setResetOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>重置密码</DialogTitle>
            <DialogDescription>
              将「{resettingUser?.realName || resettingUser?.username}」的密码重置为以下新密码：
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label>新密码</Label>
            <Input
              placeholder="请输入新密码"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>
          {actionError && (
            <div className="p-3 rounded-lg bg-danger/10 text-danger text-sm">
              {actionError}
            </div>
          )}
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setResetOpen(false)}
              disabled={resetting}
            >
              取消
            </Button>
            <Button
              variant="default"
              onClick={confirmResetPassword}
              disabled={resetting}
            >
              {resetting ? '重置中...' : '确认重置'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
