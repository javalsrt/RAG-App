# ========================================================
# 阶段5：前端权限状态验证脚本
# ========================================================
# 前置条件：
#   1. 后端服务已启动在 localhost:8080
#   2. 前端开发服务器已启动（默认 http://localhost:5174）
#
# 通过标准：
#   - admin/teacher/student 登录后，localStorage/store 中的 roles、permissions
#     与后端登录接口返回一致
# ========================================================

$FrontendDir = Join-Path $PSScriptRoot "..\web-admin-react"
$FrontendPort = 5174
$FrontendUrl = "http://localhost:$FrontendPort"

# 若前端服务未启动，则自动启动并等待就绪
$connection = Get-NetTCPConnection -LocalPort $FrontendPort -ErrorAction SilentlyContinue
if (-not $connection) {
    Write-Host "[INFO] 前端服务未在端口 $FrontendPort 运行，尝试自动启动..."
    $proc = Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory $FrontendDir -PassThru -WindowStyle Hidden
    $startedByUs = $true
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        $connection = Get-NetTCPConnection -LocalPort $FrontendPort -ErrorAction SilentlyContinue
        if ($connection) {
            $ready = $true
            break
        }
    }
    if (-not $ready) {
        Write-Host "[FAIL] 前端服务启动超时，请手动启动后重试"
        exit 1
    }
    # 多给 Vite 一点时间完成预构建
    Start-Sleep -Seconds 3
} else {
    $startedByUs = $false
}

try {
    $env:FRONTEND_URL = $FrontendUrl
    $env:BACKEND_URL = "http://localhost:8080"
    $scriptPath = Join-Path $FrontendDir 'scripts/test-frontend-perm.mjs'

    node $scriptPath
    $exitCode = $LASTEXITCODE
} finally {
    if ($startedByUs -and $proc) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
}

exit $exitCode
