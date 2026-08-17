package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.QuizSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizSessionMapper extends BaseMapper<QuizSession> {

    @Select("SELECT COUNT(*) FROM quiz_session WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);

    @Select("SELECT id FROM quiz_session WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    Long findLatestId(@Param("userId") Long userId);
}
