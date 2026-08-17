# ========================================================
# 阶段4：关键接口权限拦截验证脚本
# ========================================================
# 前置条件：
#   1. 后端服务已启动在 localhost:8080
#   2. 数据库已初始化 RBAC 角色/权限/用户角色关联
#   3. admin/teacher/student 三类测试账号可正常登录
#
# 通过标准：
#   - 低权限角色访问高权限接口返回 403
#   - 拥有权限的角色访问对应接口返回 200
# ========================================================

$BaseUrl = "http://localhost:8080"
$LoginUrl = "$BaseUrl/api/auth/login"

$Password = "123456"
$Failed = 0

function Get-Token($username, $password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json -Compress
    $res = Invoke-RestMethod -Uri $LoginUrl -Method POST -ContentType "application/json" -Body $body -ErrorAction Stop
    if (-not $res.token) {
        throw "登录失败：$username 未返回 token"
    }
    return $res.token
}

function Test-Access($token, $method, $url, $expectedStatus, $desc) {
    $headers = @{ Authorization = "Bearer $token" }
    $actual = $null
    try {
        # 2xx 响应不会抛出异常，直接判定为 200
        $null = Invoke-RestMethod -Uri $url -Method $method -Headers $headers -ContentType "application/json" -ErrorAction Stop
        $actual = 200
    } catch {
        if ($_.Exception.Response) {
            $actual = [int]$_.Exception.Response.StatusCode
        } else {
            $actual = 0
        }
    }

    if ($actual -eq $expectedStatus) {
        Write-Host "[PASS] $desc -> HTTP $actual"
        return $true
    } else {
        Write-Host "[FAIL] $desc -> expected HTTP $expectedStatus, actual HTTP $actual"
        return $false
    }
}

function Get-TeacherCourseId($token) {
    $headers = @{ Authorization = "Bearer $token" }
    $courses = Invoke-RestMethod -Uri "$BaseUrl/api/schedule/teacher/courses" -Method GET -Headers $headers -ContentType "application/json" -ErrorAction Stop
    if ($courses -and $courses.Count -gt 0) {
        return $courses[0].courseId
    }
    return $null
}

# 获取三种角色 token
$adminToken = Get-Token "admin" $Password
$teacherToken = Get-Token "zhangmy" $Password
$studentToken = Get-Token "student1" $Password

# 获取教师一门课程ID，用于章节接口测试
$teacherCourseId = Get-TeacherCourseId $teacherToken

Write-Host "`n--- 越权访问应返回 403 ---"

# 学生访问管理员接口
if (-not (Test-Access $studentToken "GET" "$BaseUrl/api/admin/user/list?role=3" 403 "student 访问 /api/admin/user/list")) { $Failed++ }

# 学生访问教师接口
if (-not (Test-Access $studentToken "GET" "$BaseUrl/api/teacher/stats" 403 "student 访问 /api/teacher/stats")) { $Failed++ }
if (-not (Test-Access $studentToken "GET" "$BaseUrl/api/teacher/class/list" 403 "student 访问 /api/teacher/class/list")) { $Failed++ }

# 学生访问教师专属接口（创建章节）
if (-not (Test-Access $studentToken "POST" "$BaseUrl/api/course-chapter/chapters" 403 "student 创建章节")) { $Failed++ }

# 教师访问学生专属接口（该接口允许 STUDENT 或 ADMIN，教师不应进入）
if (-not (Test-Access $teacherToken "GET" "$BaseUrl/api/schedule/student/courses" 403 "teacher 访问 /api/schedule/student/courses")) { $Failed++ }

# 教师访问管理员接口
if (-not (Test-Access $teacherToken "GET" "$BaseUrl/api/admin/user/list?role=3" 403 "teacher 访问 /api/admin/user/list")) { $Failed++ }

Write-Host "`n--- 有权限访问应返回 200 ---"

# 管理员可访问所有接口
if (-not (Test-Access $adminToken "GET" "$BaseUrl/api/admin/user/list?role=3" 200 "admin 访问 /api/admin/user/list")) { $Failed++ }
if (-not (Test-Access $adminToken "GET" "$BaseUrl/api/teacher/stats" 200 "admin 访问 /api/teacher/stats")) { $Failed++ }
if (-not (Test-Access $adminToken "GET" "$BaseUrl/api/schedule/student/courses" 200 "admin 访问 /api/schedule/student/courses")) { $Failed++ }

# 教师访问教师接口
if (-not (Test-Access $teacherToken "GET" "$BaseUrl/api/teacher/stats" 200 "teacher 访问 /api/teacher/stats")) { $Failed++ }
if (-not (Test-Access $teacherToken "GET" "$BaseUrl/api/teacher/class/list" 200 "teacher 访问 /api/teacher/class/list")) { $Failed++ }
if (-not (Test-Access $teacherToken "GET" "$BaseUrl/api/schedule/teacher/courses" 200 "teacher 访问 /api/schedule/teacher/courses")) { $Failed++ }

# 教师访问章节查看接口
if ($teacherCourseId) {
    if (-not (Test-Access $teacherToken "GET" "$BaseUrl/api/course-chapter/course/$teacherCourseId/chapters" 200 "teacher 访问章节列表")) { $Failed++ }
}

# 学生访问学生接口
if (-not (Test-Access $studentToken "GET" "$BaseUrl/api/schedule/student/courses" 200 "student 访问 /api/schedule/student/courses")) { $Failed++ }
if (-not (Test-Access $studentToken "GET" "$BaseUrl/api/schedule/student/my?week=1" 200 "student 访问 /api/schedule/student/my")) { $Failed++ }

# 学生访问章节查看接口（有 chapter:view 权限）
if ($teacherCourseId) {
    if (-not (Test-Access $studentToken "GET" "$BaseUrl/api/course-chapter/course/$teacherCourseId/chapters" 200 "student 访问章节列表")) { $Failed++ }
}

if ($Failed -gt 0) {
    Write-Host "`n[FAIL] 阶段4权限拦截验证失败，失败数: $Failed"
    exit 1
}

Write-Host "`n[PASS] 阶段4权限拦截验证通过"
exit 0
