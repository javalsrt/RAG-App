package com.znxsgl.service;

import com.znxsgl.entity.Teacher;
import com.znxsgl.mapper.TeacherMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 课表导入时的教师姓名匹配器。
 * 支持精确匹配与基于编辑距离的模糊匹配，用于容错教师姓名错别字或简写。
 */
@Component
public class ScheduleImportTeacherMatcher {

    private final TeacherMapper teacherMapper;

    public ScheduleImportTeacherMatcher(TeacherMapper teacherMapper) {
        this.teacherMapper = teacherMapper;
    }

    /**
     * 根据输入姓名匹配系统中的教师。
     *
     * @param inputName 导入文件中提取的教师姓名
     * @return 匹配结果，包含状态与候选建议
     */
    public MatchResult match(String inputName) {
        String normalized = normalize(inputName);
        if (normalized.isEmpty()) {
            return MatchResult.unmatched("", Collections.emptyList());
        }

        // 1. 精确匹配
        Teacher exact = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getRealName, normalized));
        if (exact != null) {
            return MatchResult.matched(exact.getId(), exact.getRealName());
        }

        // 2. 模糊匹配：拉取所有教师计算编辑距离
        List<Teacher> allTeachers = teacherMapper.selectList(null);
        List<Suggestion> suggestions = new ArrayList<>();
        int threshold = fuzzyThreshold(normalized);
        for (Teacher t : allTeachers) {
            if (t.getRealName() == null || t.getRealName().trim().isEmpty()) continue;
            String dbName = normalize(t.getRealName());
            int distance = levenshtein(normalized, dbName);
            if (distance <= threshold && distance > 0) {
                suggestions.add(new Suggestion(t.getId(), t.getRealName(), distance));
            }
        }

        // 按编辑距离升序，距离相同按姓名长度升序（优先更短的，即更可能的）
        suggestions.sort(Comparator.comparingInt(Suggestion::getDistance)
                .thenComparingInt(s -> s.getTeacherName().length()));

        // 如果有唯一最近且距离为 1，直接视为模糊命中
        if (!suggestions.isEmpty() && suggestions.get(0).getDistance() == 1) {
            Suggestion best = suggestions.get(0);
            return MatchResult.fuzzy(best.getTeacherId(), best.getTeacherName(), suggestions);
        }

        return MatchResult.unmatched(inputName, suggestions);
    }

    /**
     * 获取所有教师，供前端下拉选择。
     */
    public List<Teacher> listAllTeachers() {
        return teacherMapper.selectList(null);
    }

    private String normalize(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("\\s+", "");
    }

    /**
     * 中文姓名容错阈值：2-4 字姓名，错别字通常为 1 个字符差异。
     */
    private int fuzzyThreshold(String name) {
        int len = name.length();
        if (len <= 2) return 1;
        if (len <= 4) return 1;
        return Math.max(1, len / 3);
    }

    /**
     * 计算两个字符串的 Levenshtein 编辑距离。
     */
    private int levenshtein(String a, String b) {
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    /**
     * 教师匹配结果。
     */
    public static class MatchResult {
        /** 匹配到的教师ID，未匹配时为 null */
        private final Long teacherId;
        /** 匹配到的教师姓名 */
        private final String teacherName;
        /** 匹配状态：matched 精确匹配 / fuzzy 模糊匹配 / unmatched 未匹配 */
        private final String status;
        /** 候选建议列表（按相似度排序） */
        private final List<Suggestion> suggestions;

        public MatchResult(Long teacherId, String teacherName, String status, List<Suggestion> suggestions) {
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.status = status;
            this.suggestions = suggestions;
        }

        public static MatchResult matched(Long teacherId, String teacherName) {
            return new MatchResult(teacherId, teacherName, "matched",
                    Collections.singletonList(new Suggestion(teacherId, teacherName, 0)));
        }

        public static MatchResult fuzzy(Long teacherId, String teacherName, List<Suggestion> suggestions) {
            return new MatchResult(teacherId, teacherName, "fuzzy", suggestions);
        }

        public static MatchResult unmatched(String inputName, List<Suggestion> suggestions) {
            return new MatchResult(null, inputName, "unmatched", suggestions);
        }

        public Long getTeacherId() { return teacherId; }
        public String getTeacherName() { return teacherName; }
        public String getStatus() { return status; }
        public List<Suggestion> getSuggestions() { return suggestions; }
    }

    /**
     * 候选教师建议。
     */
    public static class Suggestion {
        private final Long teacherId;
        private final String teacherName;
        private final int distance;

        public Suggestion(Long teacherId, String teacherName, int distance) {
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.distance = distance;
        }

        public Long getTeacherId() { return teacherId; }
        public String getTeacherName() { return teacherName; }
        public int getDistance() { return distance; }
    }
}
