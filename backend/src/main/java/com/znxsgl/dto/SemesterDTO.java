package com.znxsgl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 学期信息 DTO（用于列表展示和当前学期查询）
 */
@Data
public class SemesterDTO {
    private Long id;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer weekCount;
    private Boolean isCurrent;

    /** 学期类型：NORMAL=正常学期，EXTRA=假期培训 */
    private String semesterType;

    /** 关联班级ID列表（EXTRA 类型专用） */
    private List<Long> classIds;

    /** 关联班级名称列表（用于前端展示） */
    private List<String> classNames;

    /** 状态标记：before=未开始 / ongoing=进行中 / ended=已结束 / vacation=假期 */
    private String status;

    /** 友好提示语（如"当前为暑假，9月1日开学"） */
    private String notice;
}
