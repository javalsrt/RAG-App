package com.znxsgl.student.model;

import java.util.ArrayList;
import java.util.List;

public class Chapter {
    private long id;
    private int chapterNo;
    private String chapterName;
    private String description;
    private List<Lesson> lessons = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getChapterNo() { return chapterNo; }
    public void setChapterNo(int chapterNo) { this.chapterNo = chapterNo; }
    public String getChapterName() { return chapterName; }
    public void setChapterName(String chapterName) { this.chapterName = chapterName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Lesson> getLessons() { return lessons; }
    public void setLessons(List<Lesson> lessons) { this.lessons = lessons; }
}
