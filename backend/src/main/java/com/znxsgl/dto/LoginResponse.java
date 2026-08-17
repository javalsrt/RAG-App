package com.znxsgl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String realName;
    private String username;
    private Integer role;       // 1学生 2教师 3管理员（兼容旧字段）
    private Long userId;
    private String avatarUrl;
    private String message;

    /** 角色编码列表，如 ["admin", "teacher"] */
    private List<String> roles;

    /** 权限编码列表，如 ["course:view", "course:edit:self"] */
    private List<String> permissions;

    /** 登录失败时使用：仅返回错误消息 */
    public LoginResponse(String message) {
        this.message = message;
    }

    /** 登录成功时使用：构造完整响应 */
    public static LoginResponse success(String token, String realName, String username,
                                        Integer role, Long userId, String avatarUrl,
                                        List<String> roles, List<String> permissions) {
        return LoginResponse.builder()
                .token(token)
                .realName(realName)
                .username(username)
                .role(role)
                .userId(userId)
                .avatarUrl(avatarUrl)
                .roles(roles)
                .permissions(permissions)
                .message("登录成功")
                .build();
    }
}
