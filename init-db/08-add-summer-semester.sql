USE znxsgltest;

-- 添加2026暑假培训学期（无需设为当前学期，系统会自动检测日期范围内的EXTRA类型学期）
INSERT IGNORE INTO semester (name, start_date, end_date, week_count, semester_type, is_current) VALUES
('2026-暑假培训', '2026-07-25', '2026-08-31', 6, 'EXTRA', 0);

-- 查看所有学期
SELECT id, name, start_date, end_date, week_count, semester_type, is_current FROM semester ORDER BY start_date;
