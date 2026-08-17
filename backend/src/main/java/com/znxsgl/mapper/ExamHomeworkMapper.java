package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.ExamHomework;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamHomeworkMapper extends BaseMapper<ExamHomework> {

    @Select("SELECT * FROM exam_homework WHERE class_id = #{classId} " +
            "AND status IN (1, 2) AND end_time > NOW() ORDER BY start_time")
    List<ExamHomework> findActiveByClassId(@Param("classId") Long classId);
}
