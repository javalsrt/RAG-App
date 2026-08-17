package com.znxsgl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增/修改学期的请求 DTO
 */
@Data
public class SemesterSaveRequest {

    @NotBlank(message = "学期名称不能为空")
    private String name;

    @NotNull(message = "开学日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer weekCount;

    /** 学期类型：NORMAL=正常学期，EXTRA=假期培训 */
    private String semesterType;

    /** 关联班级ID列表（仅 EXTRA 类型需要，NORMAL 默认所有班级） */
    private List<Long> classIds;
}
