USE znxsgltest;
-- ========================================================-- 查看人工智能2024级1班所有课表记录分布-- 用于排查旧数据未清除、学期不一致、weeks异常等问题-- ========================================================

-- 1. 按学期统计该班级的 schedule 记录数
SELECT s.semester,
       COUNT(*) AS total_records,
       COUNT(DISTINCT s.course_name) AS course_count,
       COUNT(DISTINCT s.weeks) AS week_variants,
       MIN(s.status) AS min_status,
       MAX(s.status) AS max_status
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE ci.class_name = '人工智能2024级1班'
  AND s.day_of_week > 0
GROUP BY s.semester
ORDER BY s.semester;

-- 2. 列出每门课程、每个时间段的 weeks 分布（区分 status）
SELECT s.course_name,
       s.semester,
       s.day_of_week,
       s.start_node,
       s.step,
       s.status,
       COUNT(*) AS record_count,
       s.weeks
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE ci.class_name = '人工智能2024级1班'
  AND s.day_of_week > 0
GROUP BY s.course_name, s.semester, s.day_of_week, s.start_node, s.step, s.status, s.weeks
ORDER BY s.course_name, s.semester, s.day_of_week, s.start_node, s.status;
