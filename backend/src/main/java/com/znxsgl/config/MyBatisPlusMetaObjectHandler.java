package com.znxsgl.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * 配合实体字段上的 {@code @TableField(fill = FieldFill.INSERT)} /
 * {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 注解使用，
 * 自动维护 createdAt / lastLogin / checkedAt 等审计字段，无需手动赋值。
 */
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

    /** 插入时自动填充 */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 严格按字段名匹配：存在的字段才填充
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "lastLogin", LocalDateTime.class, now);
        strictInsertFill(metaObject, "checkedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "completedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    /** 更新时自动填充 */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictUpdateFill(metaObject, "lastLogin", LocalDateTime.class, now);
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
}
