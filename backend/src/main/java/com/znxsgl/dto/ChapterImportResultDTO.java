package com.znxsgl.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程章节 Excel 导入结果统计
 */
@Data
public class ChapterImportResultDTO {

    /** 成功导入的章节数量 */
    private int chapterCount;

    /** 成功导入的课时数量 */
    private int lessonCount;

    /** 失败行数 */
    private int failCount;

    /** 失败明细 */
    private List<ImportFailureDTO> failures = new ArrayList<>();
}
