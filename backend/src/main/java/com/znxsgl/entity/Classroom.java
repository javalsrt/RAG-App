package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教室资源表
 */
@Data
@TableName("classroom")
public class Classroom {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 教室名称 */
    private String name;

    /** 类型：normal/lab/computer/music/dance/art/sports */
    private String type;

    /** 容纳人数 */
   private Integer capacity;

    /** 所在楼 */
   private String building;

    /** 楼层 */
    private Integer floor;

    /** 设备清单 */
    private String equipment;

    /** 是否启用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
