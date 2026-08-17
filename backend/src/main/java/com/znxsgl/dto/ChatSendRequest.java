package com.znxsgl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 学生发送聊天消息请求
 *
 * 示范：用类型化 DTO + Validation 替代 Map<String, String>，提升类型安全与可读性。
 */
public record ChatSendRequest(

        @NotBlank(message = "课程名不能为空")
        String courseName,

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 2000, message = "单条消息不能超过 2000 字")
        String content,

        /** @私密消息的目标学生 ID（可空，null=公开消息） */
        Long mentionUserId
) {
}
