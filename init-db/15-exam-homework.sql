-- ============================================================
-- 教师发布考试/作业功能
-- 包含：考试作业主表、题目表、学生作答记录、学生每题作答
-- ============================================================

USE znxsgltest;
SET NAMES utf8mb4;

-- ----------------------------
-- 考试作业主表
-- ----------------------------
DROP TABLE IF EXISTS `exam_submission_answer`;
DROP TABLE IF EXISTS `exam_submission`;
DROP TABLE IF EXISTS `exam_question`;
DROP TABLE IF EXISTS `exam_homework`;

CREATE TABLE `exam_homework` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(20) NOT NULL COMMENT 'exam考试 / homework作业',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `description` TEXT COMMENT '描述',
  `course_id` BIGINT NULL COMMENT '关联课程ID（可选）',
  `class_id` BIGINT NOT NULL COMMENT '目标班级ID',
  `teacher_id` BIGINT NOT NULL COMMENT '发布教师ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '截止时间',
  `time_limit` INT DEFAULT 0 COMMENT '限时分钟，0=不限时（作业）',
  `total_score` INT NOT NULL COMMENT '总分',
  `pass_score` INT NOT NULL COMMENT '及格分',
  `publish_mode` VARCHAR(20) DEFAULT 'immediate' COMMENT 'immediate立即发布 / scheduled定时发布',
  `scheduled_time` DATETIME NULL COMMENT '定时发布时间',
  `question_mode` VARCHAR(20) NULL COMMENT 'ai-range按范围 / ai-document按文档',
  `question_types` JSON NULL COMMENT '题型配置数组',
  `difficulty` VARCHAR(20) NULL COMMENT '难度',
  `question_count` INT NOT NULL COMMENT '题目数量',
  `status` TINYINT DEFAULT 0 COMMENT '0草稿 1进行中 2已结束 3已下架',
  `edit_count` TINYINT DEFAULT 0 COMMENT '教师修改次数，最多2次',
  `max_edit_count` TINYINT DEFAULT 2 COMMENT '最大可修改次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_class_time` (`class_id`, `start_time`, `end_time`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_status_time` (`status`, `scheduled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师发布的考试/作业';

-- ----------------------------
-- 题目表
-- ----------------------------
CREATE TABLE `exam_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_homework_id` BIGINT NOT NULL COMMENT '所属考试作业ID',
  `question_index` INT NOT NULL COMMENT '题号 1-N',
  `question_type` VARCHAR(20) NOT NULL COMMENT 'single_choice/multiple_choice/true_false/fill_blank/short_answer',
  `content` TEXT NOT NULL COMMENT '题目内容',
  `options` JSON NULL COMMENT '选项列表',
  `answer` TEXT NULL COMMENT '参考答案',
  `score` INT NOT NULL DEFAULT 0 COMMENT '分值',
  `difficulty` VARCHAR(20) NULL COMMENT '难度',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exam` (`exam_homework_id`),
  CONSTRAINT `fk_eq_exam` FOREIGN KEY (`exam_homework_id`) REFERENCES `exam_homework`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试作业题目';

-- ----------------------------
-- 学生作答记录
-- ----------------------------
CREATE TABLE `exam_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_homework_id` BIGINT NOT NULL COMMENT '考试作业ID',
  `user_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending进行中 / completed已完成',
  `total_score` INT DEFAULT 0 COMMENT '总得分',
  `duration_sec` INT DEFAULT 0 COMMENT '用时秒',
  `started_at` DATETIME NULL COMMENT '开始时间',
  `submitted_at` DATETIME NULL COMMENT '提交时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_user` (`exam_homework_id`, `user_id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `fk_sub_exam` FOREIGN KEY (`exam_homework_id`) REFERENCES `exam_homework`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sub_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生考试作业作答记录';

-- ----------------------------
-- 学生每题作答
-- ----------------------------
CREATE TABLE `exam_submission_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT NOT NULL COMMENT '作答记录ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `question_index` INT NOT NULL COMMENT '题号',
  `question_type` VARCHAR(20) NULL COMMENT '题型',
  `question` TEXT NULL COMMENT '题目内容快照',
  `options` JSON NULL COMMENT '选项快照',
  `user_answer` TEXT NULL COMMENT '学生答案',
  `correct_answer` TEXT NULL COMMENT '正确答案快照',
  `is_correct` TINYINT DEFAULT 0 COMMENT '1对 0错 -1不会 -2跳过',
  `score` INT DEFAULT 0 COMMENT '本题得分',
  `ai_score` INT DEFAULT NULL COMMENT 'AI评分（简答题）',
  `ai_comment` TEXT NULL COMMENT 'AI评语（简答题）',
  `duration_sec` INT DEFAULT 0 COMMENT '本题耗时秒',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submission_id`),
  KEY `idx_question` (`question_id`),
  CONSTRAINT `fk_esa_submission` FOREIGN KEY (`submission_id`) REFERENCES `exam_submission`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_esa_question` FOREIGN KEY (`question_id`) REFERENCES `exam_question`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生每题作答详情';
