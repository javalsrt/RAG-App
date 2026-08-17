USE znxsgltest;
-- ========================================================
-- 阶段1：课程章节与资源体系表结构验证脚本
-- 目标：确认 course_chapter、course_lesson 表结构正确
-- 前置条件：已执行 init-db/10-course-chapter.sql
-- 通过标准：以下所有检查项均为 [PASS]
-- ========================================================

-- 1. 检查 course_chapter 表是否存在
SELECT IF(COUNT(*) = 1,
  '[PASS] course_chapter 表存在',
  '[FAIL] course_chapter 表不存在'
) AS result
FROM information_schema.TABLES
WHERE table_schema = 'znxsgltest' AND table_name = 'course_chapter';

-- 2. 检查 course_chapter 字段完整性
SELECT IF(COUNT(*) >= 10,
  '[PASS] course_chapter 字段完整',
  CONCAT('[FAIL] course_chapter 字段不完整，实际 ', COUNT(*))
) AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'course_chapter'
  AND column_name IN ('id', 'course_id', 'chapter_no', 'chapter_name', 'description', 'sort_order', 'status', 'deleted', 'create_time', 'update_time');

-- 3. 检查 course_lesson 表是否存在
SELECT IF(COUNT(*) = 1,
  '[PASS] course_lesson 表存在',
  '[FAIL] course_lesson 表不存在'
) AS result
FROM information_schema.TABLES
WHERE table_schema = 'znxsgltest' AND table_name = 'course_lesson';

-- 4. 检查 course_lesson 字段完整性
SELECT IF(COUNT(*) >= 12,
  '[PASS] course_lesson 字段完整',
  CONCAT('[FAIL] course_lesson 字段不完整，实际 ', COUNT(*))
) AS result
FROM information_schema.COLUMNS
WHERE table_schema = 'znxsgltest' AND table_name = 'course_lesson'
  AND column_name IN ('id', 'chapter_id', 'lesson_no', 'lesson_name', 'resource_type', 'resource_url', 'duration', 'content', 'sort_order', 'status', 'deleted', 'create_time', 'update_time');

-- 5. 检查 course_chapter 索引
SELECT IF(COUNT(*) >= 2,
  '[PASS] course_chapter 索引完整',
  CONCAT('[FAIL] course_chapter 索引缺失，实际 ', COUNT(*))
) AS result
FROM information_schema.STATISTICS
WHERE table_schema = 'znxsgltest' AND table_name = 'course_chapter'
  AND index_name IN ('idx_course_id', 'idx_sort_order');

-- 6. 检查 course_lesson 索引
SELECT IF(COUNT(*) >= 2,
  '[PASS] course_lesson 索引完整',
  CONCAT('[FAIL] course_lesson 索引缺失，实际 ', COUNT(*))
) AS result
FROM information_schema.STATISTICS
WHERE table_schema = 'znxsgltest' AND table_name = 'course_lesson'
  AND index_name IN ('idx_chapter_id', 'idx_sort_order');
