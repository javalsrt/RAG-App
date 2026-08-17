package com.znxsgl.student.model;

import com.google.gson.annotations.SerializedName;

public class ChatMsgDto {
    @SerializedName("id")
    private long id;
    @SerializedName("courseName")
    private String courseName;
    @SerializedName("userId")
    private long userId;
    @SerializedName("senderName")
    private String senderName;
    @SerializedName("senderRole")
    private String senderRole;
    @SerializedName("content")
    private String content;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("bizType")
    private String bizType;
    @SerializedName("bizId")
    private Long bizId;

    // 客户端解析：消息类型（text / image / file / exam_card）
    public String msgType = "text";
    public String imageUrl;
    public String fileName;
    public String fileUrl;
    // 考试卡片解析字段
    public String examTitle;
    public String examStatus;
    public Long examId;
    public String examType;

    public long getId() { return id; }
    public void setId(long v) { id = v; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String v) { courseName = v; }
    public long getUserId() { return userId; }
    public void setUserId(long v) { userId = v; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String v) { senderName = v; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String v) { senderRole = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }

    public String getBizType() { return bizType; }
    public void setBizType(String v) { bizType = v; }

    public Long getBizId() { return bizId; }
    public void setBizId(Long v) { bizId = v; }
}
