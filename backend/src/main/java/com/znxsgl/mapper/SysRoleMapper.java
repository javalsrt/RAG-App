package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色编码列表
     */
    @Select("""
        SELECT DISTINCT sr.role_code
        FROM sys_role sr
        JOIN sys_user_role sur ON sr.id = sur.role_id
        WHERE sur.user_id = #{userId} AND sr.status = 1
        ORDER BY sr.role_code
    """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
