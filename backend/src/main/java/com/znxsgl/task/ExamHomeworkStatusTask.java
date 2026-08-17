package com.znxsgl.task;

import com.znxsgl.service.ExamHomeworkNotifyService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 考试作业状态定时任务：自动发布与自动结束
 * 定时发布到点时，触发通知给班级学生（带去重）。
 */
@Component
public class ExamHomeworkStatusTask {

    private final JdbcTemplate jdbc;
    private final ExamHomeworkNotifyService notifyService;

    public ExamHomeworkStatusTask(JdbcTemplate jdbc, ExamHomeworkNotifyService notifyService) {
        this.jdbc = jdbc;
        this.notifyService = notifyService;
    }

    /** 每分钟执行一次 */
    @Scheduled(fixedRate = 60_000)
    public void updateStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 定时发布的草稿到点开始：先查出要发布的记录，再UPDATE，最后逐个通知（带去重）
        List<Map<String, Object>> pending = jdbc.queryForList(
                "SELECT id, class_id, course_id, title, type FROM exam_homework " +
                        "WHERE status = 0 AND publish_mode = 'scheduled' AND scheduled_time <= ?",
                now);
        if (!pending.isEmpty()) {
            int updated = jdbc.update(
                    "UPDATE exam_homework SET status = 1 " +
                            "WHERE status = 0 AND publish_mode = 'scheduled' AND scheduled_time <= ?",
                    now);
            System.out.println("=== 定时发布考试作业 " + updated + " 条，开始发送通知...");
            for (Map<String, Object> row : pending) {
                Long id = ((Number) row.get("id")).longValue();
                Long classId = row.get("class_id") != null ? ((Number) row.get("class_id")).longValue() : null;
                Long courseId = row.get("course_id") != null ? ((Number) row.get("course_id")).longValue() : null;
                String title = (String) row.get("title");
                String type = (String) row.get("type");
                System.out.println("[ExamNotify] 定时任务调用通知: examId=" + id + ", classId=" + classId + ", title=" + title);
                notifyService.notifyStudents(id, classId, courseId, title, type);
            }
        }

        // 2. 进行中的考试到点结束
        int ended = jdbc.update(
                "UPDATE exam_homework SET status = 2 " +
                        "WHERE status = 1 AND end_time <= ?",
                now);
        if (ended > 0) {
            System.out.println("=== 自动结束考试作业 " + ended + " 条");
        }
    }
}
