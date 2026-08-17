package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档向量表，存储课程文档的分块内容及向量嵌入（用于RAG检索）
 */
@Data
@TableName("document_vector")
public class DocumentVector {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseName;

    /** 章节ID(空=课程级文档) */
    private Long chapterId;

    /** 文档名称 */
    private String docName;

    /** 文档内容分块 */
    private String contentChunk;

    /** 向量嵌入，JSON数组格式存储（text-embedding-v3: 1024维） */
    private String embedding;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
