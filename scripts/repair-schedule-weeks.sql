USE znxsgltest;
-- ========================================================-- 课表 weeks 字段修复脚本-- 说明：将导入/排课时产生的范围写法、格式错误修复为标准 JSON 数组。-- 修复前请先执行 scripts/diagnose-schedule.sql 查看异常数据。-- 执行前建议先备份：CREATE TABLE schedule_backup AS SELECT * FROM schedule;-- ========================================================

-- 1. 修复常见的范围写法（如 [1-6]、[1-16]、[2-6]、[3-6]）-- 注意：只处理精确匹配这些范围的记录；若有其他范围请手动补充。
UPDATE schedule SET weeks = '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]' WHERE weeks = '[1-16]';
UPDATE schedule SET weeks = '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]' WHERE weeks = '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]';
UPDATE schedule SET weeks = '[1,2,3,4,5,6]' WHERE weeks = '[1-6]';
UPDATE schedule SET weeks = '[2,3,4,5,6]' WHERE weeks = '[2-6]';
UPDATE schedule SET weeks = '[3,4,5,6]' WHERE weeks = '[3-6]';
UPDATE schedule SET weeks = '[4,5,6]' WHERE weeks = '[4-6]';
UPDATE schedule SET weeks = '[1,2]' WHERE weeks = '[1-2]';
UPDATE schedule SET weeks = '[1,2,3]' WHERE weeks = '[1-3]';
UPDATE schedule SET weeks = '[1,2,3,4]' WHERE weeks = '[1-4]';
UPDATE schedule SET weeks = '[1,2,3,4,5]' WHERE weeks = '[1-5]';

-- 2. 修复缺少外层方括号但数字合法的 weeks（如 "1,2,3" → "[1,2,3]"）
-- 仅处理纯数字和逗号组成的字符串
UPDATE schedule
SET weeks = CONCAT('[', weeks, ']')
WHERE weeks IS NOT NULL
  AND weeks != ''
  AND weeks NOT LIKE '[%'
  AND weeks REGEXP '^[0-9]+(,[0-9]+)*$';

-- 3. 删除 weeks 中多余的空格（如 "[1, 2, 3]" 是合法 JSON，但统一去掉空格更规范）
UPDATE schedule
SET weeks = REPLACE(weeks, ' ', '')
WHERE weeks LIKE '% %';

-- 4. 修复空 weeks 为默认全学期（16 周）-- 仅当课程已有 day_of_week > 0 时才补全；占位记录（day_of_week=0）保持 []。
UPDATE schedule
SET weeks = '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]'
WHERE (weeks IS NULL OR weeks = '' OR weeks = '[]')
  AND day_of_week > 0;

-- 5. 验证修复结果：再次检查异常记录
SELECT '修复后仍含范围写法' AS check_item, COUNT(*) AS cnt
FROM schedule
WHERE weeks REGEXP '^\\[[0-9]+-[0-9]+\\]$';

SELECT '修复后 weeks 为空且 day_of_week>0' AS check_item, COUNT(*) AS cnt
FROM schedule
WHERE (weeks IS NULL OR weeks = '' OR weeks = '[]')
  AND day_of_week > 0;

-- 6. 展示修复后 Python 编程入门 的周次样例
SELECT DISTINCT weeks
FROM schedule
WHERE course_name = 'Python编程入门';
