package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表 (学生/教师/管理员)
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String username;
    private String passwordHash;
    private String realName;
    private String avatarUrl;
    private String email;
    private String phone;
    private Integer role;     // 1学生 2教师 3管理员
    private Long classId;
    private String major;
    private String grade;
    private Integer status;   // 0禁用 1正常
    private String currentToken;  // 当前有效token，用于单设备登录

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastLogin;
}
