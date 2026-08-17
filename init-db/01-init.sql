-- ============================================================
-- Docker 环境首次启动自动初始化脚本
-- SQL 文件按文件名排序执行，01-init 会最先执行
-- ============================================================

USE znxsglTest;

-- ============================================================
-- 1. 答题系统表
-- ============================================================

-- 答题会话主表
CREATE TABLE IF NOT EXISTS quiz_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subject VARCHAR(100) COMMENT '答题科目',
    subject_type VARCHAR(20) COMMENT '专业/公共',
    session_no INT DEFAULT 1 COMMENT '用户第几次答题',
    total_questions INT COMMENT '总题数',
    answered_count INT DEFAULT 0 COMMENT '已答题数',
    correct_count INT DEFAULT 0 COMMENT '正确数（客观题）',
    skip_count INT DEFAULT 0 COMMENT '跳过/不会',
    total_duration_sec INT DEFAULT 0 COMMENT '总耗时秒',
    scores JSON COMMENT '六维评分',
    strengths JSON COMMENT '长处',
    weaknesses JSON COMMENT '短处',
    suggestion TEXT COMMENT 'AI学习建议',
    study_plan JSON COMMENT '学习计划',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/completed/evaluated',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_user (user_id, created_at)
) ENGINE=InnoDB;

-- 每题作答详情
CREATE TABLE IF NOT EXISTS quiz_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    question_index INT COMMENT '题号 1-N',
    question_type VARCHAR(20) COMMENT '单选/判断/解析/填空',
    subject VARCHAR(100),
    question TEXT COMMENT '题目',
    options JSON COMMENT '选项列表',
    user_answer TEXT COMMENT '学生答案',
    correct_answer VARCHAR(500) COMMENT '正确答案',
    is_correct TINYINT COMMENT '1对 0错 -1不会 -2跳过',
    duration_sec INT DEFAULT 0 COMMENT '本题耗时秒',
    modified_count INT DEFAULT 0 COMMENT '修改次数',
    understood TINYINT DEFAULT 0 COMMENT '0未标记 1已明白',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES quiz_session(id),
    INDEX idx_session (session_id)
) ENGINE=InnoDB;

-- 收藏题目表
CREATE TABLE IF NOT EXISTS question_bookmark (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_type VARCHAR(20) COMMENT '题型',
    subject VARCHAR(100) COMMENT '科目',
    question TEXT COMMENT '题目内容',
    user_answer TEXT COMMENT '学生答案',
    correct_answer VARCHAR(500) COMMENT '正确答案',
    knowledge TEXT COMMENT '知识点解析',
    error_reason TEXT COMMENT '错因分析',
    improve TEXT COMMENT '改进建议',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB;

-- 错题分析结果缓存表
CREATE TABLE IF NOT EXISTS wrong_analysis_cache (
    user_id BIGINT PRIMARY KEY,
    cache_hash VARCHAR(50) DEFAULT '0' COMMENT '缓存hash',
    analysis_json MEDIUMTEXT COMMENT '分析结果JSON',
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB;
