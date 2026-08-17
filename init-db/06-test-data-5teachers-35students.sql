-- =============================================
-- 测试数据生成脚本
-- 5名教师（语文、数学、英语 + 2门专业课）
-- 1个班级：软件技术2024级1班（35名学生）
-- 当前学期：2025-2026-2
-- =============================================

-- 选择数据库
USE znxsgltest;

-- 设置字符集和排序规则，避免跨表比较报错
SET NAMES utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- =============================================
-- 清理旧测试数据（仅清理本次生成的测试数据）
-- =============================================
DELETE FROM schedule WHERE course_name IN ('大学语文', '高等数学', '大学英语', 'Java程序设计', '数据库原理');
SET @course_ids = (SELECT GROUP_CONCAT(id) FROM course WHERE course_name IN ('大学语文', '高等数学', '大学英语', 'Java程序设计', '数据库原理'));
DELETE FROM course_class WHERE FIND_IN_SET(course_id, @course_ids);
DELETE FROM course WHERE course_name IN ('大学语文', '高等数学', '大学英语', 'Java程序设计', '数据库原理');
DELETE FROM user WHERE username LIKE 'stu2024%';
DELETE FROM user WHERE username IN ('zhangyq', 'lijg', 'wangml', 'chenzq', 'liuxy');
DELETE FROM teacher WHERE real_name IN ('张雅琴', '李建国', '王美玲', '陈志强', '刘晓燕');
DELETE FROM class_info WHERE class_name = '软件技术2024级1班';

-- =============================================
-- 1. 创建班级
-- =============================================
INSERT INTO class_info (class_name, grade, major, department)
VALUES ('软件技术2024级1班', '2024', '软件技术', '计算机系');
SET @class_id = LAST_INSERT_ID();

-- =============================================
-- 2. 创建5名教师
-- =============================================
-- 教师1：语文老师 - 张雅琴
INSERT INTO teacher (teacher_no, real_name, gender, title, email, phone, status)
VALUES ('T202001', '张雅琴', 2, '讲师', 'zhangyq@example.com', '13800000001', 1);
SET @teacher_chinese = LAST_INSERT_ID();
INSERT INTO user (username, password_hash, real_name, role, phone, email, status)
VALUES ('zhangyq', 'e10adc3949ba59abbe56e057f20f883e', '张雅琴', 2, '13800000001', 'zhangyq@example.com', 1);

-- 教师2：数学老师 - 李建国
INSERT INTO teacher (teacher_no, real_name, gender, title, email, phone, status)
VALUES ('T202002', '李建国', 1, '副教授', 'lijg@example.com', '13800000002', 1);
SET @teacher_math = LAST_INSERT_ID();
INSERT INTO user (username, password_hash, real_name, role, phone, email, status)
VALUES ('lijg', 'e10adc3949ba59abbe56e057f20f883e', '李建国', 2, '13800000002', 'lijg@example.com', 1);

-- 教师3：英语老师 - 王美玲
INSERT INTO teacher (teacher_no, real_name, gender, title, email, phone, status)
VALUES ('T202003', '王美玲', 2, '讲师', 'wangml@example.com', '13800000003', 1);
SET @teacher_english = LAST_INSERT_ID();
INSERT INTO user (username, password_hash, real_name, role, phone, email, status)
VALUES ('wangml', 'e10adc3949ba59abbe56e057f20f883e', '王美玲', 2, '13800000003', 'wangml@example.com', 1);

-- 教师4：专业课老师1 - 陈志强（Java程序设计）
INSERT INTO teacher (teacher_no, real_name, gender, title, email, phone, status, dept_id)
VALUES ('T202004', '陈志强', 1, '副教授', 'chenzq@example.com', '13800000004', 1, 1);
SET @teacher_java = LAST_INSERT_ID();
INSERT INTO user (username, password_hash, real_name, role, phone, email, status)
VALUES ('chenzq', 'e10adc3949ba59abbe56e057f20f883e', '陈志强', 2, '13800000004', 'chenzq@example.com', 1);

-- 教师5：专业课老师2 - 刘晓燕（数据库原理）
INSERT INTO teacher (teacher_no, real_name, gender, title, email, phone, status, dept_id)
VALUES ('T202005', '刘晓燕', 2, '讲师', 'liuxy@example.com', '13800000005', 1, 1);
SET @teacher_db = LAST_INSERT_ID();
INSERT INTO user (username, password_hash, real_name, role, phone, email, status)
VALUES ('liuxy', 'e10adc3949ba59abbe56e057f20f883e', '刘晓燕', 2, '13800000005', 'liuxy@example.com', 1);

-- =============================================
-- 3. 创建35名学生
-- =============================================
DROP PROCEDURE IF EXISTS create_students;
DELIMITER //
CREATE PROCEDURE create_students()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE username VARCHAR(50);
    DECLARE student_no VARCHAR(50);
    DECLARE realname VARCHAR(50);
    DECLARE surnames VARCHAR(200) DEFAULT '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜';
    DECLARE names VARCHAR(200) DEFAULT '伟芳娜敏静丽强磊军洋勇艳杰涛明超秀兰霞平刚桂英';
    DECLARE surname CHAR(1);
    DECLARE name_char1 CHAR(1);
    DECLARE name_char2 CHAR(1);

    WHILE i <= 35 DO
        SET username = CONCAT('stu2024', LPAD(i, 3, '0'));
        SET student_no = CONCAT('2024', LPAD(i, 4, '0'));
        -- 生成随机姓名
        SET surname = SUBSTRING(surnames, FLOOR(RAND() * CHAR_LENGTH(surnames)) + 1, 1);
        SET name_char1 = SUBSTRING(names, FLOOR(RAND() * CHAR_LENGTH(names)) + 1, 1);
        SET name_char2 = SUBSTRING(names, FLOOR(RAND() * CHAR_LENGTH(names)) + 1, 1);
        SET realname = CONCAT(surname, name_char1, name_char2);

        INSERT INTO user (username, password_hash, student_no, real_name, role, class_id, major, grade, phone, email, status)
        VALUES (
            username,
            'e10adc3949ba59abbe56e057f20f883e',
            student_no,
            realname,
            1,
            @class_id,
            '软件技术',
            '2024',
            CONCAT('139', LPAD(FLOOR(RAND() * 100000000), 8, '0')),
            CONCAT(username, '@example.com'),
            1
        );

        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;
CALL create_students();
DROP PROCEDURE IF EXISTS create_students;

-- =============================================
-- 4. 创建5门课程
-- =============================================
SET @semester = '2025-2026-2';

-- 大学语文（张雅琴，64课时）
INSERT INTO course (course_name, teacher_id, course_type, description, credit, semester)
VALUES ('大学语文', @teacher_chinese, '公共基础课', '培养学生的语言文字运用能力和文学素养', 64.0, @semester);
SET @course_chinese = LAST_INSERT_ID();
INSERT INTO course_class (course_id, class_id, semester)
VALUES (@course_chinese, @class_id, @semester);

-- 高等数学（李建国，80课时）
INSERT INTO course (course_name, teacher_id, course_type, description, credit, semester)
VALUES ('高等数学', @teacher_math, '公共基础课', '学习微积分、线性代数等数学基础知识', 80.0, @semester);
SET @course_math = LAST_INSERT_ID();
INSERT INTO course_class (course_id, class_id, semester)
VALUES (@course_math, @class_id, @semester);

-- 大学英语（王美玲，64课时）
INSERT INTO course (course_name, teacher_id, course_type, description, credit, semester)
VALUES ('大学英语', @teacher_english, '公共基础课', '培养学生的英语听说读写综合能力', 64.0, @semester);
SET @course_english = LAST_INSERT_ID();
INSERT INTO course_class (course_id, class_id, semester)
VALUES (@course_english, @class_id, @semester);

-- Java程序设计（陈志强，72课时）
INSERT INTO course (course_name, teacher_id, course_type, description, credit, semester)
VALUES ('Java程序设计', @teacher_java, '专业核心课', '学习Java编程语言和面向对象编程思想', 72.0, @semester);
SET @course_java = LAST_INSERT_ID();
INSERT INTO course_class (course_id, class_id, semester)
VALUES (@course_java, @class_id, @semester);

-- 数据库原理（刘晓燕，56课时）
INSERT INTO course (course_name, teacher_id, course_type, description, credit, semester)
VALUES ('数据库原理', @teacher_db, '专业核心课', '学习数据库系统原理和SQL语言', 56.0, @semester);
SET @course_db = LAST_INSERT_ID();
INSERT INTO course_class (course_id, class_id, semester)
VALUES (@course_db, @class_id, @semester);

-- =============================================
-- 5. 为班级所有学生创建已上架的课表记录（已排课状态）
-- =============================================
-- 课表安排（周1-周5，每天10节课/5大节）：
-- 周一 1-2节: 高等数学
-- 周一 3-4节: Java程序设计
-- 周二 1-2节: 大学英语
-- 周二 5-6节: 数据库原理
-- 周三 1-2节: 大学语文
-- 周三 3-4节: 高等数学
-- 周四 1-2节: 大学英语
-- 周四 3-4节: Java程序设计
-- 周五 1-2节: 大学语文
-- 周五 3-4节: 数据库原理

-- 先删除该班级已有排课
DELETE FROM schedule
WHERE semester = @semester AND status = 1
  AND user_id IN (SELECT id FROM user WHERE class_id = @class_id AND role = 1);

-- 创建课表存储过程
DROP PROCEDURE IF EXISTS create_schedules;
DELIMITER //
CREATE PROCEDURE create_schedules()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE student_id BIGINT;
    DECLARE cur CURSOR FOR SELECT id FROM user WHERE class_id = @class_id AND role = 1;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO student_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 周一 1-2节: 高等数学
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_math, '高等数学', 1, '08:00', '09:40', 1, 2, 'A101', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周一 3-4节: Java程序设计
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_java, 'Java程序设计', 1, '10:00', '11:40', 3, 2, 'B201', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周二 1-2节: 大学英语
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_english, '大学英语', 2, '08:00', '09:40', 1, 2, 'A102', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周二 5-6节: 数据库原理
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_db, '数据库原理', 2, '14:00', '15:40', 5, 2, 'B202', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周三 1-2节: 大学语文
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_chinese, '大学语文', 3, '08:00', '09:40', 1, 2, 'A103', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周三 3-4节: 高等数学
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_math, '高等数学', 3, '10:00', '11:40', 3, 2, 'A101', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周四 1-2节: 大学英语
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_english, '大学英语', 4, '08:00', '09:40', 1, 2, 'A102', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周四 3-4节: Java程序设计
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_java, 'Java程序设计', 4, '10:00', '11:40', 3, 2, 'B201', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周五 1-2节: 大学语文
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_chinese, '大学语文', 5, '08:00', '09:40', 1, 2, 'A103', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

        -- 周五 3-4节: 数据库原理
        INSERT INTO schedule (user_id, course_id, course_name, day_of_week, start_time, end_time, start_node, step, classroom, semester, weeks, status)
        VALUES (student_id, @course_db, '数据库原理', 5, '10:00', '11:40', 3, 2, 'B202', @semester, '[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]', 1);

    END LOOP;

    CLOSE cur;
END //
DELIMITER ;
CALL create_schedules();
DROP PROCEDURE IF EXISTS create_schedules;

-- =============================================
-- 6. 验证数据
-- =============================================
SELECT '班级数' AS type, COUNT(*) AS count FROM class_info WHERE class_name = '软件技术2024级1班'
UNION ALL
SELECT '教师数', COUNT(*) FROM teacher WHERE real_name IN ('张雅琴', '李建国', '王美玲', '陈志强', '刘晓燕')
UNION ALL
SELECT '学生数', COUNT(*) FROM user WHERE username LIKE 'stu2024%' AND role = 1
UNION ALL
SELECT '课程数', COUNT(*) FROM course WHERE course_name IN ('大学语文', '高等数学', '大学英语', 'Java程序设计', '数据库原理')
UNION ALL
SELECT '课表记录数', COUNT(*) FROM schedule WHERE semester = '2025-2026-2';
