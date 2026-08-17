USE znxsglTest;
ALTER TABLE exam_submission_answer ADD COLUMN teacher_comment TEXT NULL COMMENT '教师评分评语' AFTER ai_comment;
ALTER TABLE exam_submission_answer ADD COLUMN score_adjust_count TINYINT DEFAULT 0 COMMENT '教师调整次数，限2次' AFTER teacher_comment;
ALTER TABLE exam_submission_answer ADD COLUMN max_score INT DEFAULT NULL COMMENT '本题满分快照' AFTER score;
ALTER TABLE exam_submission ADD COLUMN auto_score INT DEFAULT NULL COMMENT 'AI自动总评分（教师调整前分数）' AFTER total_score;
SELECT 'ALTER DONE' AS result;
