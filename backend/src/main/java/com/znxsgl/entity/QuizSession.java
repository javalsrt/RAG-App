package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 测验会话表，存储用户一次测验的整体情况与结果分析
 */
@Data
@TableName("quiz_session")
public class QuizSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;
    private String subject;

    /** 课程ID（自适应出题） */
    private Long courseId;

    /** 本次出题难度：1基础/2中等/3进阶 */
    private Integer difficulty;

    /** 本次出题章节范围(JSON: chapterId数组) */
    private String chapterScope;

    /** 学科类型 */
    private String subjectType;

    /** 测验序号（第几次测验） */
    private Integer sessionNo;
    private Integer totalQuestions;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer skipCount;

    /** 总耗时，单位秒 */
    private Integer totalDurationSec;

    /** 各题得分，JSON格式存储 */
    private String scores;
    private String strengths;
    private String weaknesses;
    private String suggestion;
    private String studyPlan;

    /** 会话状态 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
