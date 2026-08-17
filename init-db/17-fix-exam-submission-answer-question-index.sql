-- 修复：exam_submission_answer 表 question_index 字段无默认值，自动保存(requireSnapshot=false)时未填导致插入失败
-- 方案：给该字段添加默认值 0 兜底，同时清理可能存在的脏数据

USE znxsglTest;

-- 1. 给 question_index 添加默认值 0（已有 NOT NULL 约束，仅加默认值）
ALTER TABLE exam_submission_answer MODIFY COLUMN question_index INT NOT NULL DEFAULT 0 COMMENT '题号 1-N';

-- 2. 修复历史脏数据：题号为 0 或 NULL 的，根据题目表回填真实题号
UPDATE exam_submission_answer a
LEFT JOIN exam_question q ON q.id = a.question_id
SET a.question_index = COALESCE(q.question_index, 0)
WHERE a.question_index IS NULL OR a.question_index = 0;

SELECT 'DONE' AS result;