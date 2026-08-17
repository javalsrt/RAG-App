import { chromium } from 'playwright'

const BASE_URL = process.env.FRONTEND_URL || 'http://localhost:5174'
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080'
const TEST_USERS = [
  { username: 'admin', expectedRole: 'admin', minPerms: 25 },
  { username: 'zhangmy', expectedRole: 'teacher', minPerms: 10 },
  { username: 'student1', expectedRole: 'student', minPerms: 5 },
]

// 各角色应包含的章节/资源相关权限抽样
const EXPECTED_PERMS = {
  admin: ['chapter:manage', 'chapter:view', 'chapter:create', 'chapter:delete', 'resource:create', 'resource:delete'],
  teacher: ['chapter:view', 'chapter:create', 'chapter:edit:self', 'resource:view', 'resource:create', 'resource:edit:self'],
  student: ['chapter:view', 'resource:view'],
}
const PASSWORD = '123456'

async function getBackendPermissions(username) {
  const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: PASSWORD }),
  })
  if (!res.ok) throw new Error(`后端登录失败: ${username}`)
  return res.json()
}

async function testUser(page, user) {
  const backend = await getBackendPermissions(user.username)
  if (!backend.token) throw new Error(`后端未返回 token: ${user.username}`)

  await page.goto(`${BASE_URL}/login`)
  await page.fill('input#username', user.username)
  await page.fill('input#password', PASSWORD)
  await page.click('button[type="submit"]')
  await page.waitForURL('**/dashboard', { timeout: 10000 })

  const lsRolesRaw = await page.evaluate(() => localStorage.getItem('roles'))
  const lsPermsRaw = await page.evaluate(() => localStorage.getItem('permissions'))
  const lsUserRaw = await page.evaluate(() => localStorage.getItem('user'))

  const lsRoles = JSON.parse(lsRolesRaw || '[]')
  const lsPerms = JSON.parse(lsPermsRaw || '[]')
  const lsUser = JSON.parse(lsUserRaw || '{}')

  const errors = []
  if (!lsRoles.includes(user.expectedRole)) {
    errors.push(`localStorage roles 不包含 ${user.expectedRole}: ${JSON.stringify(lsRoles)}`)
  }
  if (lsRoles.length !== (backend.roles || []).length) {
    errors.push(`roles 数量不一致: store=${lsRoles.length}, backend=${(backend.roles || []).length}`)
  }
  if (lsPerms.length !== (backend.permissions || []).length) {
    errors.push(`permissions 数量不一致: store=${lsPerms.length}, backend=${(backend.permissions || []).length}`)
  }
  if (lsPerms.length < user.minPerms) {
    errors.push(`permissions 数量不足: ${lsPerms.length} < ${user.minPerms}`)
  }
  if (lsUser.role !== user.expectedRole) {
    errors.push(`user.role 不匹配: ${lsUser.role} !== ${user.expectedRole}`)
  }

  // 抽样校验：store 中的权限是后端返回权限的子集且数量一致
  const backendPermSet = new Set(backend.permissions || [])
  const missing = lsPerms.filter((p) => !backendPermSet.has(p))
  if (missing.length > 0) {
    errors.push(`store 权限包含后端未返回项: ${missing.join(', ')}`)
  }

  // 校验章节/资源权限是否符合角色预期
  const expected = EXPECTED_PERMS[user.expectedRole] || []
  const notFound = expected.filter((p) => !lsPerms.includes(p))
  if (notFound.length > 0) {
    errors.push(`缺少章节/资源权限: ${notFound.join(', ')}`)
  }

  // 章节管理页面可访问性校验（学生不应看到新增章节按钮，但页面可加载）
  await page.goto(`${BASE_URL}/course-chapters`)
  try {
    await page.waitForSelector('text=章节管理', { timeout: 5000 })
  } catch (e) {
    errors.push('章节管理页面无法加载')
  }

  if (errors.length > 0) {
    console.log(`[FAIL] ${user.username}: ${errors.join('; ')}`)
    return false
  }
  console.log(`[PASS] ${user.username}: role=${lsUser.role}, roles=${lsRoles.length}, permissions=${lsPerms.length}`)
  return true
}

let allPassed = true
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const context = await browser.newContext()

try {
  for (const user of TEST_USERS) {
    const page = await context.newPage()
    const ok = await testUser(page, user)
    if (!ok) allPassed = false
    await page.close()
  }
} catch (e) {
  console.error(`[FAIL] 验证异常: ${e.message}`)
  allPassed = false
} finally {
  await browser.close()
}

if (!allPassed) process.exit(1)
console.log('[PASS] 阶段5前端权限状态验证通过')
