package com.znxsgl.dto;

import lombok.Data;

/**
 * 学生提问统计（教师端查看）
 */
@Data
public class StudentAskStatsDTO {
    private Long userId;
    private String studentNo;
    private String realName;
    private String className;
    private int askCount;
}
