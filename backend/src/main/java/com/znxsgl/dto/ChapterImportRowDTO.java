package com.znxsgl.dto;

import lombok.Data;

/**
 * Excel 导入行数据，对应课程章节导入模板的一行记录
 */
@Data
public class ChapterImportRowDTO {

    /** 章节序号，如第1章、第2章 */
    private Integer chapterNo;

    /** 章节名称 */
    private String chapterName;

    /** 章节描述 */
    private String description;

    /** 课时序号，如第1节、第2节 */
    private Integer lessonNo;

    /** 课时名称 */
    private String lessonName;

    /** 资源类型：video/document/quiz/link */
    private String resourceType;

    /** 资源URL */
    private String resourceUrl;

    /** 视频/音频时长（秒） */
    private Integer duration;

    /** 文本内容或富文本 */
    private String content;
}
