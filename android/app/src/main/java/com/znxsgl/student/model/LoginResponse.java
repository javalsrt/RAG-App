package com.znxsgl.student.model;

public class LoginResponse {
    private String token;
    private String realName;
    private String username;
    private int role;
    private long userId;
    private String avatarUrl;
    private String message;

    public String getToken() { return token; }
    public String getRealName() { return realName; }
    public String getUsername() { return username; }
    public int getRole() { return role; }
    public long getUserId() { return userId; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getMessage() { return message; }
}
