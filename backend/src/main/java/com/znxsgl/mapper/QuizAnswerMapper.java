package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.QuizAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuizAnswerMapper extends BaseMapper<QuizAnswer> {

    /** 查询学生所有错题/空题（已明白/已收藏的排除） */
    @Select("SELECT a.* FROM quiz_answer a " +
            "JOIN quiz_session s ON a.session_id = s.id " +
            "WHERE s.user_id = #{userId} " +
            "AND (a.is_correct = 0 OR a.is_correct = -1 OR a.is_correct = -2) " +
            "AND a.understood = 0 " +
            "ORDER BY a.subject, a.created_at")
    List<QuizAnswer> findWrongByUser(@Param("userId") Long userId);

    /** 查询学生各科目正确率 */
    @Select("SELECT a.subject, COUNT(*) as total, " +
            "SUM(CASE WHEN a.is_correct = 1 THEN 1 ELSE 0 END) as correct " +
            "FROM quiz_answer a JOIN quiz_session s ON a.session_id = s.id " +
            "WHERE s.user_id = #{userId} GROUP BY a.subject")
    List<Map<String, Object>> subjectStats(@Param("userId") Long userId);
}
