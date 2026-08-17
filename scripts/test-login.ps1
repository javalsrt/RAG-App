# ========================================================
# 阶段3：登录接口返回角色与权限验证脚本
# ========================================================

$BaseUrl = "http://localhost:8080"
$LoginUrl = "$BaseUrl/api/auth/login"

$TestUsers = @(
    @{ username = "admin";    expectedRole = "admin";    minPerms = 20 },
    @{ username = "zhangmy";  expectedRole = "teacher";  minPerms = 5 },
    @{ username = "student1"; expectedRole = "student";  minPerms = 2 }
)

$Password = "123456"
$Failed = 0

foreach ($u in $TestUsers) {
    $name = $u.username
    $expected = $u.expectedRole
    $min = $u.minPerms
    $body = @{ username = $name; password = $Password } | ConvertTo-Json -Compress

    try {
        $res = Invoke-RestMethod -Uri $LoginUrl -Method POST -ContentType "application/json" -Body $body -ErrorAction Stop

        if (-not $res.token) {
            Write-Host "[FAIL] $name login failed: no token"
            $Failed++
            continue
        }

        if (-not $res.roles -or $res.roles.Count -eq 0) {
            Write-Host "[FAIL] $name login ok but roles empty"
            $Failed++
            continue
        }

        if (-not $res.permissions -or $res.permissions.Count -lt $min) {
            Write-Host "[FAIL] $name permissions count too low (expect >= $min, actual $($res.permissions.Count))"
            $Failed++
            continue
        }

        if (-not ($res.roles -contains $expected)) {
            Write-Host "[FAIL] $name expected role $expected not in ($($res.roles -join ', '))"
            $Failed++
            continue
        }

        Write-Host "[PASS] $name login ok, roles: $($res.roles -join ', '), permissions: $($res.permissions.Count)"
    } catch {
        Write-Host "[FAIL] $name request error: $($_.Exception.Message)"
        $Failed++
    }
}

if ($Failed -gt 0) {
    Write-Host "[FAIL] stage 3 verification failed, failures: $Failed"
    exit 1
}

Write-Host "[PASS] stage 3 verification passed"
exit 0
