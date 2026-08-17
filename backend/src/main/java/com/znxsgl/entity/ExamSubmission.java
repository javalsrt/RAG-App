package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生考试/作业作答记录
 */
@Data
@TableName("exam_submission")
public class ExamSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试作业ID */
    private Long examHomeworkId;

    /** 学生用户ID */
    private Long userId;

    /** 状态：pending进行中 / completed已完成 */
    private String status;

    /** 总得分 */
    private Integer totalScore;

    /** AI自动评分总分（教师调整前） */
    private Integer autoScore;

    /** 用时秒 */
    private Integer durationSec;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
