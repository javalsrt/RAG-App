-- ========================================================
-- RBAC 默认角色、权限、数据规则初始化脚本
-- 同步现有 user.role 字段到 sys_user_role 关联表
-- ========================================================

USE znxsgltest;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 清空旧数据（幂等执行）
-- ----------------------------
DELETE FROM sys_data_permission;
DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_permission;
DELETE FROM sys_role;

-- ----------------------------
-- 初始化角色
-- ----------------------------
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'admin', '系统管理员', '拥有系统全部权限'),
(2, 'teacher', '教师', '管理自己所授课程、班级及学生'),
(3, 'student', '学生', '查看自己的课程、课表、学习数据');

-- ----------------------------
-- 初始化菜单权限
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, path, icon, sort_order) VALUES
(100, 'dashboard:view', '数据概览', 'MENU', NULL, '/dashboard', 'LayoutDashboard', 1),
(101, 'course:manage', '课程管理', 'MENU', NULL, '/courses', 'BookOpen', 2),
(102, 'learning:stats', '学习统计', 'MENU', NULL, '/stats', 'BarChart3', 3),
(103, 'staff:manage', '人员管理', 'MENU', NULL, '/staff', 'Users', 4),
(104, 'semester:manage', '学期管理', 'MENU', NULL, '/semesters', 'Calendar', 5),
(105, 'chapter:manage', '章节管理', 'MENU', NULL, '/course-chapters', 'ListTree', 6),
(106, 'system:config', '系统配置', 'MENU', NULL, '/system', 'Settings', 7);

-- ----------------------------
-- 初始化按钮/API权限（课程模块）
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, sort_order) VALUES
(200, 'course:view', '查看课程', 'BUTTON', 101, 1),
(201, 'course:create', '创建课程', 'BUTTON', 101, 2),
(202, 'course:edit:all', '编辑所有课程', 'BUTTON', 101, 3),
(203, 'course:edit:self', '编辑自己的课程', 'BUTTON', 101, 4),
(204, 'course:delete', '删除课程', 'BUTTON', 101, 5),
(205, 'course:publish', '上架课程', 'BUTTON', 101, 6),
(206, 'course:unpublish', '下架课程', 'BUTTON', 101, 7),
(207, 'course:schedule', '课程排课', 'BUTTON', 101, 8),
(208, 'course:import', '导入课程', 'BUTTON', 101, 9);

-- ----------------------------
-- 初始化按钮/API权限（人员模块）
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, sort_order) VALUES
(209, 'staff:view', '查看人员', 'BUTTON', 103, 1),
(210, 'staff:create', '新增人员', 'BUTTON', 103, 2),
(211, 'staff:edit', '编辑人员', 'BUTTON', 103, 3),
(212, 'staff:delete', '删除人员', 'BUTTON', 103, 4);

-- ----------------------------
-- 初始化按钮/API权限（学期模块）
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, sort_order) VALUES
(213, 'semester:view', '查看学期', 'BUTTON', 104, 1),
(214, 'semester:create', '新增学期', 'BUTTON', 104, 2),
(215, 'semester:edit', '编辑学期', 'BUTTON', 104, 3),
(216, 'semester:delete', '删除学期', 'BUTTON', 104, 4);

-- ----------------------------
-- 初始化按钮/API权限（章节模块）
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, sort_order) VALUES
(217, 'chapter:view', '查看章节', 'BUTTON', 105, 1),
(218, 'chapter:create', '创建章节', 'BUTTON', 105, 2),
(219, 'chapter:edit:self', '编辑自己的章节', 'BUTTON', 105, 3),
(220, 'chapter:edit:all', '编辑所有章节', 'BUTTON', 105, 4),
(221, 'chapter:delete', '删除章节', 'BUTTON', 105, 5),
(228, 'chapter:import', '导入章节', 'BUTTON', 105, 6);

-- ----------------------------
-- 初始化按钮/API权限（课时/资源模块）
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, sort_order) VALUES
(223, 'resource:view', '查看课时资源', 'BUTTON', 105, 6),
(224, 'resource:create', '创建课时资源', 'BUTTON', 105, 7),
(225, 'resource:edit:self', '编辑自己的课时资源', 'BUTTON', 105, 8),
(226, 'resource:edit:all', '编辑所有课时资源', 'BUTTON', 105, 9),
(227, 'resource:delete', '删除课时资源', 'BUTTON', 105, 10);

-- ----------------------------
-- 初始化按钮/API权限（统计模块）
-- ----------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, sort_order) VALUES
(222, 'stats:view', '查看学习统计', 'BUTTON', 102, 1);

-- ----------------------------
-- 角色-权限关联：管理员拥有全部权限
-- ----------------------------
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE status = 1;

-- ----------------------------
-- 角色-权限关联：教师
-- ----------------------------
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 100), -- 数据概览
(2, 101), -- 课程管理菜单
(2, 200), -- 查看课程
(2, 203), -- 编辑自己的课程
(2, 205), -- 上架课程
(2, 206), -- 下架课程
(2, 207), -- 课程排课
(2, 102), -- 学习统计菜单
(2, 222), -- 查看学习统计
(2, 105), -- 章节管理菜单
(2, 217), -- 查看章节
(2, 218), -- 创建章节
(2, 219), -- 编辑自己的章节
(2, 221), -- 删除章节
(2, 228), -- 导入章节
(2, 223), -- 查看课时资源
(2, 224), -- 创建课时资源
(2, 225), -- 编辑自己的课时资源
(2, 227); -- 删除课时资源

-- ----------------------------
-- 角色-权限关联：学生
-- ----------------------------
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 100), -- 数据概览
(3, 101), -- 课程管理菜单（只读）
(3, 200), -- 查看课程
(3, 102), -- 学习统计菜单
(3, 222), -- 查看学习统计
(3, 105), -- 章节管理菜单（只读）
(3, 217), -- 查看章节
(3, 223); -- 查看课时资源

-- ----------------------------
-- 数据权限规则
-- ----------------------------
INSERT INTO sys_data_permission (role_id, resource_type, data_scope, description) VALUES
(1, 'course', 'ALL', '管理员可管理所有课程'),
(1, 'chapter', 'ALL', '管理员可管理所有章节'),
(1, 'resource', 'ALL', '管理员可管理所有课时资源'),
(1, 'student', 'ALL', '管理员可管理所有学生'),
(1, 'score', 'ALL', '管理员可查看所有成绩'),
(2, 'course', 'SELF', '教师只能管理自己所授课程'),
(2, 'chapter', 'SELF', '教师只能管理自己所授课程的章节'),
(2, 'resource', 'SELF', '教师只能管理自己所授课程的课时资源'),
(2, 'student', 'CLASS', '教师只能查看所教班级学生'),
(2, 'score', 'CLASS', '教师只能查看所教班级成绩'),
(3, 'course', 'SELF', '学生只能查看自己已选课程'),
(3, 'chapter', 'SELF', '学生只能查看自己已选课程的章节'),
(3, 'resource', 'SELF', '学生只能查看自己已选课程的课时资源'),
(3, 'student', 'SELF', '学生只能查看自己的信息'),
(3, 'score', 'SELF', '学生只能查看自己的成绩');

-- ----------------------------
-- 同步现有 user.role 到 sys_user_role
-- role: 1学生 -> sys_role.id=3
-- role: 2教师 -> sys_role.id=2
-- role: 3管理员 -> sys_role.id=1
-- ----------------------------
INSERT INTO sys_user_role (user_id, role_id)
SELECT id, CASE role
  WHEN 1 THEN 3
  WHEN 2 THEN 2
  WHEN 3 THEN 1
  ELSE 3
END
FROM user
WHERE role IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
