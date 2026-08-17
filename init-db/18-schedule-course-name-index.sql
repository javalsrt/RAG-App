-- 优化：为 schedule 表 course_name 增加索引，加速课程管理接口中按课程名聚合排课记录的查询
-- 说明：批量查询课程列表时按 s.course_name 过滤/分组，无索引会导致全表扫描

USE znxsglTest;

-- 存在性判断后再创建（MySQL 不支持 CREATE INDEX IF NOT EXISTS）
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'znxsglTest' AND table_name = 'schedule' AND index_name = 'idx_schedule_course_name'
);

SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_schedule_course_name ON schedule (course_name)',
    'SELECT ''INDEX ALREADY EXISTS'' AS result');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'DONE' AS result;
