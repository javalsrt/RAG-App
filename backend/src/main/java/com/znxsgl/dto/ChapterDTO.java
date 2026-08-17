package com.znxsgl.dto;

import lombok.Data;
import java.util.List;

/**
 * 课程章节响应对象，包含章节基础信息及下属课时列表
 */
@Data
public class ChapterDTO {

    /** 章节ID */
    private Long id;

    /** 所属课程ID */
    private Long courseId;

    /** 课程名称 */
    private String courseName;

    /** 章节序号 */
    private Integer chapterNo;

    /** 章节名称 */
    private String chapterName;

    /** 章节描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 下属课时列表 */
    private List<LessonDTO> lessons;
}
