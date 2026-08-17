-- ========================================================
-- 自适应出题功能：章节阅读进度 + 课程难度状态 + RAG章节检索
-- ========================================================

USE znxsgltest;
SET NAMES utf8mb4;

-- ----------------------------
-- 章节阅读进度表（学生手动标记完成的课时）
-- ----------------------------
DROP TABLE IF EXISTS `chapter_read_progress`;
CREATE TABLE `chapter_read_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
  `lesson_id` BIGINT NOT NULL COMMENT '课时ID',
  `completed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_lesson` (`user_id`, `lesson_id`),
  KEY `idx_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节阅读进度表';

-- ----------------------------
-- 课程难度状态表（按课程维度维护当前难度档位）
-- ----------------------------
DROP TABLE IF EXISTS `user_course_difficulty`;
CREATE TABLE `user_course_difficulty` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '当前难度档位：1基础/2中等/3进阶',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生课程难度状态表';

-- ----------------------------
-- quiz_session 增加自适应字段
-- ----------------------------
ALTER TABLE `quiz_session` ADD COLUMN `course_id` BIGINT NULL COMMENT '课程ID' AFTER `subject`;
ALTER TABLE `quiz_session` ADD COLUMN `difficulty` TINYINT DEFAULT 1 COMMENT '本次出题难度：1基础/2中等/3进阶' AFTER `course_id`;
ALTER TABLE `quiz_session` ADD COLUMN `chapter_scope` TEXT COMMENT '本次出题章节范围(JSON: chapterId数组)' AFTER `difficulty`;
ALTER TABLE `quiz_session` ADD INDEX `idx_user_course` (`user_id`, `course_id`);

-- ----------------------------
-- document_vector 增加章节关联（RAG按章节检索）
-- ----------------------------
ALTER TABLE `document_vector` ADD COLUMN `chapter_id` BIGINT NULL COMMENT '章节ID(空=课程级文档)' AFTER `course_name`;
ALTER TABLE `document_vector` ADD INDEX `idx_chapter` (`chapter_id`);
