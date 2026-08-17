USE znxsgltest;
-- ========================================================
-- 阶段2：RBAC 初始化数据验证脚本
-- 目标：确认默认角色、权限、角色权限关联、用户角色同步正确
-- 前置条件：已执行 init-db/09-rbac-permission-data.sql
-- 通过标准：以下所有检查项均为 [PASS]
-- ========================================================

-- 1. 检查默认角色数量
SELECT IF(COUNT(*) = 3,
  '[PASS] 默认角色数量为 3',
  CONCAT('[FAIL] 默认角色数量异常，实际 ', COUNT(*))
) AS result
FROM sys_role WHERE role_code IN ('admin', 'teacher', 'student');

-- 2. 检查权限数量（至少 29 个）
SELECT IF(COUNT(*) >= 29,
  '[PASS] 权限数量 >= 29',
  CONCAT('[FAIL] 权限数量不足，实际 ', COUNT(*))
) AS result
FROM sys_permission;

-- 3. 检查管理员拥有全部权限
SELECT IF(COUNT(*) = (SELECT COUNT(*) FROM sys_permission WHERE status = 1),
  '[PASS] admin 拥有全部启用权限',
  CONCAT('[FAIL] admin 权限数量异常，实际 ', COUNT(*))
) AS result
FROM sys_role_permission WHERE role_id = 1;

-- 4. 检查教师拥有预期权限
SELECT IF(COUNT(*) = 16,
  '[PASS] teacher 拥有 16 个权限',
  CONCAT('[FAIL] teacher 权限数量异常，实际 ', COUNT(*))
) AS result
FROM sys_role_permission WHERE role_id = 2;

-- 5. 检查学生拥有预期权限
SELECT IF(COUNT(*) = 8,
  '[PASS] student 拥有 8 个权限',
  CONCAT('[FAIL] student 权限数量异常，实际 ', COUNT(*))
) AS result
FROM sys_role_permission WHERE role_id = 3;

-- 6. 检查数据权限规则数量
SELECT IF(COUNT(*) = 15,
  '[PASS] 数据权限规则数量为 15',
  CONCAT('[FAIL] 数据权限规则数量异常，实际 ', COUNT(*))
) AS result
FROM sys_data_permission;

-- 7. 检查用户角色同步
SELECT IF(NOT EXISTS (
  SELECT 1 FROM user u
  WHERE u.role IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM sys_user_role sur WHERE sur.user_id = u.id
    )
), '[PASS] 所有 user.role 非空用户已同步到 sys_user_role',
   '[FAIL] 存在 user.role 非空但未同步到 sys_user_role 的用户'
) AS result;

-- 8. 列出各角色权限明细（便于人工复核）
SELECT r.role_code, r.role_name, COUNT(rp.permission_id) AS perm_count
FROM sys_role r
LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
GROUP BY r.id, r.role_code, r.role_name
ORDER BY r.id;

-- 9. 列出教师权限编码
SELECT p.perm_code, p.perm_name
FROM sys_permission p
JOIN sys_role_permission rp ON p.id = rp.permission_id
WHERE rp.role_id = 2
ORDER BY p.id;

-- 10. 列出学生权限编码
SELECT p.perm_code, p.perm_name
FROM sys_permission p
JOIN sys_role_permission rp ON p.id = rp.permission_id
WHERE rp.role_id = 3
ORDER BY p.id;
