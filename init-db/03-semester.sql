-- ============================================================
-- 学期管理表（管理员手动切换学期方案）
--
-- 设计说明：
-- 1. 一行一个学期，按时间顺序排列；
-- 2. is_current=1 表示当前生效学期，全局只能有一行；
-- 3. 管理员通过接口切换当前学期，系统自动切换所有相关查询的数据来源；
-- 4. 历史学期数据保留可查，学生/教师端默认展示当前学期；
-- 5. 假期状态通过比较当前日期与学期起止日期判断，不单独建假期记录。
-- ============================================================

CREATE TABLE IF NOT EXISTS semester (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL COMMENT '学期名称，如 2026-2027-1',
    start_date DATE NOT NULL COMMENT '学期开学日期',
    end_date DATE NOT NULL COMMENT '学期结束日期（期末考试最后一天）',
    week_count INT DEFAULT 20 COMMENT '教学周数',
    is_current TINYINT(1) DEFAULT 0 COMMENT '是否当前学期（0=否，1=是），全局唯一',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name),
    KEY idx_is_current (is_current),
    KEY idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';

-- 初始化：插入两个学期作为示例数据（管理员可在后台调整）
-- 2025-2026 第二学期（已结束，作为历史数据可查）
INSERT INTO semester (name, start_date, end_date, week_count, is_current) VALUES
('2025-2026-2', '2026-02-28', '2026-07-03', 18, 0);

-- 2026-2027 第一学期（当前学期，9月1日开学）
INSERT INTO semester (name, start_date, end_date, week_count, is_current) VALUES
('2026-2027-1', '2026-09-01', '2027-01-15', 20, 1);

-- 2026-2027 第二学期（下学期，2月底开学）
INSERT INTO semester (name, start_date, end_date, week_count, is_current) VALUES
('2026-2027-2', '2027-02-27', '2027-07-02', 18, 0);
