package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程章节表，存储课程下的一级章节结构
 */
@Data
@TableName("course_chapter")
public class CourseChapter {

    /** 章节ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属课程ID */
    private Long courseId;

    /** 章节序号，如第1章、第2章 */
    private Integer chapterNo;

    /** 章节名称 */
    private String chapterName;

    /** 章节描述 */
    private String description;

    /** 排序，越小越靠前 */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 逻辑删除标记（配合 MyBatis-Plus） */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
