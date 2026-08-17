package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生每题作答详情
 */
@Data
@TableName("exam_submission_answer")
public class ExamSubmissionAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作答记录ID */
    private Long submissionId;

    /** 题目ID */
    private Long questionId;

    /** 题号 */
    private Integer questionIndex;

    /** 题型 */
    private String questionType;

    /** 题目内容快照 */
    private String question;

    /** 选项快照 JSON */
    private String options;

    /** 学生答案 */
    private String userAnswer;

    /** 正确答案快照 */
    private String correctAnswer;

    /** 1对 0错 -1不会 -2跳过 */
    private Integer isCorrect;

    /** 本题得分 */
    private Integer score;

    /** AI评分（简答题） */
    private Integer aiScore;

    /** AI评语（简答题） */
    private String aiComment;

    /** 本题满分快照 */
    private Integer maxScore;

    /** 教师评分评语 */
    private String teacherComment;

    /** 教师人工调整次数（最多2次） */
    private Integer scoreAdjustCount;

    /** 本题耗时秒 */
    private Integer durationSec;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
