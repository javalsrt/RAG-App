package com.znxsgl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 学生向 AI 助教提问请求（基于 RAG 检索课程知识库后回答）
 */
public record RagChatRequest(

        @NotBlank(message = "课程名不能为空")
        String courseName,

        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题不能超过 1000 字")
        String question
) {
}
