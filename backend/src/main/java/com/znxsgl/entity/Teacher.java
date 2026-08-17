package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 教师信息表，存储教师基础资料
 */
@Data
@TableName("teacher")
public class Teacher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 教师工号 */
    private String teacherNo;
    private String realName;

    /** 性别：0女，1男，2保密 */
    private Integer gender;

    /** 职称 */
    private String title;

    /** 所属院系ID */
    private Long deptId;
    private String email;
    private String phone;

    /** 头像地址 */
    private String avatarUrl;

    /** 状态：0禁用，1正常 */
    private Integer status;
}
