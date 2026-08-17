package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 课程-班级关联表，表示某学期某课程开设给哪些班级
 */
@Data
@TableName("course_class")
public class CourseClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 班级ID */
    private Long classId;
    private String semester;
}
