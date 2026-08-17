package com.znxsgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限表（RBAC）：菜单、按钮、API、数据范围统一抽象为权限
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码，如 course:view */
    private String permCode;

    /** 权限名称 */
    private String permName;

    /** 权限类型：MENU菜单/BUTTON按钮/API接口/DATA数据 */
    private String permType;

    /** 父级权限ID */
    private Long parentId;

    /** 菜单路径或API路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;

    private LocalDateTime createTime;
}
