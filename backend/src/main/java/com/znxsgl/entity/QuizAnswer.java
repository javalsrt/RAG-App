package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 测验答题明细表，存储一次测验中每道题的作答情况
 */
@Data
@TableName("quiz_answer")
public class QuizAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 测验会话ID */
    private Long sessionId;

    /** 题目序号 */
    private Integer questionIndex;
    private String questionType;
    private String subject;
    private String question;

    /** 选项，JSON格式存储 */
    private String options;
    private String userAnswer;
    private String correctAnswer;

    /** 是否答对：0答错，1答对 */
    private Integer isCorrect;

    /** 答题耗时，单位秒 */
    private Integer durationSec;

    /** 答案修改次数 */
    private Integer modifiedCount;

    /** 是否已掌握：0未掌握，1已掌握 */
    private Integer understood;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
