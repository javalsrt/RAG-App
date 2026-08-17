package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalTime;

/**
 * 课程表/排课信息表，存储用户的上课安排
 */
@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 课程ID */
    private Long courseId;
    private String courseName;

    /** 星期几：1-7分别表示周一到周日 */
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    /** 起始节次 */
    private Integer startNode;

    /** 持续节数 */
    private Integer step;
    private String classroom;
    private String semester;

    /** 周次范围 */
    private String weeks;

    /** 状态：1正常，0已下架 */
    private Integer status;
}
