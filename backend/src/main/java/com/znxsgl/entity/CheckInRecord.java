package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 签到记录表，存储学生参与签到活动的结果
 */
@Data
@TableName("check_in_record")
public class CheckInRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 签到活动ID */
    private Long checkInId;

    /** 学生用户ID */
    private Long studentId;
    private String studentName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime checkedAt;
}
