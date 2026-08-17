package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.DocumentVector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentVectorMapper extends BaseMapper<DocumentVector> {

    /** 查询指定课程下所有带向量的文档块（用于向量语义检索） */
    @Select("SELECT * FROM document_vector " +
            "WHERE course_name = #{courseName} AND embedding IS NOT NULL")
    List<DocumentVector> findByCourseWithEmbedding(@Param("courseName") String courseName);
}
