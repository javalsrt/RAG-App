package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.ExamSubmissionAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamSubmissionAnswerMapper extends BaseMapper<ExamSubmissionAnswer> {

    @Select("SELECT * FROM exam_submission_answer WHERE submission_id = #{submissionId} ORDER BY question_index")
    List<ExamSubmissionAnswer> findBySubmissionId(@Param("submissionId") Long submissionId);
}
