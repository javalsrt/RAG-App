package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程课时与资源表，存储章节下的课时及关联资源
 * 支持视频、文档、测验、外部链接四种资源类型
 */
@Data
@TableName("course_lesson")
public class CourseLesson {

    /** 课时ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属章节ID */
    private Long chapterId;

    /** 课时序号，如第1节、第2节 */
    private Integer lessonNo;

    /** 课时名称 */
    private String lessonName;

    /** 资源类型：video视频/document文档/quiz测验/link链接 */
    private String resourceType;

    /** 资源URL（视频地址、文档地址、链接等） */
    private String resourceUrl;

    /** 视频/音频时长（秒） */
    private Integer duration;

    /** 文本内容或富文本（测验题目、文档正文等） */
    private String content;

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
