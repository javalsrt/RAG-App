-- ============================================
-- 测试数据：软件技术2024级1班
-- 包含：班级、老师、课程、学生、专注时长、聊天消息、答题记录
-- 学期：2025-2026-2
-- ============================================

-- 1. 先清理可能存在的旧测试数据
DELETE FROM focus_session WHERE user_id IN (SELECT id FROM user WHERE class_id = (SELECT id FROM class_info WHERE class_name = '软件技术2024级1班'));
DELETE FROM chat_message WHERE user_id IN (SELECT id FROM user WHERE class_id = (SELECT id FROM class_info WHERE class_name = '软件技术2024级1班'));
DELETE FROM quiz_session WHERE user_id IN (SELECT id FROM user WHERE class_id = (SELECT id FROM class_info WHERE class_name = '软件技术2024级1班'));
DELETE FROM course_class WHERE class_id = (SELECT id FROM class_info WHERE class_name = '软件技术2024级1班');
DELETE FROM user WHERE class_id = (SELECT id FROM class_info WHERE class_name = '软件技术2024级1班');
DELETE FROM course WHERE course_name IN ('Java程序设计', '数据库原理', 'Web前端开发') AND teacher_id = (SELECT id FROM teacher WHERE real_name = '李文博');
DELETE FROM teacher WHERE real_name = '李文博';
DELETE FROM class_info WHERE class_name = '软件技术2024级1班';

-- 2. 创建班级
INSERT INTO class_info (class_name, major, department, grade) 
VALUES ('软件技术2024级1班', '软件技术', '信息工程学院', '2024级');
SET @class_id = LAST_INSERT_ID();

-- 3. 创建老师
INSERT INTO teacher (teacher_no, real_name, gender, title, dept_id, email, phone, status)
VALUES ('T2024001', '李文博', 1, '讲师', 1, 'liwenbo@test.com', '13800000001', 1);
SET @teacher_id = LAST_INSERT_ID();

-- 4. 创建老师对应的user账号（用于登录）
INSERT INTO user (username, password_hash, real_name, role, status)
VALUES ('liwenbo', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李文博', 2, 1);
-- 密码：123456

-- 5. 创建3门课程
INSERT INTO course (course_name, course_no, teacher_id, semester, course_type, credit, description)
VALUES 
('Java程序设计', 'JAVA001', @teacher_id, '2025-2026-2', '必修', 4.0, 'Java面向对象程序设计基础与应用'),
('数据库原理', 'DB001', @teacher_id, '2025-2026-2', '必修', 3.0, '关系型数据库原理与MySQL应用'),
('Web前端开发', 'WEB001', @teacher_id, '2025-2026-2', '必修', 3.5, 'HTML/CSS/JavaScript前端开发技术');

-- 6. 课程关联班级
INSERT INTO course_class (course_id, class_id, semester)
SELECT id, @class_id, '2025-2026-2' FROM course WHERE teacher_id = @teacher_id;

-- 7. 创建30名学生
INSERT INTO user (student_no, username, password_hash, real_name, role, class_id, major, grade, status) VALUES
('20240101', 'student001', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张伟', 1, @class_id, '软件技术', '2024级', 1),
('20240102', 'student002', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王芳', 1, @class_id, '软件技术', '2024级', 1),
('20240103', 'student003', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李强', 1, @class_id, '软件技术', '2024级', 1),
('20240104', 'student004', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '刘洋', 1, @class_id, '软件技术', '2024级', 1),
('20240105', 'student005', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '陈静', 1, @class_id, '软件技术', '2024级', 1),
('20240106', 'student006', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '杨帆', 1, @class_id, '软件技术', '2024级', 1),
('20240107', 'student007', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '赵磊', 1, @class_id, '软件技术', '2024级', 1),
('20240108', 'student008', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '黄敏', 1, @class_id, '软件技术', '2024级', 1),
('20240109', 'student009', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '周杰', 1, @class_id, '软件技术', '2024级', 1),
('20240110', 'student010', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '吴婷', 1, @class_id, '软件技术', '2024级', 1),
('20240111', 'student011', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '徐浩', 1, @class_id, '软件技术', '2024级', 1),
('20240112', 'student012', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '孙丽', 1, @class_id, '软件技术', '2024级', 1),
('20240113', 'student013', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '马超', 1, @class_id, '软件技术', '2024级', 1),
('20240114', 'student014', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '朱琳', 1, @class_id, '软件技术', '2024级', 1),
('20240115', 'student015', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '胡军', 1, @class_id, '软件技术', '2024级', 1),
('20240116', 'student016', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '郭雪', 1, @class_id, '软件技术', '2024级', 1),
('20240117', 'student017', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '何伟', 1, @class_id, '软件技术', '2024级', 1),
('20240118', 'student018', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '高燕', 1, @class_id, '软件技术', '2024级', 1),
('20240119', 'student019', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '林峰', 1, @class_id, '软件技术', '2024级', 1),
('20240120', 'student020', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '罗娜', 1, @class_id, '软件技术', '2024级', 1),
('20240121', 'student021', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '郑浩', 1, @class_id, '软件技术', '2024级', 1),
('20240122', 'student022', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '梁欣', 1, @class_id, '软件技术', '2024级', 1),
('20240123', 'student023', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '谢鹏', 1, @class_id, '软件技术', '2024级', 1),
('20240124', 'student024', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '宋佳', 1, @class_id, '软件技术', '2024级', 1),
('20240125', 'student025', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '唐宇', 1, @class_id, '软件技术', '2024级', 1),
('20240126', 'student026', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '韩梅', 1, @class_id, '软件技术', '2024级', 1),
('20240127', 'student027', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '冯阳', 1, @class_id, '软件技术', '2024级', 1),
('20240128', 'student028', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '董悦', 1, @class_id, '软件技术', '2024级', 1),
('20240129', 'student029', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '萧然', 1, @class_id, '软件技术', '2024级', 1),
('20240130', 'student030', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '程亮', 1, @class_id, '软件技术', '2024级', 1);

-- 8. 生成最近14天的专注时长数据（每个学生每天1-5次专注学习会话）
-- 用存储过程批量生成
DELIMITER $$
DROP PROCEDURE IF EXISTS generate_focus_data$$
CREATE PROCEDURE generate_focus_data()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE uid BIGINT;
  DECLARE cur CURSOR FOR SELECT id FROM user WHERE class_id = @class_id AND role = 1;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO uid;
    IF done THEN LEAVE read_loop; END IF;

    SET @day_offset = 0;
    WHILE @day_offset < 14 DO
      SET @session_count = FLOOR(1 + RAND() * 5);
      SET @s = 0;
      WHILE @s < @session_count DO
        SET @duration = FLOOR(15 + RAND() * 105);
        SET @start_hour = FLOOR(8 + RAND() * 12);
        SET @start_min = FLOOR(RAND() * 60);
        SET @start_time = DATE_SUB(CURDATE(), INTERVAL @day_offset DAY) + INTERVAL @start_hour HOUR + INTERVAL @start_min MINUTE;
        SET @end_time = @start_time + INTERVAL @duration MINUTE;
        
        INSERT INTO focus_session (user_id, duration_seconds, started_at, finished_at)
        VALUES (uid, @duration * 60, @start_time, @end_time);
        
        SET @s = @s + 1;
      END WHILE;
      SET @day_offset = @day_offset + 1;
    END WHILE;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL generate_focus_data();
DROP PROCEDURE IF EXISTS generate_focus_data;

-- 9. 生成聊天消息数据（每个学生在3门课程中各有若干次提问）
DELIMITER $$
DROP PROCEDURE IF EXISTS generate_chat_data$$
CREATE PROCEDURE generate_chat_data()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE uid BIGINT;
  DECLARE uname VARCHAR(50);
  DECLARE cur CURSOR FOR SELECT id, real_name FROM user WHERE class_id = @class_id AND role = 1;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO uid, uname;
    IF done THEN LEAVE read_loop; END IF;

    -- Java程序设计 提问
    SET @q_count = FLOOR(1 + RAND() * 6);
    SET @i = 0;
    WHILE @i < @q_count DO
      SET @created = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 336) HOUR);
      INSERT INTO chat_message (course_name, user_id, sender_name, sender_role, content, is_read, created_at)
      VALUES ('Java程序设计', uid, uname, 'student', 
        ELT(FLOOR(1 + RAND() * 6), 
          '老师，请问什么是多态？',
          '继承和接口有什么区别？',
          'ArrayList和LinkedList哪个更快？',
          '异常处理中finally一定会执行吗？',
          'String和StringBuilder的区别是什么？',
          '泛型的通配符怎么用？'
        ), 1, @created);
      -- AI回复
      INSERT INTO chat_message (course_name, user_id, sender_name, sender_role, content, is_read, created_at)
      VALUES ('Java程序设计', uid, 'AI助教', 'assistant', '这是AI助教的自动回复内容，详细解答了学生的问题。', 0, @created + INTERVAL 10 SECOND);
      SET @i = @i + 1;
    END WHILE;

    -- 数据库原理 提问
    SET @q_count = FLOOR(1 + RAND() * 5);
    SET @i = 0;
    WHILE @i < @q_count DO
      SET @created = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 336) HOUR);
      INSERT INTO chat_message (course_name, user_id, sender_name, sender_role, content, is_read, created_at)
      VALUES ('数据库原理', uid, uname, 'student', 
        ELT(FLOOR(1 + RAND() * 5), 
          '主键和外键有什么区别？',
          '索引的工作原理是什么？',
          '事务的ACID特性怎么理解？',
          '什么是SQL注入？如何防范？',
          '三范式具体是什么？'
        ), 1, @created);
      INSERT INTO chat_message (course_name, user_id, sender_name, sender_role, content, is_read, created_at)
      VALUES ('数据库原理', uid, 'AI助教', 'assistant', '这是AI助教的自动回复内容，详细解答了学生的问题。', 0, @created + INTERVAL 10 SECOND);
      SET @i = @i + 1;
    END WHILE;

    -- Web前端开发 提问
    SET @q_count = FLOOR(1 + RAND() * 5);
    SET @i = 0;
    WHILE @i < @q_count DO
      SET @created = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 336) HOUR);
      INSERT INTO chat_message (course_name, user_id, sender_name, sender_role, content, is_read, created_at)
      VALUES ('Web前端开发', uid, uname, 'student', 
        ELT(FLOOR(1 + RAND() * 5), 
          'CSS的flex布局怎么用？',
          '闭包是什么意思？',
          'Promise和async/await的区别？',
          'Vue的生命周期有哪些？',
          'DOM操作有哪些常用方法？'
        ), 1, @created);
      INSERT INTO chat_message (course_name, user_id, sender_name, sender_role, content, is_read, created_at)
      VALUES ('Web前端开发', uid, 'AI助教', 'assistant', '这是AI助教的自动回复内容，详细解答了学生的问题。', 0, @created + INTERVAL 10 SECOND);
      SET @i = @i + 1;
    END WHILE;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL generate_chat_data();
DROP PROCEDURE IF EXISTS generate_chat_data;

-- 10. 生成答题记录数据
DELIMITER $$
DROP PROCEDURE IF EXISTS generate_quiz_data$$
CREATE PROCEDURE generate_quiz_data()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE uid BIGINT;
  DECLARE cur CURSOR FOR SELECT id FROM user WHERE class_id = @class_id AND role = 1;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO uid;
    IF done THEN LEAVE read_loop; END IF;

    SET @session_count = FLOOR(2 + RAND() * 6);
    SET @s = 0;
    WHILE @s < @session_count DO
      SET @total = 10 + FLOOR(RAND() * 11);
      SET @correct = FLOOR(RAND() * (@total + 1));
      SET @duration = FLOOR(300 + RAND() * 1500);
      SET @created = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 336) HOUR);
      SET @subj = ELT(FLOOR(1 + RAND() * 3), 'Java程序设计', '数据库原理', 'Web前端开发');
      SET @score = ROUND((@correct / @total) * 100, 1);
      
      INSERT INTO quiz_session (user_id, subject, subject_type, session_no, total_questions, answered_count, correct_count, skip_count, total_duration_sec, scores, status, created_at)
      VALUES (uid, @subj, 'chapter', 1, @total, @total, @correct, 0, @duration, 
        CONCAT('{\"total\":', @score, ',\"knowledge\":', ROUND(70 + RAND() * 30, 1), ',\"skill\":', ROUND(60 + RAND() * 40, 1), '}'),
        'finished', @created);
      SET @s = @s + 1;
    END WHILE;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL generate_quiz_data();
DROP PROCEDURE IF EXISTS generate_quiz_data;

-- ============================================
-- 测试数据生成完成
-- 班级：软件技术2024级1班（30人）
-- 老师：李文博（账号：liwenbo 密码：123456）
-- 课程：Java程序设计、数据库原理、Web前端开发
-- 学期：2025-2026-2
-- ============================================
