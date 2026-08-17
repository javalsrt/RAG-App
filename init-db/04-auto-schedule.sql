-- ============================================================
-- 全自动排课系统 - 数据库表结构
--
-- 表清单：
--   1. classroom        教室资源表（普通教室/机房/实训室/琴房/舞蹈室等）
--   2. teaching_task    教学任务表（班级+课程+教师+周课时 = 排课需求）
--   3. schedule_rule    排课规则表（公共基础课/专业核心课 的排课偏好）
--   4. schedule_lock    课表锁定表（教师调课后锁定，重排时不覆盖）
--
-- 设计原则：
--   - 所有表都带 semester 字段，按学期隔离
--   - teaching_task 是排课的"输入"，schedule 是排课的"输出"
--   - schedule_lock 保证教师调课后的课在重新排课时不被覆盖
-- ============================================================

-- ============================================================
-- 1. 教室资源表
-- ============================================================
CREATE TABLE IF NOT EXISTS classroom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '教室名称，如"实训楼A301"',
    type VARCHAR(32) NOT NULL COMMENT '类型：normal=普通教室, lab=专业实训室, computer=计算机机房, music=琴房, dance=舞蹈室, art=美术室, sports=运动场',
    capacity INT DEFAULT 50 COMMENT '容纳人数',
    building VARCHAR(64) COMMENT '所在楼',
    floor INT COMMENT '楼层',
    equipment TEXT COMMENT '设备清单（JSON数组或逗号分隔）',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_type (type),
    KEY idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室资源表';

-- 示例数据：一所中职学校的典型教室配置
INSERT INTO classroom (name, type, capacity, building, floor) VALUES
('教学楼A101', 'normal', 50, '教学楼A', 1),
('教学楼A102', 'normal', 50, '教学楼A', 1),
('教学楼A201', 'normal', 50, '教学楼A', 2),
('教学楼A202', 'normal', 50, '教学楼A', 2),
('教学楼B101', 'normal', 50, '教学楼B', 1),
('教学楼B102', 'normal', 50, '教学楼B', 1),
('计算机机房1', 'computer', 45, '实训楼', 2),
('计算机机房2', 'computer', 45, '实训楼', 2),
('电子技术实训室', 'lab', 40, '实训楼', 1),
('单片机实训室', 'lab', 35, '实训楼', 3),
('琴房1', 'music', 15, '艺术楼', 1),
('琴房2', 'music', 15, '艺术楼', 1),
('舞蹈教室', 'dance', 30, '艺术楼', 2),
('美术手工室', 'art', 35, '艺术楼', 3);

-- ============================================================
-- 2. 教学任务表（排课输入）
-- ============================================================
CREATE TABLE IF NOT EXISTS teaching_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester VARCHAR(32) NOT NULL COMMENT '学期，如2026-2027-1',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    course_name VARCHAR(128) COMMENT '课程名称（冗余，方便查询）',
    teacher_id BIGINT COMMENT '教师ID（teacher表主键）',
    teacher_name VARCHAR(64) COMMENT '教师姓名（冗余）',
    weekly_hours INT NOT NULL COMMENT '每周课时数',
    consecutive INT DEFAULT 1 COMMENT '连堂节数：1=单节, 2=两连堂, 3=三连堂',
    preferred_room_type VARCHAR(32) COMMENT '首选教室类型：normal/lab/computer/music/dance/art/sports',
    preferred_period VARCHAR(32) COMMENT '首选时段：morning=上午, afternoon=下午, any=不限',
    priority INT DEFAULT 5 COMMENT '排课优先级（1-10，越小越先排）',
    status VARCHAR(16) DEFAULT 'pending' COMMENT '状态：pending=待排, scheduled=已排, failed=排课失败, locked=锁定',
    fail_reason VARCHAR(512) COMMENT '排课失败原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sem_class_course (semester, class_id, course_id),
    KEY idx_semester (semester),
    KEY idx_status (status),
    KEY idx_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学任务表（排课输入）';

-- ============================================================
-- 3. 排课规则表（可选，提供默认规则）
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_type VARCHAR(32) NOT NULL COMMENT '课程类型：public_basic=公共基础课, professional_core=专业核心课, skill=专业技能课, art=艺术实践课',
    subject VARCHAR(64) COMMENT '具体科目（空=通用规则）',
    preferred_period VARCHAR(32) COMMENT '建议时段：morning/afternoon/any',
    preferred_room_type VARCHAR(32) COMMENT '建议教室类型',
    consecutive INT COMMENT '建议连堂节数',
    priority_boost INT DEFAULT 0 COMMENT '优先级加成（负数=更先排）',
    description VARCHAR(256) COMMENT '规则说明',
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排课规则表';

-- 中职学校标准排课规则（参考《中等职业学校公共基础课程方案》）
INSERT INTO schedule_rule (course_type, subject, preferred_period, preferred_room_type, consecutive, priority_boost, description) VALUES
('public_basic', '语文', 'morning', 'normal', 1, -1, '语文课建议安排在上午'),
('public_basic', '数学', 'morning', 'normal', 1, -1, '数学课建议安排在上午'),
('public_basic', '英语', 'any', 'normal', 1, 0, '英语课时段不限'),
('public_basic', '思想政治', 'afternoon', 'normal', 1, 1, '思政课建议安排在下午'),
('public_basic', '体育与健康', 'afternoon', 'sports', 2, 2, '体育课连排2节，安排在下午'),
('public_basic', '信息技术', 'any', 'computer', 2, 0, '信息技术课连排2节，机房上课'),
('professional_core', NULL, 'morning', 'lab', 2, -2, '专业核心课优先上午，建议连排2节'),
('skill', NULL, 'afternoon', 'lab', 3, -3, '实训技能课连排3节，下午安排'),
('art', '舞蹈', 'afternoon', 'dance', 2, 0, '舞蹈课连排2节，舞蹈室'),
('art', '幼儿歌曲与伴奏', 'any', 'music', 2, 0, '琴法课连排2节，琴房上课'),
('art', '幼儿美术与手工', 'afternoon', 'art', 2, 1, '美术手工课连排2节');

-- ============================================================
-- 4. 课表锁定表（教师调课后锁定，重排时不被覆盖）
--
-- 作用：
--   1. 教师自主调课后，将调课后的 schedule 记录锁定
--   2. 管理员重新执行"一键排课"时，锁定的课作为硬约束
--   3. 锁定的课位置不变，只排未锁定的课
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_lock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL COMMENT '对应 schedule 表记录ID',
    semester VARCHAR(32) NOT NULL,
    locked_by BIGINT NOT NULL COMMENT '锁定人 user_id（教师）',
    locked_by_name VARCHAR(64) COMMENT '锁定人姓名',
    reason VARCHAR(256) COMMENT '锁定原因（调课说明）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_schedule (schedule_id),
    KEY idx_semester (semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课表锁定表（教师调课后不被自动排课覆盖）';
