package com.znxsgl.dto;

import lombok.Data;

/**
 * 课时/资源响应对象
 */
@Data
public class LessonDTO {

    /** 课时ID */
    private Long id;

    /** 所属章节ID */
    private Long chapterId;

    /** 课时序号 */
    private Integer lessonNo;

    /** 课时名称 */
    private String lessonName;

    /** 资源类型：video视频/document文档/quiz测验/link链接 */
    private String resourceType;

    /** 资源URL */
    private String resourceUrl;

    /** 视频/音频时长（秒） */
    private Integer duration;

    /** 文本内容或富文本 */
    private String content;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;
}
