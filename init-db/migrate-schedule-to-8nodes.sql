-- ============================================================
-- 作息表切换迁移脚本：从 12 小节制切换到艺术学部/汽车学部 8 小节制
-- 新节次时间表：
--   第1节 08:10-08:50
--   第2节 09:00-09:40
--   第3节 09:50-10:30
--   第4节 10:40-11:20
--   第5节 15:10-15:50
--   第6节 16:00-16:40
--   第7节 19:50-20:10
--   第8节 20:20-21:00
-- 策略：
--   1. 先备份 schedule 表
--   2. 午休前课程（11:30-12:30）保留数据但 status=0, day_of_week=0
--   3. 其余课程按旧 start_node 语义映射到新 1-8 节
--   4. 截断 step 防止超出 8
--   5. 重新计算 start_time / end_time
--   6. 清理周日排课记录
-- ============================================================

-- 1. 备份
CREATE TABLE IF NOT EXISTS schedule_backup_20260726 AS SELECT * FROM schedule;

-- 2. 映射 start_node（节次语义映射）
UPDATE schedule SET
  start_node = CASE
    WHEN start_node = 1 THEN 1
    WHEN start_node = 2 THEN 2
    WHEN start_node = 3 THEN 3
    WHEN start_node = 4 THEN 4
    WHEN start_node = 5 THEN 5
    WHEN start_node = 6 THEN 6
    WHEN start_node = 7 THEN 7
    WHEN start_node = 8 THEN 7
    WHEN start_node = 9 THEN 8
    WHEN start_node = 10 THEN 8
    WHEN start_node = 11 THEN 8
    WHEN start_node = 12 THEN 8
    ELSE start_node
  END
WHERE day_of_week > 0 AND NOT (start_time >= '11:30:00' AND start_time < '12:30:00');

-- 3. 截断 step
UPDATE schedule SET step = LEAST(step, 9 - start_node)
WHERE day_of_week > 0 AND NOT (start_time >= '11:30:00' AND start_time < '12:30:00');

-- 4. 重新计算 start_time / end_time
UPDATE schedule SET
  start_time = CASE start_node
    WHEN 1 THEN '08:10:00'
    WHEN 2 THEN '09:00:00'
    WHEN 3 THEN '09:50:00'
    WHEN 4 THEN '10:40:00'
    WHEN 5 THEN '15:10:00'
    WHEN 6 THEN '16:00:00'
    WHEN 7 THEN '19:50:00'
    WHEN 8 THEN '20:20:00'
  END,
  end_time = CASE (start_node + step - 1)
    WHEN 1 THEN '08:50:00'
    WHEN 2 THEN '09:40:00'
    WHEN 3 THEN '10:30:00'
    WHEN 4 THEN '11:20:00'
    WHEN 5 THEN '15:50:00'
    WHEN 6 THEN '16:40:00'
    WHEN 7 THEN '20:10:00'
    WHEN 8 THEN '21:00:00'
  END
WHERE day_of_week > 0 AND NOT (start_time >= '11:30:00' AND start_time < '12:30:00');

-- 5. 午休前课程保留数据但不显示
UPDATE schedule SET status = 0, day_of_week = 0
WHERE day_of_week > 0 AND start_time >= '11:30:00' AND start_time < '12:30:00';

-- 6. 清理周日排课
UPDATE schedule SET status = 0, day_of_week = 0 WHERE day_of_week = 7;
