package com.znxsgl.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 章节保存请求（新增/更新）
 */
@Data
public class ChapterSaveRequest {

    /** 章节ID，更新时必填 */
    private Long id;

    /** 所属课程ID */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /** 章节序号 */
    @NotNull(message = "章节序号不能为空")
    private Integer chapterNo;

    /** 章节名称 */
    @NotBlank(message = "章节名称不能为空")
    private String chapterName;

    /** 章节描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;
}
