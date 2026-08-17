package com.znxsgl.student;

/**
 * @提及选项数据模型
 */
public class MentionItem {
    public final String id;        // 用户ID或标识，AI固定为"AI"
    public final String name;      // 用于插入输入框的@名称
    public final String label;     // 展示名称
    public final String tag;       // 副标题/标签
    public final String type;      // ai / teacher / student
    public final String avatarText; // 头像文字

    public MentionItem(String id, String name, String label, String tag, String type, String avatarText) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.tag = tag;
        this.type = type;
        this.avatarText = avatarText;
    }

    public MentionItem(String name, String label, String tag) {
        this(name, name, label, tag, "ai", name.substring(0, Math.min(2, name.length())));
    }
}
