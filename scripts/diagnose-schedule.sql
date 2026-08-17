USE znxsgltest;
-- ========================================================-- 课表数据诊断脚本-- 目标：发现 schedule 表中 weeks 字段异常、分布不一致、统计偏差等问题-- 执行方式：在 MySQL 客户端或数据库管理工具中逐条执行-- ========================================================

-- 1. 检查 weeks 为空的记录
SELECT 'weeks 为空' AS check_item, COUNT(*) AS cnt
FROM schedule
WHERE weeks IS NULL OR weeks = '' OR weeks = '[]';

-- 2. 检查 weeks 包含范围写法（如 [1-6]）的记录
SELECT 'weeks 含范围写法' AS check_item, COUNT(*) AS cnt
FROM schedule
WHERE weeks REGEXP '^\\[[0-9]+-[0-9]+\\]$';

-- 3. 检查 weeks 不是合法 JSON 数组的记录（样例，仅 MySQL 8.0+）
SELECT 'weeks 不是合法 JSON 数组' AS check_item, COUNT(*) AS cnt
FROM schedule
WHERE weeks IS NOT NULL AND weeks != '' AND weeks != '[]'
  AND JSON_VALID(weeks) = 0;

-- 4. 检查同一课程在同一班级同一时间段存在多条记录（重复排课）
SELECT '同一课程班级时段重复记录' AS check_item, COUNT(*) AS cnt
FROM (
    SELECT s.course_name, u.class_id, s.day_of_week, s.start_time, s.end_time, s.weeks, COUNT(*) AS c
    FROM schedule s
    JOIN user u ON u.id = s.user_id
    GROUP BY s.course_name, u.class_id, s.day_of_week, s.start_time, s.end_time, s.weeks
    HAVING c > 1
) t;

-- 5. 列出各课程在当前学期的周次分布（按课程名聚合）
SELECT s.course_name,
       COUNT(DISTINCT u.class_id) AS class_count,
       COUNT(*) AS total_records,
       GROUP_CONCAT(DISTINCT s.weeks ORDER BY s.weeks SEPARATOR ' | ') AS week_samples
FROM schedule s
JOIN user u ON u.id = s.user_id
WHERE s.day_of_week > 0
GROUP BY s.course_name
ORDER BY s.course_name;

-- 6. 详细列出 Python编程入门 在人工智能2024级1班的按周分布
-- 需要先知道班级 ID，或者通过 class_name 查询
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
WHERE s.course_name = 'Python编程入门'
  AND ci.class_name = '人工智能2024级1班'
GROUP BY s.course_name, s.day_of_week, s.start_time, s.end_time, s.start_node, s.step, s.classroom, s.weeks, s.status
ORDER BY s.day_of_week, s.start_time;

-- 7. 按周次查看某班级所有课程的覆盖情况（验证 JSON_CONTAINS 是否生效）
-- 将 @target_class_id 替换为实际班级 ID
SET @target_class_id = (SELECT id FROM class_info WHERE class_name = '人工智能2024级1班' LIMIT 1);
SELECT w.week_no,
       COUNT(DISTINCT CONCAT(s.course_name, '-', s.day_of_week, '-', s.start_node)) AS slot_count
FROM (
    SELECT 1 AS week_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
    UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
) w
LEFT JOIN schedule s ON JSON_CONTAINS(s.weeks, CAST(w.week_no AS JSON))
LEFT JOIN user u ON u.id = s.user_id AND u.class_id = @target_class_id
WHERE s.day_of_week > 0
GROUP BY w.week_no
ORDER BY w.week_no;

-- 8. 检查 weeks 字段存储的是字符串数字而非数组（如 "[1,2,3]" 是对的，"1,2,3" 是错的）
SELECT 'weeks 缺少方括号' AS check_item, COUNT(*) AS cnt
FROM schedule
WHERE weeks IS NOT NULL AND weeks != '' AND weeks NOT LIKE '[%' AND weeks NOT LIKE '{%';

-- 9. 检查课程总课时（credit）是否小于单周最大排课节数
-- 结果说明：若某课程在任意一周占用的节数 > course.credit，排课弹窗会显示「已超 X 课时」
SELECT '单周排课超过课程总课时' AS check_item, COUNT(*) AS cnt
FROM (
    SELECT s.course_name, u.class_id,
           JSON_UNQUOTE(JSON_EXTRACT(s.weeks, '$[0]')) AS sample_week,
           SUM(s.step) AS week_slots
    FROM schedule s
    JOIN user u ON u.id = s.user_id
    JOIN course c ON c.course_name = s.course_name
    WHERE s.day_of_week > 0
    GROUP BY s.course_name, u.class_id, JSON_UNQUOTE(JSON_EXTRACT(s.weeks, '$[0]')), c.credit
    HAVING week_slots > c.credit
) t;

-- 10. 列出每门课程在班级中的占用节数与课程 credit 对比（取第一个周次样例）
SELECT s.course_name,
       ci.class_name,
       c.credit AS course_credit,
       SUM(s.step) AS slots_in_sample_week,
       (SUM(s.step) - c.credit) AS over_count
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
JOIN course c ON c.course_name = s.course_name
WHERE s.day_of_week > 0
  AND JSON_CONTAINS(s.weeks, CAST(1 AS JSON))
GROUP BY s.course_name, ci.class_name, c.credit
HAVING slots_in_sample_week > c.credit
ORDER BY over_count DESC;
