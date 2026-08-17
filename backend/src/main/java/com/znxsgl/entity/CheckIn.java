package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 签到活动表，存储课程签到任务及其密码/有效期
 */
@Data
@TableName("check_in")
public class CheckIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseName;

    /** 创建者（教师）用户ID */
    private Long createdBy;
    private String creatorName;

    /** 签到密码 */
    private String password;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 签到截止时间 */
    private LocalDateTime expiresAt;

    /** 是否启用：0未启用，1进行中 */
    private Integer active;

    /** 逻辑删除标志：0未删除，1已删除 */
    @TableLogic
    private Integer deleted;
}
