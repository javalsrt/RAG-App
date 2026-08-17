package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试/作业题目表
 */
@Data
@TableName("exam_question")
public class ExamQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属考试作业ID */
    private Long examHomeworkId;

    /** 题号 1-N */
    private Integer questionIndex;

    /** 题型：single_choice/multiple_choice/true_false/fill_blank/short_answer */
    private String questionType;

    /** 题目内容 */
    private String content;

    /** 选项 JSON 数组 */
    private String options;

    /** 参考答案 */
    private String answer;

    /** 分值 */
    private Integer score;

    /** 难度 */
    private String difficulty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
