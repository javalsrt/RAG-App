package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程导入记录
 *
 * 记录每次课程导入的基本信息，包括文件名、导入人、成功/跳过数量、操作时间等。
 */
@Data
@TableName("course_import_record")
public class CourseImportRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 导入时上传的文件名 */
    private String fileName;

    /** 导入人 user_id */
    private Long importedBy;

    /** 导入人姓名 */
    private String importedByName;

    /** 学期 */
    private String semester;

    /** 本次导入总条数 */
    private Integer totalCount;

    /** 成功导入条数 */
    private Integer successCount;

    /** 跳过条数 */
    private Integer skipCount;

    /** 导入结果消息（JSON 数组字符串） */
    private String messages;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
