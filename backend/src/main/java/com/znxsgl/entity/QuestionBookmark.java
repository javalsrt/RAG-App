package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 错题收藏表，存储用户收藏的题目及复盘信息
 */
@Data
@TableName("question_bookmark")
public class QuestionBookmark {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 题目类型 */
    private String questionType;

    /** 学科/课程 */
    private String subject;
    private String question;
    private String userAnswer;
    private String correctAnswer;

    /** 知识点 */
    private String knowledge;

    /** 错误原因 */
    private String errorReason;

    /** 改进建议 */
    private String improve;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
