import { useEffect, useState } from 'react'
import {
  Users,
  Clock,
  TrendingUp,
  Award,
  Activity,
  BookOpen,
} from 'lucide-react'
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  BarChart,
  Bar,
  Cell,
  LineChart,
  Line,
} from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { SpotlightCard } from '@/components/SpotlightCard'
import { Badge } from '@/components/ui/badge'
import { useRole } from '@/hooks/use-role'
import { getTeacherStats, getTeacherTrend } from '@/api/dashboard'
import type { TeacherStats, TrendItem } from '@/types'

export function DashboardPage() {
  const { isAdmin, user } = useRole()
  const [stats, setStats] = useState<TeacherStats | null>(null)
  const [trend, setTrend] = useState<TrendItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      setError('')
      try {
        const [statsData, trendData] = await Promise.all([
          getTeacherStats(),
          getTeacherTrend(),
        ])
        setStats(statsData)
        setTrend(trendData || [])
      } catch (err: any) {
        setError(err.response?.data?.message || err.response?.data?.error || '数据加载失败')
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-neutral-400">数据加载中...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-danger">{error}</div>
      </div>
    )
  }

  const cards = isAdmin
    ? [
        {
          label: '学生总数',
          value: stats?.totalStudents ?? 0,
          icon: Users,
          color: 'bg-primary-50 text-primary-550',
        },
        {
          label: '今日在线',
          value: stats?.onlineToday ?? 0,
          icon: Activity,
          color: 'bg-green-50 text-green-600',
        },
        {
          label: '平均专注时长',
          value: `${stats?.avgFocusMinutes ?? 0} 分钟`,
          icon: Clock,
          color: 'bg-orange-50 text-orange-600',
        },
        {
          label: '答题正确率',
          value: `${stats?.quizAccuracy ?? 0}%`,
          icon: Award,
          color: 'bg-cyan-50 text-cyan-600',
        },
      ]
    : [
        {
          label: '授课学生',
          value: stats?.totalStudents ?? 0,
          icon: Users,
          color: 'bg-primary-50 text-primary-550',
        },
        {
          label: '今日在线',
          value: stats?.onlineToday ?? 0,
          icon: Activity,
          color: 'bg-green-50 text-green-600',
        },
        {
          label: '平均专注时长',
          value: `${stats?.avgFocusMinutes ?? 0} 分钟`,
          icon: Clock,
          color: 'bg-orange-50 text-orange-600',
        },
        {
          label: '答题正确率',
          value: `${stats?.quizAccuracy ?? 0}%`,
          icon: Award,
          color: 'bg-cyan-50 text-cyan-600',
        },
      ]

  const classRankingData = stats?.classFocusRanking || []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-neutral-900">
          {isAdmin ? '数据总览' : '教学总览'}
        </h1>
        <p className="text-neutral-500 mt-1">
          {isAdmin
            ? '查看全局学习数据和统计'
            : `${user?.realName || ''}老师，欢迎回来，这是您的教学数据`}
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((card, i) => {
          const Icon = card.icon
          return (
            <SpotlightCard key={i} className="bg-white">
              <div className="p-5">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-neutral-500 text-sm">{card.label}</div>
                    <div className="text-2xl font-bold text-neutral-900 mt-1">
                      {card.value}
                    </div>
                  </div>
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${card.color}`}>
                    <Icon className="w-6 h-6" />
                  </div>
                </div>
              </div>
            </SpotlightCard>
          )
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-primary-550" />
              近 7 天学习时长趋势
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={trend}>
                <defs>
                  <linearGradient id="colorMinutes" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#5b58ff" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#5b58ff" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e6e9ef" />
                <XAxis dataKey="date" tick={{ fontSize: 12, fill: '#7f8798' }} />
                <YAxis tick={{ fontSize: 12, fill: '#7f8798' }} />
                <Tooltip
                  contentStyle={{
                    borderRadius: '12px',
                    border: '1px solid #e6e9ef',
                    fontSize: '12px',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="minutes"
                  stroke="#5b58ff"
                  strokeWidth={2}
                  fill="url(#colorMinutes)"
                  name="学习时长(分钟)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Award className="w-4 h-4 text-primary-550" />
              各班级平均学习时长
            </CardTitle>
          </CardHeader>
          <CardContent>
            {classRankingData.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={classRankingData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#e6e9ef" />
                  <XAxis type="number" tick={{ fontSize: 12, fill: '#7f8798' }} unit="分" />
                  <YAxis
                    type="category"
                    dataKey="name"
                    tick={{ fontSize: 12, fill: '#7f8798' }}
                    width={120}
                  />
                  <Tooltip
                    formatter={(value: number, _name: string, props: any) => [
                      `${value} 分钟（${props.payload.studentCount}人）`,
                      '人均时长',
                    ]}
                    contentStyle={{
                      borderRadius: '12px',
                      border: '1px solid #e6e9ef',
                      fontSize: '12px',
                    }}
                  />
                  <Bar dataKey="avgMinutes" name="人均时长" radius={[0, 6, 6, 0]}>
                    {classRankingData.map((_item: any, i: number) => (
                      <Cell key={i} fill={['#5b58ff', '#22c55e', '#f59e0b', '#06b6d4', '#ec4899'][i % 5]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[280px] flex items-center justify-center text-neutral-400">
                暂无班级数据
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="w-4 h-4 text-primary-550" />
            学生学习状态
          </CardTitle>
        </CardHeader>
        <CardContent>
          {stats?.students && stats.students.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-neutral-200">
                    <th className="text-left py-3 px-4 text-sm font-medium text-neutral-500">学生</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-neutral-500">学号</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-neutral-500">班级</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-neutral-500">今日时长</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-neutral-500">累计时长</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-neutral-500">状态</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.students.map((s) => (
                    <tr key={s.id} className="border-b border-neutral-100 hover:bg-neutral-50">
                      <td className="py-3 px-4 text-sm font-medium text-neutral-900">{s.realName}</td>
                      <td className="py-3 px-4 text-sm text-neutral-600">{s.studentNo}</td>
                      <td className="py-3 px-4 text-sm text-neutral-600">{s.className}</td>
                      <td className="py-3 px-4 text-sm text-neutral-600">
                        {Math.floor(s.todaySeconds / 60)} 分钟
                      </td>
                      <td className="py-3 px-4 text-sm text-neutral-600">
                        {Math.floor(s.totalSeconds / 3600)} 小时
                      </td>
                      <td className="py-3 px-4">
                        {s.online ? (
                          <Badge variant="success">在线</Badge>
                        ) : (
                          <Badge variant="secondary">离线</Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="h-[200px] flex items-center justify-center text-neutral-400">
              暂无学生数据
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
