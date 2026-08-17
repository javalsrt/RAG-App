package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 章节阅读进度表，记录学生已完成的课时
 */
@Data
@TableName("chapter_read_progress")
public class ChapterReadProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Long lessonId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime completedAt;
}
