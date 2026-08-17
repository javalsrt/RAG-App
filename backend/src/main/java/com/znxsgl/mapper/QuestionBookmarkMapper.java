package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.QuestionBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionBookmarkMapper extends BaseMapper<QuestionBookmark> {

    @Select("SELECT * FROM question_bookmark WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<QuestionBookmark> findByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM question_bookmark WHERE user_id = #{userId} AND question LIKE CONCAT('%',#{question},'%') LIMIT 1")
    QuestionBookmark findByUserAndQuestion(@Param("userId") Long userId, @Param("question") String question);
}
