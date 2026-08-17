package com.znxsgl.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 课时/资源保存请求（新增/更新）
 */
@Data
public class LessonSaveRequest {

    /** 课时ID，更新时必填 */
    private Long id;

    /** 所属章节ID */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /** 课时序号 */
    @NotNull(message = "课时序号不能为空")
    private Integer lessonNo;

    /** 课时名称 */
    @NotBlank(message = "课时名称不能为空")
    private String lessonName;

    /** 资源类型：video/document/quiz/link */
    @NotBlank(message = "资源类型不能为空")
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
