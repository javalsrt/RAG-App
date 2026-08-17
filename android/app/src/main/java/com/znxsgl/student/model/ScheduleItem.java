package com.znxsgl.student.model;

import com.google.gson.annotations.SerializedName;

public class ScheduleItem {
    @SerializedName("scheduleId")
    private long scheduleId;

    @SerializedName("courseId")
    private Long courseId;

    @SerializedName("courseName")
    private String courseName;

    @SerializedName("teacherName")
    private String teacherName;

    @SerializedName("dayOfWeek")
    private int dayOfWeek;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("startNode")
    private int startNode;

    @SerializedName("step")
    private int step;

    @SerializedName("classroom")
    private String classroom;

    // Getters
    public long getScheduleId() { return scheduleId; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getTeacherName() { return teacherName; }
    public int getDayOfWeek() { return dayOfWeek; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getStartNode() { return startNode; }
    public int getStep() { return step; }
    public String getClassroom() { return classroom; }

    /**
     * 判断该课是否匹配指定时段
     * @param slotStartTime 如 "08:30"
     * @param slotEndTime 如 "09:55"
     */
    public boolean matchesTimeSlot(String slotStartTime, String slotEndTime) {
        if (startTime == null || endTime == null) return false;
        return startTime.compareTo(slotEndTime) < 0 && endTime.compareTo(slotStartTime) > 0;
    }
}
