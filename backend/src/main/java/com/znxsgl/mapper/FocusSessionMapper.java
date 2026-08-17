package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.entity.FocusSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FocusSessionMapper extends BaseMapper<FocusSession> {

    /** 某学生今日专注总时长（秒） */
    @Select("SELECT IFNULL(SUM(duration_seconds), 0) FROM focus_session WHERE user_id = #{userId} AND DATE(finished_at) = CURDATE()")
    int todayTotalSeconds(@Param("userId") Long userId);

    /** 某学生总专注时长（秒） */
    @Select("SELECT IFNULL(SUM(duration_seconds), 0) FROM focus_session WHERE user_id = #{userId}")
    int totalSeconds(@Param("userId") Long userId);

    /** 某班级学生专注排行 */
    @Select("SELECT u.id AS userId, u.real_name AS realName, SUM(f.duration_seconds) AS totalSeconds " +
            "FROM focus_session f JOIN user u ON u.id = f.user_id " +
            "WHERE u.class_id = #{classId} " +
            "GROUP BY u.id ORDER BY totalSeconds DESC")
    List<Map<String, Object>> rankByClass(@Param("classId") Long classId);
}
