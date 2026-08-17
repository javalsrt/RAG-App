package com.znxsgl.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求（只读 DTO，使用 Java Record 简化样板代码）
 *
 * @param username 用户名或学号
 * @param password 密码明文（HTTPS 传输）
 */
public record LoginRequest(

        @NotBlank(message = "账号不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
