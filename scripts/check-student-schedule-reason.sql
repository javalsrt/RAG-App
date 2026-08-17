USE znxsgltest;
-- ========================================================-- 排查学生 App 课表为空的原因-- 检查 semester、schedule.status、班级关联、日期范围-- ========================================================

-- 1. 查看 semester 表中是否存在 2026-暑假测试 学期
SELECT id, name, semester_type, start_date, end_date, week_count, is_current
FROM semester
WHERE name IN ('2026-暑假测试', '暑假培训', '2026-暑假培训')
   OR semester_type = 'EXTRA';

-- 2. 查看人工智能2024级1班关联的学期
SELECT s.id, s.name, s.semester_type, s.start_date, s.end_date, s.week_count, s.is_current, sc.class_id
FROM semester s
JOIN semester_class sc ON sc.semester_id = s.id
JOIN class_info ci ON ci.id = sc.class_id
WHERE ci.class_name = '人工智能2024级1班';

-- 3. 查看该班级 schedule 的 status 分布
SELECT s.status, s.semester, COUNT(*) AS cnt
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE ci.class_name = '人工智能2024级1班'
  AND s.day_of_week > 0
GROUP BY s.status, s.semester;
