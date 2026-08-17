USE znxsgltest;
-- ========================================================
-- 阶段1：RBAC 表结构验证脚本
-- 目标：确认 sys_role、sys_permission、sys_role_permission、
--       sys_user_role、sys_data_permission 五张表结构正确
-- 前置条件：数据库已创建并选中
-- 通过标准：5 张表均存在且字段完整
-- ========================================================

-- 检查表是否存在
SET @expected_tables = 5;
SELECT COUNT(*) INTO @actual_tables
FROM information_schema.TABLES
WHERE table_schema = 'znxsgltest'
  AND table_name IN ('sys_role', 'sys_permission', 'sys_role_permission', 'sys_user_role', 'sys_data_permission');

SELECT IF(@actual_tables = @expected_tables,
  '[PASS] RBAC 5 张表均存在',
  CONCAT('[FAIL] RBAC 表缺失，期望 ', @expected_tables, ' 张，实际 ', @actual_tables, ' 张')
) AS result;

-- 检查 sys_role 字段
SELECT IF(COUNT(*) >= 7, '[PASS] sys_role 字段完整', '[FAIL] sys_role 字段不完整') AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'sys_role'
  AND column_name IN ('id', 'role_code', 'role_name', 'description', 'status', 'create_time', 'update_time');

-- 检查 sys_permission 字段
SELECT IF(COUNT(*) >= 9, '[PASS] sys_permission 字段完整', '[FAIL] sys_permission 字段不完整') AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'sys_permission'
  AND column_name IN ('id', 'perm_code', 'perm_name', 'perm_type', 'parent_id', 'path', 'icon', 'sort_order', 'status');

-- 检查 sys_role_permission 字段
SELECT IF(COUNT(*) >= 2, '[PASS] sys_role_permission 字段完整', '[FAIL] sys_role_permission 字段不完整') AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'sys_role_permission'
  AND column_name IN ('role_id', 'permission_id');

-- 检查 sys_user_role 字段
SELECT IF(COUNT(*) >= 2, '[PASS] sys_user_role 字段完整', '[FAIL] sys_user_role 字段不完整') AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'sys_user_role'
  AND column_name IN ('user_id', 'role_id');

-- 检查 sys_data_permission 字段
SELECT IF(COUNT(*) >= 5, '[PASS] sys_data_permission 字段完整', '[FAIL] sys_data_permission 字段不完整') AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'sys_data_permission'
  AND column_name IN ('id', 'role_id', 'resource_type', 'data_scope', 'description');
