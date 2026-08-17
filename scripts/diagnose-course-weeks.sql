USE znxsgltest;
-- ========================================================-- 诊断指定课程在指定班级的周次分布-- 目标：确认 courseMaxWeek 应该返回多少，排查排课弹窗周次范围异常-- 用法：替换下面的课程名和班级名后执行-- ========================================================

SET @course_name = '深度学习实践';
SET @class_name = '人工智能2024级1班';

-- 1. 列出该课程在该班级的所有 weeks 样例
SELECT s.course_name,
       s.day_of_week,
       s.start_time,
       s.end_time,
       s.start_node,
       s.step,
       s.classroom,
       s.weeks,
       s.status,
       COUNT(*) AS student_record_count
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE s.course_name = @course_name
  AND ci.class_name = @class_name
GROUP BY s.course_name, s.day_of_week, s.start_time, s.end_time, s.start_node, s.step, s.classroom, s.weeks, s.status
ORDER BY s.day_of_week, s.start_time;

-- 2. 计算该课程在该班级的最大周次
SELECT @course_name AS course_name,
       MAX(CAST(JSON_UNQUOTE(JSON_EXTRACT(s.weeks, '$[0]')) AS UNSIGNED)) AS sample_first_week,
       MAX(CAST(JSON_UNQUOTE(JSON_EXTRACT(s.weeks, CONCAT('$[', JSON_LENGTH(s.weeks) - 1, ']'))) AS UNSIGNED)) AS sample_last_week,
       MAX(
         (SELECT MAX(CAST(jt.week_no AS UNSIGNED))
          FROM JSON_TABLE(s.weeks, '$[*]' COLUMNS (week_no INT PATH '$')) AS jt)
       ) AS actual_max_week
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE s.course_name = @course_name
  AND ci.class_name = @class_name;

-- 3. 检查学期表配置（如果课程 semester 字段有值）
SELECT s.course_name, s.semester, sem.name, sem.week_count, sem.start_date, sem.end_date
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
LEFT JOIN semester sem ON sem.name = s.semester
WHERE s.course_name = @course_name
  AND ci.class_name = @class_name
LIMIT 1;
