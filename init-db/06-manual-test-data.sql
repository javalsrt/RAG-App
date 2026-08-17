-- ============================================
-- 手动导入测试数据
-- 包含：5名老师（语数英 + 2专业）、35名学生、多门课程
-- 学期：2025-2026-2
-- ============================================

-- ========== 1. 创建班级 ==========
INSERT INTO class_info (class_name, major, department, grade) 
VALUES ('计算机应用2024级1班', '计算机应用技术', '信息工程学院', '2024级');
SET @class_id = LAST_INSERT_ID();

-- ========== 2. 创建5名老师 ==========
-- 语文老师
INSERT INTO teacher (teacher_no, real_name, gender, title, dept_id, email, phone, status)
VALUES ('T2024002', '张雅琴', 2, '副教授', 1, 'zhangyaqin@test.com', '13800000002', 1);
SET @teacher1_id = LAST_INSERT_ID();

-- 数学老师
INSERT INTO teacher (teacher_no, real_name, gender, title, dept_id, email, phone, status)
VALUES ('T2024003', '王建国', 1, '教授', 1, 'wangjianguo@test.com', '13800000003', 1);
SET @teacher2_id = LAST_INSERT_ID();

-- 英语老师
INSERT INTO teacher (teacher_no, real_name, gender, title, dept_id, email, phone, status)
VALUES ('T2024004', '李晓燕', 2, '讲师', 1, 'lixiaoyan@test.com', '13800000004', 1);
SET @teacher3_id = LAST_INSERT_ID();

-- 专业课老师1（Java）
INSERT INTO teacher (teacher_no, real_name, gender, title, dept_id, email, phone, status)
VALUES ('T2024005', '陈志远', 1, '副教授', 1, 'chenzhiyuan@test.com', '13800000005', 1);
SET @teacher4_id = LAST_INSERT_ID();

-- 专业课老师2（数据库）
INSERT INTO teacher (teacher_no, real_name, gender, title, dept_id, email, phone, status)
VALUES ('T2024006', '刘美玲', 2, '讲师', 1, 'liumeiling@test.com', '13800000006', 1);
SET @teacher5_id = LAST_INSERT_ID();

-- ========== 3. 创建老师对应的user账号（用于登录） ==========
-- 密码统一为：123456
INSERT INTO user (username, password_hash, real_name, role, status) VALUES
('zhangyaqin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张雅琴', 2, 1),
('wangjianguo', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王建国', 2, 1),
('lixiaoyan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李晓燕', 2, 1),
('chenzhiyuan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '陈志远', 2, 1),
('liumeiling', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu