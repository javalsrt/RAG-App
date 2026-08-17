USE znxsgltest;
-- ========================================================-- 修复人工智能2024级1班暑假培训课表的 weeks 字段-- 适用场景：导入后 schedule.weeks 被错误存为 1-18 周，需按课程实际周次修正-- 执行后请在教师课表中刷新验证-- ========================================================

UPDATE schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
SET s.weeks = CASE
    WHEN s.course_name IN ('Python编程入门', '机器学习基础') THEN '[1,2,3,4,5,6]'
    WHEN s.course_name IN ('深度学习实践', '数据可视化') THEN '[2,3,4,5,6]'
    WHEN s.course_name = '自然语言处理入门' THEN '[3,4,5,6]'
    ELSE s.weeks
END
WHERE ci.class_name = '人工智能2024级1班'
  AND s.semester = '2026-暑假培训'
  AND s.day_of_week > 0;

-- 验证修复结果
SELECT s.course_name,
       s.day_of_week,
       s.start_node,
       s.step,
       COUNT(DISTINCT s.weeks) AS week_variants,
       MAX(s.weeks) AS weeks
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE ci.class_name = '人工智能2024级1班'
  AND s.semester = '2026-暑假培训'
GROUP BY s.course_name, s.day_of_week, s.start_node, s.step
ORDER BY s.course_name, s.day_of_week, s.start_node;
