package com.znxsgl.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量导入教学任务的请求 DTO
 */
@Data
public class TeachingTaskSaveRequest {

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Long teacherId;

    /** 周课时数 */
    private Integer weeklyHours;

    /** 连堂节数：1=单节, 2=两连, 3=三连 */
    private Integer consecutive;

    /** 首选教室类型：normal/lab/computer/music/dance/art/sports */
    private String preferredRoomType;

    /** 首选时段：morning/afternoon/any */
    private String preferredPeriod;

    /** 优先级（1-10，越小越先排） */
    private Integer priority;
}
