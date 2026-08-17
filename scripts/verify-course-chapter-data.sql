USE znxsgltest;
-- ========================================================
-- 阶段2：课程章节与资源体系 RBAC 数据验证脚本
-- 目标：确认章节/资源权限、数据权限规则、角色权限关联正确
-- 前置条件：已执行 init-db/09-rbac-permission-data.sql
-- 通过标准：以下所有检查项均为 [PASS]
-- ========================================================

-- 1. 检查章节/资源权限存在
SELECT IF(COUNT(*) = 10,
  '[PASS] 章节/资源权限共 10 个',
  CONCAT('[FAIL] 章节/资源权限数量异常，实际 ', COUNT(*))
) AS result
FROM sys_permission
WHERE perm_code IN (
  'chapter:view', 'chapter:create', 'chapter:edit:self', 'chapter:edit:all', 'chapter:delete',
  'resource:view', 'resource:create', 'resource:edit:self', 'resource:edit:all', 'resource:delete'
);

-- 2. 检查教师拥有章节/资源相关权限
SELECT IF(COUNT(*) = 6,
  '[PASS] teacher 拥有 6 个章节/资源权限',
  CONCAT('[FAIL] teacher 章节/资源权限数量异常，实际 ', COUNT(*))
) AS result
FROM sys_role_permission rp
JOIN sys_permission p ON rp.permission_id = p.id
WHERE rp.role_id = 2
  AND p.perm_code IN (
    'chapter:view', 'chapter:create', 'chapter:edit:self',
    'resource:view', 'resource:create', 'resource:edit:self'
  );

-- 3. 检查学生拥有查看权限
SELECT IF(COUNT(*) = 2,
  '[PASS] student 拥有 chapter:view 和 resource:view',
  CONCAT('[FAIL] student 查看权限数量异常，实际 ', COUNT(*))
) AS result
FROM sys_role_permission rp
JOIN sys_permission p ON rp.permission_id = p.id
WHERE rp.role_id = 3
  AND p.perm_code IN ('chapter:view', 'resource:view');

-- 4. 检查数据权限规则包含 chapter 和 resource
SELECT IF(COUNT(*) = 6,
  '[PASS] chapter/resource 数据权限规则共 6 条',
  CONCAT('[FAIL] chapter/resource 数据权限规则数量异常，实际 ', COUNT(*))
) AS result
FROM sys_data_permission
WHERE resource_type IN ('chapter', 'resource');

-- 5. 列出章节/资源权限明细
SELECT p.perm_code, p.perm_name
FROM sys_permission p
WHERE p.perm_code LIKE 'chapter:%' OR p.perm_code LIKE 'resource:%'
ORDER BY p.id;
