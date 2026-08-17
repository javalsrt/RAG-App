package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课表锁定表
 *
 * 教师自主调课后，将调课后的 schedule 记录锁定。
 * 管理员重新执行"一键排课"时，锁定的课作为硬约束，位置不变。
 */
@Data
@TableName("schedule_lock")
public class ScheduleLock {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对应 schedule 表记录ID */
    private Long scheduleId;

    /** 学期（冗余，方便按学期查询） */
    private String semester;

    /** 锁定人 user_id（教师） */
    private Long lockedBy;

    /** 锁定人姓名 */
    private String lockedByName;

    /** 锁定原因 */
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
