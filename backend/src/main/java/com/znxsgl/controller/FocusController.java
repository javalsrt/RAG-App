package com.znxsgl.controller;

import com.znxsgl.entity.FocusSession;
import com.znxsgl.mapper.FocusSessionMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/focus")
public class FocusController {

    private final FocusSessionMapper mapper;
    private final JdbcTemplate jdbc;

    public FocusController(FocusSessionMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    // 学生：保存专注记录
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> save(
            @RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        int seconds = ((Number) body.get("durationSeconds")).intValue();
        String startedStr = (String) body.get("startedAt");
        String finishedStr = (String) body.get("finishedAt");

        FocusSession s = new FocusSession();
        s.setUserId(userId);
        s.setDurationSeconds(seconds);
        s.setStartedAt(startedStr != null ? LocalDateTime.parse(startedStr) : LocalDateTime.now().minusSeconds(seconds));
        s.setFinishedAt(finishedStr != null ? LocalDateTime.parse(finishedStr) : LocalDateTime.now());
        s.setCreatedAt(LocalDateTime.now());
        mapper.insert(s);

        int todayTotal = mapper.todayTotalSeconds(userId);
        return ResponseEntity.ok(Map.of("id", s.getId(), "todayTotal", todayTotal));
    }

    // 学生：今日专注总时长
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> todayTotal(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(Map.of("totalSeconds", mapper.todayTotalSeconds(userId)));
    }

    // 学生：累计专注总时长
    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> total(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        int secs = mapper.totalSeconds(userId);
        double hours = Math.round(secs / 36.0) / 100.0; // 转为小时，保留2位小数
        return ResponseEntity.ok(Map.of("totalSeconds", secs, "totalHours", hours));
    }

    // 学生：获取上次专注时长
    @GetMapping("/last")
    public ResponseEntity<Map<String, Object>> lastSession(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT COALESCE(duration_seconds, 0) AS seconds FROM focus_session " +
                "WHERE user_id = ? ORDER BY finished_at DESC LIMIT 1", userId);
        if (rows.isEmpty()) return ResponseEntity.ok(Map.of("seconds", 0));
        return ResponseEntity.ok(rows.get(0));
    }

    // 学生：更新专注状态
    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String status = body.getOrDefault("status", "idle");
        jdbc.update("INSERT INTO student_status (user_id, status, last_active) VALUES (?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE status = ?, last_active = NOW()",
                userId, status, status);
        return ResponseEntity.ok(Map.of("status", status));
    }

    // 教师：查询某班级学生专注状态
    @GetMapping("/students/{classId}")
    public ResponseEntity<List<Map<String, Object>>> students(
            @PathVariable Long classId, Authentication auth) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT u.id, u.real_name, u.student_no, " +
                "COALESCE(s.status, 'idle') AS status, " +
                "s.last_active, " +
                "COALESCE(SUM(f.duration_seconds), 0) AS today_seconds " +
                "FROM user u " +
                "LEFT JOIN student_status s ON s.user_id = u.id " +
                "LEFT JOIN focus_session f ON f.user_id = u.id AND DATE(f.finished_at) = CURDATE() " +
                "WHERE u.class_id = ? AND u.role = 1 " +
                "GROUP BY u.id, u.real_name, u.student_no, s.status, s.last_active",
                classId);
        return ResponseEntity.ok(rows);
    }

    // 教师：某班级专注排行
    @GetMapping("/rank/{classId}")
    public ResponseEntity<List<Map<String, Object>>> rankByClass(
            @PathVariable Long classId, Authentication auth) {
        return ResponseEntity.ok(mapper.rankByClass(classId));
    }
}
