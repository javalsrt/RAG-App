package com.znxsgl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excel 导入失败行信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportFailureDTO {

    /** 失败行号（从1开始，含表头） */
    private int row;

    /** 失败原因 */
    private String reason;
}
