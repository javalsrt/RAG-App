import { NavLink, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { useAuthStore } from '@/store/auth'
import { adminMenus, teacherMenus, type MenuItem } from '@/config/menu'
import { Sparkles, ChevronDown } from 'lucide-react'
import { useState } from 'react'

export function Sidebar() {
  const { user } = useAuthStore()
  const location = useLocation()
  const menus = user?.role === 'admin' ? adminMenus : teacherMenus
  const [expandedKeys, setExpandedKeys] = useState<string[]>(['/admin/staff'])

  const toggleExpand = (path: string) => {
    setExpandedKeys((prev) =>
      prev.includes(path) ? prev.filter((k) => k !== path) : [...prev, path]
    )
  }

  const isChildActive = (item: MenuItem) => {
    if (!item.children) return false
    return item.children.some((child) => location.pathname === child.path)
  }

  return (
    <aside className="w-64 h-screen bg-white border-r border-neutral-200 flex flex-col fixed left-0 top-0 z-40">
      {/* Logo */}
      <div className="h-20 flex items-center px-6 border-b border-neutral-100">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 via-primary-400 to-cyan-400 flex items-center justify-center">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="font-bold text-lg text-neutral-900 leading-tight">
              智学平台
            </div>
            <div className="text-xs text-neutral-400">AI Learning</div>
          </div>
        </div>
      </div>

      {/* Menu */}
      <nav className="flex-1 overflow-y-auto py-4 px-3 scrollbar-thin">
        <div className="space-y-1">
          {menus.map((item) => (
            <div key={item.path}>
              {item.children ? (
                <>
                  <button
                    onClick={() => toggleExpand(item.path)}
                    className={cn(
                      'w-full flex items-center gap-3 px-4 py-2.5 text-sm font-medium rounded-xl transition-all duration-200',
                      isChildActive(item)
                        ? 'text-primary-600 bg-primary-50'
                        : 'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900'
                    )}
                  >
                    <item.icon className="w-5 h-5 flex-shrink-0" />
                    <span className="flex-1 text-left">{item.title}</span>
                    <ChevronDown
                      className={cn(
                        'w-4 h-4 transition-transform duration-200',
                        expandedKeys.includes(item.path) && 'rotate-180'
                      )}
                    />
                  </button>
                  {expandedKeys.includes(item.path) && (
                    <div className="mt-1 ml-4 space-y-0.5">
                      {item.children.map((child) => (
                        <NavLink
                          key={child.path}
                          to={child.path}
                          className={({ isActive }) =>
                            cn(
                              'flex items-center gap-3 px-4 py-2 text-sm rounded-lg transition-all duration-200',
                              isActive
                                ? 'text-primary-600 bg-primary-50 font-medium'
                                : 'text-neutral-500 hover:text-neutral-900 hover:bg-neutral-50'
                            )
                          }
                        >
                          <child.icon className="w-4 h-4 flex-shrink-0" />
                          <span>{child.title}</span>
                        </NavLink>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <NavLink
                  to={item.path}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 px-4 py-2.5 text-sm font-medium rounded-xl transition-all duration-200',
                      isActive
                        ? 'text-primary-600 bg-primary-50'
                        : 'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900'
                    )
                  }
                >
                  <item.icon className="w-5 h-5 flex-shrink-0" />
                  <span>{item.title}</span>
                </NavLink>
              )}
            </div>
          ))}
        </div>
      </nav>

      {/* User info */}
      <div className="p-4 border-t border-neutral-100">
        <div className="flex items-center gap-3 px-2">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white font-medium">
            {user?.realName?.charAt(0) || user?.username?.charAt(0) || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-sm font-medium text-neutral-900 truncate">
              {user?.realName || user?.username}
            </div>
            <div className="text-xs text-neutral-400 truncate">
              {user?.role === 'admin' ? '管理员' : user?.role === 'teacher' ? '教师' : '学生'}
            </div>
          </div>
        </div>
      </div>
    </aside>
  )
}
