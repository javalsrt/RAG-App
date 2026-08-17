package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 班级信息表，存储学校班级的基础信息
 */
@Data
@TableName("class_info")
public class ClassInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    private String major;
    private String department;
    private String grade;
}
