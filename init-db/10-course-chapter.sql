-- ========================================================
-- 课程章节与资源体系表初始化脚本
-- 支持课程-章节-课时三级结构，课时可关联视频、文档、测验、链接等资源
-- ========================================================

USE znxsgltest;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 课程章节表
-- ----------------------------
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
  `chapter_no` INT NOT NULL DEFAULT 1 COMMENT '章节序号，如第1章、第2章',
  `chapter_name` VARCHAR(200) NOT NULL COMMENT '章节名称',
  `description` TEXT COMMENT '章节描述',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（配合 MyBatis-Plus）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程章节表';

-- ----------------------------
-- 课程课时/资源表
-- ----------------------------
DROP TABLE IF EXISTS `course_lesson`;
CREATE TABLE `course_lesson` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课时ID',
  `chapter_id` BIGINT NOT NULL COMMENT '所属章节ID',
  `lesson_no` INT NOT NULL DEFAULT 1 COMMENT '课时序号，如第1节、第2节',
  `lesson_name` VARCHAR(200) NOT NULL COMMENT '课时名称',
  `resource_type` VARCHAR(20) NOT NULL DEFAULT 'video' COMMENT '资源类型：video视频/document文档/quiz测验/link链接',
  `resource_url` VARCHAR(500) DEFAULT NULL COMMENT '资源URL（视频地址、文档地址、链接等）',
  `duration` INT DEFAULT NULL COMMENT '视频/音频时长（秒）',
  `content` TEXT COMMENT '文本内容或富文本（测验题目、文档正文等）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（配合 MyBatis-Plus）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程课时与资源表';

SET FOREIGN_KEY_CHECKS = 1;
