/*
 Navicat Premium Dump SQL

 Source Server         : lsrt
 Source Server Type    : MySQL
 Source Server Version : 80028 (8.0.28)
 Source Host           : localhost:3306
 Source Schema         : znxsgltest

 Target Server Type    : MySQL
 Target Server Version : 80028 (8.0.28)
 File Encoding         : 65001

 Date: 17/08/2026 20:58:53
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chapter_read_progress
-- ----------------------------
DROP TABLE IF EXISTS `chapter_read_progress`;
CREATE TABLE `chapter_read_progress`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学生用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `lesson_id` bigint NOT NULL COMMENT '课时ID',
  `completed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_lesson`(`user_id` ASC, `lesson_id` ASC) USING BTREE,
  INDEX `idx_user_course`(`user_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '章节阅读进度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名',
  `user_id` bigint NOT NULL COMMENT '发送者用户ID',
  `sender_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '发送者姓名',
  `sender_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'student/teacher/ai',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `mention_user_id` bigint NULL DEFAULT NULL,
  `biz_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型：exam_publish/作业发布等普通聊天为NULL',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID：对应 exam_homework.id 等',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_biz`(`user_id` ASC, `biz_type` ASC, `biz_id` ASC) USING BTREE,
  INDEX `idx_course`(`course_name` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_course_time`(`course_name` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_course_name`(`course_name` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  CONSTRAINT `chat_message_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4941 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程聊天消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for check_in
-- ----------------------------
DROP TABLE IF EXISTS `check_in`;
CREATE TABLE `check_in`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `creator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `password` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime NULL DEFAULT NULL,
  `active` int NULL DEFAULT 1,
  `deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for check_in_record
-- ----------------------------
DROP TABLE IF EXISTS `check_in_record`;
CREATE TABLE `check_in_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_in_id` bigint NOT NULL,
  `student_id` bigint NULL DEFAULT NULL,
  `student_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `checked_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for checkin_record
-- ----------------------------
DROP TABLE IF EXISTS `checkin_record`;
CREATE TABLE `checkin_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `checkin_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际签到方式',
  `location` json NULL COMMENT '签到位置',
  `checkin_time` datetime NOT NULL,
  `status` tinyint NULL DEFAULT 1 COMMENT '0缺勤 1正常 2迟到 3补签',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_user`(`task_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `checkin_record_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `checkin_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `checkin_record_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for checkin_task
-- ----------------------------
DROP TABLE IF EXISTS `checkin_task`;
CREATE TABLE `checkin_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `course_id` bigint NULL DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkin_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'gps/qrcode/face/beacon',
  `location` json NULL COMMENT '位置信息{\"lat\":xx,\"lng\":xx,\"radius\":200}',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `course_id`(`course_id` ASC) USING BTREE,
  CONSTRAINT `checkin_task_ibfk_1` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `checkin_task_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for class_info
-- ----------------------------
DROP TABLE IF EXISTS `class_info`;
CREATE TABLE `class_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业',
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '院系',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '年级',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '班级信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for classroom
-- ----------------------------
DROP TABLE IF EXISTS `classroom`;
CREATE TABLE `classroom`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教室名称，如\"实训楼A301\"',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：normal=普通教室, lab=专业实训室, computer=计算机机房, music=琴房, dance=舞蹈室, art=美术室, sports=运动场',
  `capacity` int NULL DEFAULT 50 COMMENT '容纳人数',
  `building` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所在楼',
  `floor` int NULL DEFAULT NULL COMMENT '楼层',
  `equipment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '设备清单（JSON数组或逗号分隔）',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教室资源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名称',
  `course_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程编号',
  `teacher_id` bigint NULL DEFAULT NULL COMMENT '授课教师ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '开课院系',
  `semester` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期 如2025-2026-2',
  `course_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '必修' COMMENT '必修/选修',
  `credit` decimal(6, 1) NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程描述',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_semester`(`semester` ASC) USING BTREE,
  INDEX `idx_course_teacher`(`teacher_id` ASC) USING BTREE,
  INDEX `dept_id`(`dept_id` ASC) USING BTREE,
  CONSTRAINT `course_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `course_ibfk_2` FOREIGN KEY (`dept_id`) REFERENCES `department` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 352 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_chapter
-- ----------------------------
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `course_id` bigint NOT NULL COMMENT '所属课程ID',
  `chapter_no` int NOT NULL DEFAULT 1 COMMENT '章节序号，如第1章、第2章',
  `chapter_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '章节名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '章节描述',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（配合 MyBatis-Plus）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 117 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程章节表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_class
-- ----------------------------
DROP TABLE IF EXISTS `course_class`;
CREATE TABLE `course_class`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `semester` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_class`(`course_id` ASC, `class_id` ASC, `semester` ASC) USING BTREE,
  INDEX `class_id`(`class_id` ASC) USING BTREE,
  CONSTRAINT `course_class_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `course_class_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `class_info` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 635 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程-班级关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_import_record
-- ----------------------------
DROP TABLE IF EXISTS `course_import_record`;
CREATE TABLE `course_import_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '导入时上传的文件名',
  `imported_by` bigint NULL DEFAULT NULL COMMENT '导入人 user_id',
  `imported_by_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '导入人姓名',
  `semester` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学期',
  `total_count` int NULL DEFAULT 0 COMMENT '本次导入总条数',
  `success_count` int NULL DEFAULT 0 COMMENT '成功导入条数',
  `skip_count` int NULL DEFAULT 0 COMMENT '跳过条数',
  `messages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '导入结果消息（JSON 数组字符串）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_imported_by`(`imported_by` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程导入记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_lesson
-- ----------------------------
DROP TABLE IF EXISTS `course_lesson`;
CREATE TABLE `course_lesson`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课时ID',
  `chapter_id` bigint NOT NULL COMMENT '所属章节ID',
  `lesson_no` int NOT NULL DEFAULT 1 COMMENT '课时序号，如第1节、第2节',
  `lesson_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '课时名称',
  `resource_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'video' COMMENT '资源类型：video视频/document文档/quiz测验/link链接',
  `resource_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源URL（视频地址、文档地址、链接等）',
  `duration` int NULL DEFAULT NULL COMMENT '视频/音频时长（秒）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文本内容或富文本（测验题目、文档正文等）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（配合 MyBatis-Plus）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 144 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程课时与资源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '院系名称',
  `dept_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '院系编码',
  `dept_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教学单位/行政单位',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '院系部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程，可为空',
  `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '阿里云OSS路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '字节',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'pdf/docx/ppt/jpg/png',
  `doc_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '其他' COMMENT '课件/笔记/试卷/参考书/其他',
  `ocr_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'OCR提取文本',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI摘要',
  `status` tinyint NULL DEFAULT 0 COMMENT '0审核中 1已发布 2驳回',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_doc_user_category`(`user_id` ASC, `doc_category` ASC) USING BTREE,
  INDEX `idx_doc_user_course`(`user_id` ASC, `course_id` ASC) USING BTREE,
  INDEX `idx_doc_status`(`status` ASC) USING BTREE,
  INDEX `course_id`(`course_id` ASC) USING BTREE,
  CONSTRAINT `document_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `document_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for document_chunk
-- ----------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `document_id` bigint NOT NULL,
  `chunk_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文本分块内容',
  `vector_id` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '向量库(阿里云DashVector)中的ID',
  `chunk_index` int NOT NULL COMMENT '分块序号',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chunk_doc`(`document_id` ASC) USING BTREE,
  CONSTRAINT `document_chunk_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档分块表(向量检索)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for document_vector
-- ----------------------------
DROP TABLE IF EXISTS `document_vector`;
CREATE TABLE `document_vector`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '章节ID(空=课程级文档)',
  `doc_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '文档名称',
  `content_chunk` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文本块内容',
  `embedding` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '向量嵌入(JSON数组，支持1024维以上)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course`(`course_name` ASC) USING BTREE,
  INDEX `idx_course_content`(`course_name` ASC, `content_chunk`(100) ASC) USING BTREE,
  INDEX `idx_chapter`(`chapter_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 97 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_homework
-- ----------------------------
DROP TABLE IF EXISTS `exam_homework`;
CREATE TABLE `exam_homework`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'exam考试 / homework作业',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '描述',
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程ID（可选）',
  `class_id` bigint NOT NULL COMMENT '目标班级ID',
  `teacher_id` bigint NOT NULL COMMENT '发布教师ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '截止时间',
  `time_limit` int NULL DEFAULT 0 COMMENT '限时分钟，0=不限时（作业）',
  `total_score` int NOT NULL COMMENT '总分',
  `pass_score` int NOT NULL COMMENT '及格分',
  `publish_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'immediate' COMMENT 'immediate立即发布 / scheduled定时发布',
  `scheduled_time` datetime NULL DEFAULT NULL COMMENT '定时发布时间',
  `question_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ai-range按范围 / ai-document按文档',
  `question_types` json NULL COMMENT '题型配置数组',
  `difficulty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '难度',
  `question_count` int NOT NULL COMMENT '题目数量',
  `status` tinyint NULL DEFAULT 0 COMMENT '0草稿 1进行中 2已结束 3已下架',
  `edit_count` tinyint NULL DEFAULT 0 COMMENT '教师修改次数，最多2次',
  `max_edit_count` tinyint NULL DEFAULT 2 COMMENT '最大可修改次数',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_class_time`(`class_id` ASC, `start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_teacher`(`teacher_id` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `scheduled_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '教师发布的考试/作业' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_question
-- ----------------------------
DROP TABLE IF EXISTS `exam_question`;
CREATE TABLE `exam_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_homework_id` bigint NOT NULL COMMENT '所属考试作业ID',
  `question_index` int NOT NULL COMMENT '题号 1-N',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'single_choice/multiple_choice/true_false/fill_blank/short_answer',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题目内容',
  `options` json NULL COMMENT '选项列表',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参考答案',
  `score` int NOT NULL DEFAULT 0 COMMENT '分值',
  `difficulty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '难度',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exam`(`exam_homework_id` ASC) USING BTREE,
  CONSTRAINT `fk_eq_exam` FOREIGN KEY (`exam_homework_id`) REFERENCES `exam_homework` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 134 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考试作业题目' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_submission
-- ----------------------------
DROP TABLE IF EXISTS `exam_submission`;
CREATE TABLE `exam_submission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_homework_id` bigint NOT NULL COMMENT '考试作业ID',
  `user_id` bigint NOT NULL COMMENT '学生用户ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT 'pending进行中 / completed已完成',
  `total_score` int NULL DEFAULT 0 COMMENT '总得分',
  `auto_score` int NULL DEFAULT NULL COMMENT 'AI自动总评分（教师调整前分数）',
  `duration_sec` int NULL DEFAULT 0 COMMENT '用时秒',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `submitted_at` datetime NULL DEFAULT NULL COMMENT '提交时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_user`(`exam_homework_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_sub_exam` FOREIGN KEY (`exam_homework_id`) REFERENCES `exam_homework` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_sub_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生考试作业作答记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_submission_answer
-- ----------------------------
DROP TABLE IF EXISTS `exam_submission_answer`;
CREATE TABLE `exam_submission_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL COMMENT '作答记录ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `question_index` int NOT NULL DEFAULT 0 COMMENT '题号 1-N',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '题型',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '题目内容快照',
  `options` json NULL COMMENT '选项快照',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '学生答案',
  `correct_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正确答案快照',
  `is_correct` tinyint NULL DEFAULT 0 COMMENT '1对 0错 -1不会 -2跳过',
  `score` int NULL DEFAULT 0 COMMENT '本题得分',
  `max_score` int NULL DEFAULT NULL COMMENT '本题满分快照',
  `ai_score` int NULL DEFAULT NULL COMMENT 'AI评分（简答题）',
  `ai_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI评语（简答题）',
  `teacher_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '教师评分评语',
  `score_adjust_count` tinyint NULL DEFAULT 0 COMMENT '教师调整次数，限2次',
  `duration_sec` int NULL DEFAULT 0 COMMENT '本题耗时秒',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission`(`submission_id` ASC) USING BTREE,
  INDEX `idx_question`(`question_id` ASC) USING BTREE,
  CONSTRAINT `fk_esa_question` FOREIGN KEY (`question_id`) REFERENCES `exam_question` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_esa_submission` FOREIGN KEY (`submission_id`) REFERENCES `exam_submission` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生每题作答详情' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for focus_session
-- ----------------------------
DROP TABLE IF EXISTS `focus_session`;
CREATE TABLE `focus_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `duration_seconds` int NULL DEFAULT 0,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `focus_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1643 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for learning_goal
-- ----------------------------
DROP TABLE IF EXISTS `learning_goal`;
CREATE TABLE `learning_goal`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `goal_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `goal_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '考试/考证/技能/其他',
  `target_date` date NULL DEFAULT NULL,
  `progress` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `learning_goal_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习目标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for learning_plan
-- ----------------------------
DROP TABLE IF EXISTS `learning_plan`;
CREATE TABLE `learning_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程，可为空',
  `plan_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '计划详细内容',
  `start_date` date NULL DEFAULT NULL,
  `end_date` date NULL DEFAULT NULL,
  `progress` int NULL DEFAULT 0 COMMENT '进度百分比 0-100',
  `status` tinyint NULL DEFAULT 0 COMMENT '0待开始 1进行中 2已完成 3已放弃',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plan_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `course_id`(`course_id` ASC) USING BTREE,
  CONSTRAINT `learning_plan_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `learning_plan_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL COMMENT '发布者(user.id, role=3)',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'all/class/major/user',
  `target_ids` json NULL COMMENT '目标ID列表',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '普通' COMMENT '紧急/重要/普通',
  `send_time` datetime NULL DEFAULT NULL COMMENT '定时发送时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notif_time`(`created_at` ASC) USING BTREE,
  INDEX `admin_id`(`admin_id` ASC) USING BTREE,
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification_receipt
-- ----------------------------
DROP TABLE IF EXISTS `notification_receipt`;
CREATE TABLE `notification_receipt`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `is_read` tinyint NULL DEFAULT 0 COMMENT '0未读 1已读',
  `read_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notif_user`(`notification_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_receipt_user_read`(`user_id` ASC, `is_read` ASC) USING BTREE,
  CONSTRAINT `notification_receipt_ibfk_1` FOREIGN KEY (`notification_id`) REFERENCES `notification` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `notification_receipt_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知接收状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qa_message
-- ----------------------------
DROP TABLE IF EXISTS `qa_message`;
CREATE TABLE `qa_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID，同一会话消息聚合',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `answer` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_chunks` json NULL COMMENT '引用的文档分块ID，如[12,45,78]',
  `context_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '综合' COMMENT 'schedule/doc/plan/focus/综合',
  `feedback` tinyint NULL DEFAULT 0 COMMENT '0无 1赞 2踩',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_qa_user_time`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_qa_session`(`session_id` ASC) USING BTREE,
  CONSTRAINT `qa_message_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI问答消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question_bookmark
-- ----------------------------
DROP TABLE IF EXISTS `question_bookmark`;
CREATE TABLE `question_bookmark`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `subject` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `correct_answer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `knowledge` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `error_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `improve` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quiz_answer
-- ----------------------------
DROP TABLE IF EXISTS `quiz_answer`;
CREATE TABLE `quiz_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `question_index` int NULL DEFAULT NULL COMMENT '题号 1-N',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单选/判断/解析/填空',
  `subject` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '题目',
  `options` json NULL COMMENT '选项列表',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '学生答案',
  `correct_answer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '正确答案',
  `is_correct` tinyint NULL DEFAULT NULL COMMENT '1对 0错 -1不会 -2跳过',
  `duration_sec` int NULL DEFAULT 0 COMMENT '本题耗时秒',
  `modified_count` int NULL DEFAULT 0 COMMENT '修改次数',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `understood` tinyint NULL DEFAULT 0 COMMENT '0未标记 1已明白',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session`(`session_id` ASC) USING BTREE,
  CONSTRAINT `quiz_answer_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `quiz_session` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 356 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quiz_session
-- ----------------------------
DROP TABLE IF EXISTS `quiz_session`;
CREATE TABLE `quiz_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `subject` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '答题科目',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `difficulty` tinyint NULL DEFAULT 1 COMMENT '本次出题难度：1基础/2中等/3进阶',
  `chapter_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '本次出题章节范围(JSON: chapterId数组)',
  `subject_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业/公共',
  `session_no` int NULL DEFAULT 1 COMMENT '用户第几次答题',
  `total_questions` int NULL DEFAULT NULL COMMENT '总题数',
  `answered_count` int NULL DEFAULT 0 COMMENT '已答题数',
  `correct_count` int NULL DEFAULT 0 COMMENT '正确数（客观题）',
  `skip_count` int NULL DEFAULT 0 COMMENT '跳过/不会',
  `total_duration_sec` int NULL DEFAULT 0 COMMENT '总耗时秒',
  `scores` json NULL COMMENT '六维评分',
  `strengths` json NULL COMMENT '长处',
  `weaknesses` json NULL COMMENT '短处',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI学习建议',
  `study_plan` json NULL COMMENT '学习计划',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT 'pending/completed/evaluated',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_user_course`(`user_id` ASC, `course_id` ASC) USING BTREE,
  CONSTRAINT `quiz_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 179 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NULL DEFAULT NULL,
  `course_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程名称',
  `day_of_week` int NOT NULL COMMENT '星期 1-7',
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `start_node` int NOT NULL COMMENT '开始节次 1-13',
  `step` int NOT NULL COMMENT '持续节数',
  `classroom` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教室',
  `semester` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期',
  `weeks` json NOT NULL COMMENT '上课周次，如[1,2,3,4,5,6,7,8]',
  `status` tinyint NULL DEFAULT 1 COMMENT '1=正常 0=已下架',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_schedule_user_day`(`user_id` ASC, `day_of_week` ASC) USING BTREE,
  INDEX `idx_schedule_user_semester`(`user_id` ASC, `semester` ASC) USING BTREE,
  INDEX `course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_schedule_course_name`(`course_name` ASC) USING BTREE,
  CONSTRAINT `schedule_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `schedule_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 462913 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课表/排课表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedule_backup_20260726
-- ----------------------------
DROP TABLE IF EXISTS `schedule_backup_20260726`;
CREATE TABLE `schedule_backup_20260726`  (
  `id` bigint NOT NULL DEFAULT 0,
  `user_id` bigint NOT NULL,
  `course_id` bigint NULL DEFAULT NULL,
  `course_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程名称',
  `day_of_week` int NOT NULL COMMENT '星期 1-7',
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `start_node` int NOT NULL COMMENT '开始节次 1-13',
  `step` int NOT NULL COMMENT '持续节数',
  `classroom` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教室',
  `semester` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期',
  `weeks` json NOT NULL COMMENT '上课周次，如[1,2,3,4,5,6,7,8]',
  `status` tinyint NULL DEFAULT 1 COMMENT '1=正常 0=已下架',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedule_lock
-- ----------------------------
DROP TABLE IF EXISTS `schedule_lock`;
CREATE TABLE `schedule_lock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL COMMENT '对应 schedule 表记录ID',
  `semester` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `locked_by` bigint NOT NULL COMMENT '锁定人 user_id（教师）',
  `locked_by_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '锁定人姓名',
  `reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '锁定原因（调课说明）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_schedule`(`schedule_id` ASC) USING BTREE,
  INDEX `idx_semester`(`semester` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 885 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课表锁定表（教师调课后不被自动排课覆盖）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedule_rule
-- ----------------------------
DROP TABLE IF EXISTS `schedule_rule`;
CREATE TABLE `schedule_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程类型：public_basic=公共基础课, professional_core=专业核心课, skill=专业技能课, art=艺术实践课',
  `subject` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '具体科目（空=通用规则）',
  `preferred_period` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '建议时段：morning/afternoon/any',
  `preferred_room_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '建议教室类型',
  `consecutive` int NULL DEFAULT NULL COMMENT '建议连堂节数',
  `priority_boost` int NULL DEFAULT 0 COMMENT '优先级加成（负数=更先排）',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规则说明',
  `is_active` tinyint(1) NULL DEFAULT 1,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '排课规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for semester
-- ----------------------------
DROP TABLE IF EXISTS `semester`;
CREATE TABLE `semester`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期名称，如 2026-2027-1',
  `start_date` date NOT NULL COMMENT '学期开学日期',
  `end_date` date NOT NULL COMMENT '学期结束日期（期末考试最后一天）',
  `week_count` int NULL DEFAULT 20 COMMENT '教学周数',
  `is_current` tinyint(1) NULL DEFAULT 0 COMMENT '是否当前学期（0=否，1=是），全局唯一',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `semester_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'NORMAL' COMMENT '学期类型：NORMAL=正常学期，EXTRA=假期培训',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_is_current`(`is_current` ASC) USING BTREE,
  INDEX `idx_dates`(`start_date` ASC, `end_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学期表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for semester_class
-- ----------------------------
DROP TABLE IF EXISTS `semester_class`;
CREATE TABLE `semester_class`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_semester_class`(`semester_id` ASC, `class_id` ASC) USING BTREE,
  INDEX `idx_semester`(`semester_id` ASC) USING BTREE,
  INDEX `idx_class`(`class_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学期-班级关联表（假期培训用）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_status
-- ----------------------------
DROP TABLE IF EXISTS `student_status`;
CREATE TABLE `student_status`  (
  `user_id` bigint NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'idle' COMMENT 'focusing/idle',
  `last_active` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `student_status_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_data_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_data_permission`;
CREATE TABLE `sys_data_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `resource_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源类型：course/student/score/chapter/schedule',
  `data_scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据范围：ALL全部/DEPT学部/CLASS班级/SELF自己',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_resource`(`role_id` ASC, `resource_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据权限规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `perm_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码，如 course:view, course:edit:self',
  `perm_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `perm_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限类型：MENU菜单/BUTTON按钮/API接口/DATA数据',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父级权限ID，用于菜单层级',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单路径或API路径',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_perm_code`(`perm_code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_perm_type`(`perm_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 229 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码，如 admin/teacher/student/dean/headteacher',
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID，关联 user 表',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教工号',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` tinyint NULL DEFAULT NULL COMMENT '0女 1男',
  `title` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职称 教授/副教授/讲师/助教',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '所属院系',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像OSS地址',
  `research_direction` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '研究方向',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '教师简介',
  `status` tinyint NULL DEFAULT 1 COMMENT '0离职 1在职',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `teacher_no`(`teacher_no` ASC) USING BTREE,
  INDEX `idx_teacher_dept`(`dept_id` ASC) USING BTREE,
  INDEX `idx_teacher_name`(`real_name` ASC) USING BTREE,
  CONSTRAINT `teacher_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `department` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 124 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for teaching_task
-- ----------------------------
DROP TABLE IF EXISTS `teaching_task`;
CREATE TABLE `teaching_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期，如2026-2027-1',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `course_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程名称（冗余，方便查询）',
  `teacher_id` bigint NULL DEFAULT NULL COMMENT '教师ID（teacher表主键）',
  `teacher_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教师姓名（冗余）',
  `weekly_hours` int NOT NULL COMMENT '每周课时数',
  `consecutive` int NULL DEFAULT 1 COMMENT '连堂节数：1=单节, 2=两连堂, 3=三连堂',
  `preferred_room_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '首选教室类型：normal/lab/computer/music/dance/art/sports',
  `preferred_period` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '首选时段：morning=上午, afternoon=下午, any=不限',
  `priority` int NULL DEFAULT 5 COMMENT '排课优先级（1-10，越小越先排）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT '状态：pending=待排, scheduled=已排, failed=排课失败, locked=锁定',
  `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '排课失败原因',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sem_class_course`(`semester` ASC, `class_id` ASC, `course_id` ASC) USING BTREE,
  INDEX `idx_semester`(`semester` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_class`(`class_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 536 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教学任务表（排课输入）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学号',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像OSS地址',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role` tinyint NOT NULL DEFAULT 1 COMMENT '1学生 2教师 3管理员',
  `class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '年级',
  `status` tinyint NULL DEFAULT 1 COMMENT '0禁用 1正常',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `last_login` datetime NULL DEFAULT NULL,
  `current_token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前JWT令牌，单设备登录',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `student_no`(`student_no` ASC) USING BTREE,
  INDEX `idx_user_class`(`class_id` ASC) USING BTREE,
  INDEX `idx_user_status`(`status` ASC) USING BTREE,
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `class_info` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1468 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_course_difficulty
-- ----------------------------
DROP TABLE IF EXISTS `user_course_difficulty`;
CREATE TABLE `user_course_difficulty`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学生用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `difficulty` tinyint NOT NULL DEFAULT 1 COMMENT '当前难度档位：1基础/2中等/3进阶',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_course`(`user_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生课程难度状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_profile
-- ----------------------------
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `total_focus_minutes` int NULL DEFAULT 0 COMMENT '累计专注时长',
  `total_qa_count` int NULL DEFAULT 0 COMMENT '累计问答次数',
  `total_docs` int NULL DEFAULT 0 COMMENT '累计文档数',
  `active_days` int NULL DEFAULT 0 COMMENT '活跃天数',
  `weak_knowledge_points` json NULL COMMENT '薄弱知识点 [\"数据结构\",\"算法\"]',
  `learning_habit` json NULL COMMENT '学习习惯 {\"peak_hour\":\"晚上\",\"style\":\"视觉型\"}',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `user_profile_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户画像表(AI辅助)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wrong_analysis_cache
-- ----------------------------
DROP TABLE IF EXISTS `wrong_analysis_cache`;
CREATE TABLE `wrong_analysis_cache`  (
  `user_id` bigint NOT NULL,
  `cache_hash` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0',
  `analysis_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
