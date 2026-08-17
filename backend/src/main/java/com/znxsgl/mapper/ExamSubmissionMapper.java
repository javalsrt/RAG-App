package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.ExamSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExamSubmissionMapper extends BaseMapper<ExamSubmission> {

    @Select("SELECT * FROM exam_submission WHERE exam_homework_id = #{examHomeworkId} AND user_id = #{userId} LIMIT 1")
    ExamSubmission findByExamAndUser(@Param("examHomeworkId") Long examHomeworkId,
                                      @Param("userId") Long userId);
}
