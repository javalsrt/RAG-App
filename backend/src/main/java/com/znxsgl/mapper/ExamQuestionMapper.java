package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.ExamQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    @Select("SELECT * FROM exam_question WHERE exam_homework_id = #{examHomeworkId} ORDER BY question_index")
    List<ExamQuestion> findByExamHomeworkId(@Param("examHomeworkId") Long examHomeworkId);
}
