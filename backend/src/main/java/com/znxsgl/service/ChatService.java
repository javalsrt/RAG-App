package com.znxsgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.znxsgl.converter.ChatMessageConverter;
import com.znxsgl.dto.ChatMessageDTO;
import com.znxsgl.dto.StudentAskStatsDTO;
import com.znxsgl.entity.ChatMessage;
import com.znxsgl.entity.Course;
import com.znxsgl.entity.User;
import com.znxsgl.mapper.ChatMessageMapper;
import com.znxsgl.mapper.CourseMapper;
import com.znxsgl.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private final ChatMessageMapper chatMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final ChatMessageConverter chatMessageConverter;

    public ChatService(ChatMessageMapper chatMapper, UserMapper userMapper,
                       CourseMapper courseMapper, ChatMessageConverter chatMessageConverter) {
        this.chatMapper = chatMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.chatMessageConverter = chatMessageConverter;
    }

    // 获取课程聊天记录（学生看：自己的消息 + @自己的消息 + 公开教师消息 + AI回复）
    public List<ChatMessageDTO> getMessages(String courseName, Long userId) {
        List<ChatMessage> msgs = chatMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getCourseName, courseName)
                        .and(w -> w.eq(ChatMessage::getUserId, userId)
                                .or().isNull(ChatMessage::getMentionUserId)
                                .or().eq(ChatMessage::getMentionUserId, userId)
                                .or().eq(ChatMessage::getSenderRole, "ai"))
                        .orderByAsc(ChatMessage::getCreatedAt));
        return chatMessageConverter.toDtoList(msgs);
    }

    // 获取课程公开聊天（教师端看全部，AI消息显示"AI对{学生}说"）
    public List<ChatMessageDTO> getPublicMessages(String courseName) {
        List<ChatMessage> msgs = chatMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getCourseName, courseName)
                        .orderByAsc(ChatMessage::getCreatedAt));
        return msgs.stream().map(m -> {
            ChatMessageDTO d = chatMessageConverter.toDto(m);
            if ("ai".equals(m.getSenderRole())) {
                User student = userMapper.selectById(m.getUserId());
                String studentName = student != null ? student.getRealName() : "学生";
                d.setSenderName("AI对" + studentName + "说");
            }
            return d;
        }).toList();
    }

    // 发送消息（支持 @mention）
    public ChatMessageDTO sendMessage(String courseName, Long userId, String content, String senderRole) {
        return sendMessage(courseName, userId, content, senderRole, null);
    }

    /** 发送消息，支持 @mention；createdAt 由 MyBatis-Plus 自动填充 */
    public ChatMessageDTO sendMessage(String courseName, Long userId, String content, String senderRole, Long mentionUserId) {
        User user = userMapper.selectById(userId);

        ChatMessage msg = new ChatMessage();
        msg.setCourseName(courseName);
        msg.setUserId(userId);
        msg.setSenderName(user != null ? user.getRealName() : "系统");
        msg.setSenderRole(senderRole);
        msg.setContent(content);
        msg.setMentionUserId(mentionUserId);
        chatMapper.insert(msg);

        return chatMessageConverter.toDto(msg);
    }

    /** 解析 content 中的 @username，返回匹配的用户ID */
    public Long parseMention(String courseName, String content) {
        if (content == null) return null;
        Pattern p = Pattern.compile("@(\\S+)");
        Matcher m = p.matcher(content);
        if (m.find()) {
            String name = m.group(1);
            // 查找该课程班级中的学生
            List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .eq(User::getRealName, name)
                    .eq(User::getRole, 1));
            if (!users.isEmpty()) return users.get(0).getId();
        }
        return null;
    }

    /** 教师查看学生提问统计（验证教师拥有该课程） */
    public List<StudentAskStatsDTO> getAskStats(String courseName, Long teacherUserId, Long classId) {
        // 验证教师是否教这门课
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getCourseName, courseName)
                        .eq(Course::getTeacherId, teacherUserId));
        if (courses.isEmpty()) {
            throw new IllegalArgumentException("无权查看该课程的统计数据");
        }
        return chatMapper.countStudentAsks(courseName, classId);
    }

    /** 标记该学生在指定课程的所有教师消息为已读 */
    public void markAsRead(String courseName, Long userId) {
        chatMapper.markAsRead(courseName, userId);
    }
}
