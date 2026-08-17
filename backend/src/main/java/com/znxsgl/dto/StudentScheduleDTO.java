package com.znxsgl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalTime;

@Data
public class StudentScheduleDTO {
    private Long scheduleId;
    private Long courseId;
    private String courseName;
    private String teacherName;
    private Integer dayOfWeek;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Integer startNode;
    private Integer step;
    private String classroom;
    private String semester;
    private String weeks;
}
