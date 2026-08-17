# 自动循环验证工作流（Auto Loop Verification Workflow）

## 1. 目标

在 `aiStudy` 项目中，任何涉及权限、数据一致性、多端交互的改动都必须经过“开发 → 自动验证 → 修复 → 再验证”的闭环，防止改完即丢、问题遗留到下一环节。

## 2. 核心思想

- **阶段化**：把大需求拆成 5 个可独立验证的阶段（见第 3 节）。
- **自动化**：每个阶段都有可一键执行的验证脚本或命令，减少人工点击。
- **可回滚**：任一阶段验证失败，必须回到该阶段起点修复，不允许跳过。
- **可追溯**：每轮验证结果写入 `docs/verify-logs/` 或终端输出，便于复盘。

## 3. 阶段划分（以 RBAC 权限体系为例）

| 阶段 | 输入 | 输出 | 验证脚本 | 通过标准 |
|---|---|---|---|---|
| 阶段 1 | 需求/设计文档 | 数据库表 SQL | `scripts/verify-rbac-db.sql` | 5 张 RBAC 表结构正确 |
| 阶段 2 | SQL 已执行 | 初始化数据 | `scripts/verify-rbac-data.sql` | 角色/权限/关联/用户角色同步正确 |
| 阶段 3 | 表和数据就绪 | 登录接口改造 | `scripts/test-login.ps1` | 三种角色登录返回 roles + permissions |
| 阶段 4 | 登录接口返回权限 | 关键接口加注解 | `scripts/test-perm.ps1` | 无权限访问返回 403 |
| 阶段 5 | 后端权限生效 | 前端权限状态改造 | `scripts/test-frontend-perm.ps1` | store 中 permissions 与后端一致 |

## 4. 循环执行规则

```text
需求 → 阶段1开发 → 阶段1验证 ─┐
                              ↓ 失败
                         阶段1修复 ─┘
                              ↓ 通过
                         阶段2开发 → 阶段2验证 ─┐
                                              ↓ 失败
                                         阶段2修复 ─┘
                                              ↓ 通过
                                         ... → 阶段5验证通过 → 合并
```

- **禁止跳阶段**：阶段 N 未通过前，不得开始阶段 N+1。
- **失败即停**：验证脚本返回非 0 时，立即停止并输出错误摘要。
- **修复后重跑**：修复后必须重新跑当前阶段及所有前置阶段验证，确保没有回退。

## 5. 验证脚本规范

每个验证脚本必须满足：

1. **幂等**：多次执行结果一致，不污染数据。
2. **自描述**：脚本头部写明验证目标、前置条件、通过标准。
3. **退出码**：0 表示通过，非 0 表示失败。
4. **输出格式**：
   - `[PASS] xxxx`
   - `[FAIL] xxxx`
   - `[SKIP] xxxx`（可选环境不满足时）

## 6. 当前应用：RBAC 权限体系

### 6.1 验证脚本清单

| 脚本 | 路径 | 作用 |
|---|---|---|
| 阶段 1 表结构验证 | `scripts/verify-rbac-schema.sql` | 检查 sys_role 等 5 张表是否存在、字段是否正确 |
| 阶段 2 数据验证 | `scripts/verify-rbac-data.sql` | 检查默认角色、权限、角色权限关联、用户角色同步 |
| 阶段 3 登录接口测试 | `scripts/test-login.ps1` | 用 PowerShell 测试 admin/teacher/student 登录，校验返回字段 |
| 阶段 4 权限拦截测试 | `scripts/test-perm.ps1` | 用无权限 token 访问受保护接口，校验 403 |
| 阶段 5 前端状态测试 | `scripts/test-frontend-perm.ps1` | 自动启动前端服务（如未启动），用 Playwright 校验 store 与后端一致 |
| 阶段 5 核心校验 | `web-admin-react/scripts/test-frontend-perm.mjs` | 被 `test-frontend-perm.ps1` 调用，执行浏览器登录与 store 校验 |

### 6.2 阶段关联表

```text
09-rbac-permission.sql      → 阶段 1
09-rbac-permission-data.sql → 阶段 2
AuthService.java            → 阶段 3
JwtAuthFilter.java          → 阶段 3/4
Controller 权限注解         → 阶段 4
web-admin auth store        → 阶段 5
```

### 6.3 阶段 4/5 执行记录

| 阶段 | 执行脚本 | 结果 | 关键验证点 |
|---|---|---|---|
| 阶段 4 | `scripts/test-perm.ps1` | 通过 | student/teacher 越权访问 admin/teacher/student 专属接口返回 403；admin 可访问所有接口；teacher 可访问教师接口；student 可访问学生接口 |
| 阶段 5 | `scripts/test-frontend-perm.ps1` | 通过 | admin/teacher/student 登录后，localStorage 与 Zustand store 中的 roles、permissions 数量与内容均与后端 `/api/auth/login` 返回一致 |

## 7. 手动触发方式

```powershell
# 后端开发环境启动后（在 PowerShell 中执行）
.\scripts\test-login.ps1
.\scripts\test-perm.ps1

# 前端权限验证（会自动检测/启动前端服务）
.\scripts\test-frontend-perm.ps1

# 数据库验证
mysql -u root -p znxsgltest < scripts/verify-rbac-schema.sql
mysql -u root -p znxsgltest < scripts/verify-rbac-data.sql
```

## 8. 应用案例：课程章节与资源体系

### 8.1 新增/变更清单

| 层级 | 文件 | 说明 |
|---|---|---|
| 数据库 | `init-db/10-course-chapter.sql` | 创建 `course_chapter`、`course_lesson` 表 |
| 数据库 | `init-db/09-rbac-permission-data.sql` | 初始化章节/资源权限与角色关联 |
| 后端 | `backend/.../entity/CourseChapter.java` | 章节实体类 |
| 后端 | `backend/.../entity/CourseLesson.java` | 课时/资源实体类 |
| 后端 | `backend/.../service/CourseChapterService.java` | 章节/课时业务与数据权限控制 |
| 后端 | `backend/.../controller/CourseChapterController.java` | RESTful API 与权限注解 |
| 前端 | `web-admin-react/src/types/index.ts` | Chapter/Lesson 类型定义 |
| 前端 | `web-admin-react/src/api/course-chapter.ts` | 章节/课时 API 封装 |
| 前端 | `web-admin-react/src/pages/course-chapters.tsx` | 章节管理页面（兼容学生只读查看）|
| 前端 | `web-admin-react/src/api/courses.ts` | 新增 `getStudentCourses` 供学生加载已选课程 |
| 前端 | `web-admin-react/src/router/index.tsx` | `/course-chapters` 路由 |
| 前端 | `web-admin-react/src/config/menu.ts` | 教师/管理员菜单增加「章节管理」|
| 验证 | `scripts/verify-course-chapter-schema.sql` | 阶段1：表结构验证 |
| 验证 | `scripts/verify-course-chapter-data.sql` | 阶段2：数据权限规则验证 |
| 验证 | `scripts/test-perm.ps1` | 阶段4：章节/资源接口权限拦截测试 |
| 验证 | `web-admin-react/scripts/test-frontend-perm.mjs` | 阶段5：store 权限与章节页面可访问性 |

### 8.2 阶段执行记录

| 阶段 | 执行脚本 | 结果 | 关键验证点 |
|---|---|---|---|
| 阶段 1 | `scripts/verify-course-chapter-schema.sql` | 通过 | `course_chapter`、`course_lesson` 表及字段、索引、注释正确 |
| 阶段 2 | `scripts/verify-course-chapter-data.sql` | 通过 | 章节/资源权限已初始化，教师/学生数据权限规则正确 |
| 阶段 3 | `scripts/test-login.ps1` | 通过 | admin/teacher/student 登录均返回 roles + permissions |
| 阶段 4 | `scripts/test-perm.ps1` | 通过 | student 创建章节返回 403；teacher/admin 访问章节列表返回 200 |
| 阶段 5 | `scripts/test-frontend-perm.ps1` | 待执行 | store 中 roles/permissions 与后端一致；admin/teacher/student 均可加载 `/course-chapters` 页面 |

### 8.3 手动触发方式

```powershell
# 数据库验证
mysql -u root -p znxsgltest < scripts/verify-course-chapter-schema.sql
mysql -u root -p znxsgltest < scripts/verify-course-chapter-data.sql

# 后端权限验证
.\scripts\test-login.ps1
.\scripts\test-perm.ps1

# 前端权限与页面验证
.\scripts\test-frontend-perm.ps1
```

## 9. 与 agent-workflow.md 的关系
- `agent-workflow.md` 定义子代理分工与跨端返工流程。
- `auto-verify-workflow.md` 定义每个阶段内部的自动验证标准。

两者结合：子代理完成开发后，必须调用对应验证脚本；验证失败则进入返工闭环。
