package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 排课规则表
 *
 * 按课程类型或具体科目配置排课偏好：
 * - 首选时段（上午/下午）
 * - 首选教室类型
 * - 连堂节数
 * - 优先级加成
 */
@Data
@TableName("schedule_rule")
public class ScheduleRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程类型：public_basic/professional_core/skill/art */
    private String courseType;

    /** 具体科目（空=通用规则） */
    private String subject;

    /** 建议时段：morning/afternoon/any */
    private String preferredPeriod;

    /** 建议教室类型 */
    private String preferredRoomType;

    /** 建议连堂节数 */
    private Integer consecutive;

    /** 优先级加成（负数=更先排） */
    private Integer priorityBoost;

    /** 规则说明 */
    private String description;

    /** 是否启用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
