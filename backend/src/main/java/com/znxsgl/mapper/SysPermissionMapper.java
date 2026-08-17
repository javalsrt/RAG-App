package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据用户ID查询权限编码列表（去重，仅启用状态）
     */
    @Select("""
        SELECT DISTINCT sp.perm_code
        FROM sys_permission sp
        JOIN sys_role_permission srp ON sp.id = srp.permission_id
        JOIN sys_user_role sur ON srp.role_id = sur.role_id
        WHERE sur.user_id = #{userId} AND sp.status = 1
        ORDER BY sp.perm_code
    """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);
}
