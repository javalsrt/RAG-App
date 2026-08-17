-- ============================================================
-- Docker 环境初始化脚本
-- 由 Docker 的 docker-entrypoint-initdb.d 自动执行
-- ============================================================

-- seed_all.sql 已包含 USE znxsglTest，但 Docker 环境下数据库已由环境变量创建
USE znxsglTest;

-- 补充: chat_message 表的 is_read 字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'znxsglTest' AND TABLE_NAME = 'chat_message' AND COLUMN_NAME = 'is_read');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE chat_message ADD COLUMN is_read TINYINT(1) DEFAULT 0 NOT NULL AFTER content',
    'SELECT ''is_read 已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补充: student_status 表（学生专注状态，FocusController 使用）
CREATE TABLE IF NOT EXISTS student_status (
    user_id BIGINT PRIMARY KEY,
    status VARCHAR(20) DEFAULT 'idle' COMMENT 'focusing/idle',
    last_active DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 补充: 课程导入记录表（ScheduleImportController 使用）
CREATE TABLE IF NOT EXISTS course_import_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(500) DEFAULT NULL COMMENT '导入时上传的文件名',
    imported_by BIGINT DEFAULT NULL COMMENT '导入人 user_id',
    imported_by_name VARCHAR(100) DEFAULT NULL COMMENT '导入人姓名',
    semester VARCHAR(100) DEFAULT NULL COMMENT '学期',
    total_count INT DEFAULT 0 COMMENT '本次导入总条数',
    success_count INT DEFAULT 0 COMMENT '成功导入条数',
    skip_count INT DEFAULT 0 COMMENT '跳过条数',
    messages TEXT DEFAULT NULL COMMENT '导入结果消息（JSON 数组字符串）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    INDEX idx_imported_by (imported_by),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程导入记录';
