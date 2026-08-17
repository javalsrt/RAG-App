USE znxsgltest;
-- ========================================================-- 修复暑假培训课表数据问题-- 适用场景：导入后课程 credit 过小、周次与课表不一致、出现「已超 X 课时」等-- 执行步骤：--   1. 执行本脚本更新 course.credit 并清掉旧 schedule--   2. 重新导入 init-db/summer-training-schedule.xlsx-- ========================================================

-- 1. 修正暑假课程的总课时（按每周实际占用节数设置，与排课弹窗上限对齐）
UPDATE course
SET credit = CASE course_name
    WHEN 'Python编程入门' THEN 12
    WHEN '机器学习基础' THEN 12
    WHEN '深度学习实践' THEN 12
    WHEN '数据可视化' THEN 12
    WHEN '自然语言处理入门' THEN 4
    ELSE credit
END
WHERE course_name IN ('Python编程入门', '机器学习基础', '深度学习实践', '数据可视化', '自然语言处理入门')
  AND semester = '2026-暑假培训';

-- 2. 清掉人工智能2024级1班暑假培训的全部 schedule 记录（重新导入后会恢复正确数据）
DELETE s FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE ci.class_name = '人工智能2024级1班'
  AND s.semester IN ('暑假培训', '2026-暑假培训');

-- 3. 验证清理结果
SELECT '清理后该班级暑假 schedule 记录数' AS check_item, COUNT(*) AS cnt
FROM schedule s
JOIN user u ON u.id = s.user_id
JOIN class_info ci ON ci.id = u.class_id
WHERE ci.class_name = '人工智能2024级1班'
  AND s.semester = '2026-暑假培训';

-- 4. 验证课程 credit 已更新
SELECT course_name, credit
FROM course
WHERE course_name IN ('Python编程入门', '机器学习基础', '深度学习实践', '数据可视化', '自然语言处理入门')
  AND semester = '2026-暑假培训';

-- 下一步：登录 web-admin，进入「课程导入」→ 重新上传 init-db/summer-training-schedule.xlsx → 确认导入。
