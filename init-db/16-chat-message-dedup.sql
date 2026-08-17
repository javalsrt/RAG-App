-- 修复考试作业通知重复发送问题：增加业务标识字段 + 唯一索引
-- 应用场景：考试/作业发布通知 biz_type='exam_publish' + biz_id=exam_homework_id
-- 聊天消息正常场景 biz_type=NULL, biz_id=NULL（不触发唯一约束）

ALTER TABLE chat_message
  ADD COLUMN biz_type VARCHAR(50) NULL COMMENT '业务类型：exam_publish/作业发布等普通聊天为NULL' AFTER mention_user_id,
  ADD COLUMN biz_id BIGINT NULL COMMENT '业务ID：对应 exam_homework.id 等' AFTER biz_type;

-- 唯一索引：同一学生不能收到同一条考试/作业的发布通知超过1次
-- (user_id, biz_type, biz_id) 三元组唯一；普通聊天 biz_type=NULL 不会进唯一索引(InnoDB B-tree NULL值不冲突)
CREATE UNIQUE INDEX uk_user_biz ON chat_message(user_id, biz_type, biz_id);

-- 清理已经重复的历史脏数据：每个(user_id, biz_type, biz_id)只保留最小id那一条
-- 这里针对考试通知先手动去重（用created_at+id判断）
DELETE c1 FROM chat_message c1
  INNER JOIN chat_message c2
  WHERE c1.user_id = c2.user_id
    AND c1.biz_type = c2.biz_type
    AND c1.biz_id = c2.biz_id
    AND c1.id > c2.id
    AND c1.biz_type IS NOT NULL;
