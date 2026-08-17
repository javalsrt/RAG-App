package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教师发布的考试/作业主表
 */
@Data
@TableName("exam_homework")
public class ExamHomework {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型：exam考试 / homework作业 */
    private String type;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 关联课程ID（可选） */
    private Long courseId;

    /** 目标班级ID */
    private Long classId;

    /** 发布教师ID */
    private Long teacherId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 截止时间 */
    private LocalDateTime endTime;

    /** 限时分钟，0=不限时（作业） */
    private Integer timeLimit;

    /** 总分 */
    private Integer totalScore;

    /** 及格分 */
    private Integer passScore;

    /** 发布方式：immediate立即发布 / scheduled定时发布 */
    private String publishMode;

    /** 定时发布时间 */
    private LocalDateTime scheduledTime;

    /** 出题方式：ai-range按范围 / ai-document按文档 */
    private String questionMode;

    /** 题型配置 JSON 数组 */
    private String questionTypes;

    /** 难度 */
    private String difficulty;

    /** 题目数量 */
    private Integer questionCount;

    /** 状态：0草稿 1进行中 2已结束 3已下架 */
    private Integer status;

    /** 教师已修改次数 */
    private Integer editCount;

    /** 最大可修改次数（默认2） */
    private Integer maxEditCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
