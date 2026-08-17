package com.znxsgl.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 章节阅读进度服务：记录学生已完成的课时，用于自适应出题前置条件判断
 */
@Service
public class ChapterProgressService {

    private final JdbcTemplate jdbc;

    public ChapterProgressService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 标记课时完成：先查 lesson 拿 chapterId/courseId，再 INSERT IGNORE
     */
    public boolean markComplete(Long userId, Long lessonId) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT l.chapter_id AS chapterId, c.course_id AS courseId " +
                "FROM course_lesson l JOIN course_chapter c ON l.chapter_id = c.id " +
                "WHERE l.id = ?", lessonId);
        if (row == null || row.isEmpty()) {
            throw new RuntimeException("课时不存在");
        }
        Long chapterId = ((Number) row.get("chapterId")).longValue();
        Long courseId = ((Number) row.get("courseId")).longValue();
        jdbc.update("INSERT IGNORE INTO chapter_read_progress(user_id, course_id, chapter_id, lesson_id, completed_at) " +
                "VALUES(?,?,?,?,NOW())", userId, courseId, chapterId, lessonId);
        return true;
    }

    /**
     * 返回已完成的 lessonId 列表
     */
    public List<Long> getCompletedLessons(Long userId, Long courseId) {
        return jdbc.queryForList(
                "SELECT lesson_id FROM chapter_read_progress WHERE user_id = ? AND course_id = ?",
                Long.class, userId, courseId);
    }

    /**
     * 返回已完成的 chapterId 去重列表
     */
    public List<Long> getCompletedChapterIds(Long userId, Long courseId) {
        return jdbc.queryForList(
                "SELECT DISTINCT chapter_id FROM chapter_read_progress WHERE user_id = ? AND course_id = ?",
                Long.class, userId, courseId);
    }

    /**
     * 是否有任何已完成课时（满足出题前置条件）
     */
    public boolean canQuiz(Long userId, Long courseId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM chapter_read_progress WHERE user_id = ? AND course_id = ? LIMIT 1",
                Integer.class, userId, courseId);
        return count != null && count > 0;
    }
}
