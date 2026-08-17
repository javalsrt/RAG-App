package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期表
 *
 * is_current=1 表示当前生效学期，全局唯一。
 * 历史学期数据保留可查，但默认查询使用当前学期。
 */
@Data
@TableName("semester")
public class Semester {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学期名称，如 "2026-2027-1" */
    private String name;

    /** 学期开学日期 */
    private LocalDate startDate;

    /** 学期结束日期（期末考试最后一天） */
    private LocalDate endDate;

    /** 教学周数 */
    private Integer weekCount;

    /** 是否当前学期（0=否，1=是），全局唯一 */
    private Integer isCurrent;

    /** 学期类型：NORMAL=正常学期，EXTRA=假期培训 */
    private String semesterType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
