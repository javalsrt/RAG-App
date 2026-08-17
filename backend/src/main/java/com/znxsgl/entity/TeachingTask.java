package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教学任务表（排课输入）
 *
 * 一条记录代表"某班级某学期某门课的排课需求"。
 * 管理员先批量录入教学任务，再一键排课生成 schedule 记录。
 */
@Data
@TableName("teaching_task")
public class TeachingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学期 */
    private String semester;

    /** 班级ID */
    private Long classId;

    /** 课程ID */
   private Long courseId;

    /** 课程名称（冗余） */
    private String courseName;

    /** 教师ID（teacher表主键） */
    private Long teacherId;

    /** 教师姓名（冗余） */
    private String teacherName;

    /** 每周课时数 */
    private Integer weeklyHours;

    /** 连堂节数：1=单节, 2=两连, 3=三连 */
    private Integer consecutive;

    /** 首选教室类型 */
    private String preferredRoomType;

    /** 首选时段：morning/afternoon/any */
    private String preferredPeriod;

    /** 排课优先级（1-10，越小越先排） */
    private Integer priority;

    /** 状态：pending/scheduled/failed/locked */
    private String status;

    /** 排课失败原因 */
    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
