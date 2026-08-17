package com.znxsgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.znxsgl.dto.StudentAskStatsDTO;
import com.znxsgl.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /** 按课程统计学生提问次数（可选班级过滤） */
    @Select("<script>" +
            "SELECT u.id AS userId, u.student_no AS studentNo, u.real_name AS realName, " +
            "  ci.class_name AS className, COUNT(cm.id) AS askCount " +
            "FROM chat_message cm " +
            "JOIN user u ON u.id = cm.user_id " +
            "LEFT JOIN class_info ci ON ci.id = u.class_id " +
            "WHERE cm.course_name = #{courseName} " +
            "  AND cm.sender_role = 'student' " +
            "<if test='classId != null'> AND u.class_id = #{classId} </if>" +
            "GROUP BY u.id, u.student_no, u.real_name, ci.class_name " +
            "ORDER BY askCount DESC" +
            "</script>")
    List<StudentAskStatsDTO> countStudentAsks(@Param("courseName") String courseName,
                                              @Param("classId") Long classId);

    /** 标记课程中所有当前学生应读的消息为已读：
     *  1) 通知/教师回复/AI 回复（user_id 为接收学生）
     *  2) 其他学生发送的公聊消息（user_id 为发送者）
     *  3) @提及当前学生的消息
     */
    @Update("UPDATE chat_message SET is_read = 1 " +
            "WHERE course_name = #{courseName} AND is_read = 0 " +
            "  AND (" +
            "    (user_id = #{userId} AND sender_role != 'student') " +
            "    OR (user_id != #{userId} AND sender_role = 'student') " +
            "    OR mention_user_id = #{userId}" +
            "  )")
    int markAsRead(@Param("courseName") String courseName, @Param("userId") Long userId);
}
