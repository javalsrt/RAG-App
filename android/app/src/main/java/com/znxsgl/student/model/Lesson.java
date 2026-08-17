package com.znxsgl.student.model;

public class Lesson {
    private long id;
    private int lessonNo;
    private String lessonName;
    private String resourceType;
    private String resourceUrl;
    private String content;
    private Integer duration;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getLessonNo() { return lessonNo; }
    public void setLessonNo(int lessonNo) { this.lessonNo = lessonNo; }
    public String getLessonName() { return lessonName; }
    public void setLessonName(String lessonName) { this.lessonName = lessonName; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
