package com.znxsgl.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znxsgl.entity.ExamHomework;
import com.znxsgl.entity.ExamQuestion;
import com.znxsgl.entity.ExamSubmission;
import com.znxsgl.entity.ExamSubmissionAnswer;
import com.znxsgl.mapper.ExamHomeworkMapper;
import com.znxsgl.mapper.ExamQuestionMapper;
import com.znxsgl.mapper.ExamSubmissionAnswerMapper;
import com.znxsgl.mapper.ExamSubmissionMapper;
import com.znxsgl.service.LlmService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生端：考试/作业作答
 */
@RestController
@RequestMapping("/api/exam-homework/student")
@PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
public class ExamHomeworkStudentController {

    private final ExamHomeworkMapper examHomeworkMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamSubmissionMapper examSubmissionMapper;
    private final ExamSubmissionAnswerMapper examSubmissionAnswerMapper;
    private final LlmService llmService;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public ExamHomeworkStudentController(ExamHomeworkMapper examHomeworkMapper,
                                         ExamQuestionMapper examQuestionMapper,
                                         ExamSubmissionMapper examSubmissionMapper,
                                         ExamSubmissionAnswerMapper examSubmissionAnswerMapper,
                                         LlmService llmService,
                                         JdbcTemplate jdbc) {
        this.examHomeworkMapper = examHomeworkMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examSubmissionMapper = examSubmissionMapper;
        this.examSubmissionAnswerMapper = examSubmissionAnswerMapper;
        this.llmService = llmService;
        this.jdbc = jdbc;
    }

    /** 获取学生各课程下的待办考试/作业 */
    @GetMapping("/course-todos")
    public ResponseEntity<List<Map<String, Object>>> courseTodos(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();

        // 查询学生所在班级的所有未结束考试作业（含未到开始时间的，都显示），关联到课程
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT e.id, e.type, e.title, e.course_id AS courseId, e.start_time AS startTime, e.end_time AS endTime, " +
                "       e.total_score AS totalScore, e.question_count AS questionCount, e.time_limit AS timeLimit, " +
                "       s.status AS submitStatus, s.total_score AS score, s.id AS submissionId, s.started_at AS startedAt\n" +
                "FROM exam_homework e\n" +
                "JOIN user u ON u.class_id = e.class_id\n" +
                "LEFT JOIN exam_submission s ON s.exam_homework_id = e.id AND s.user_id = u.id\n" +
                "WHERE u.id = ? AND e.status = 1 AND e.end_time > NOW()\n" +
                "ORDER BY CASE WHEN e.start_time <= NOW() THEN 0 ELSE 1 END, e.end_time", userId);

        // 规范化 submitStatus：0=未开始/未作答 1=已开始但未提交 2=已提交
        for (Map<String, Object> row : rows) {
            Object status = row.get("submitStatus");
            if (status == null) {
                row.put("submitStatus", 0);
                row.put("statusText", "未作答");
            } else if ("completed".equals(status)) {
                row.put("submitStatus", 2);
                Object score = row.get("score");
                row.put("statusText", "已完成" + (score != null ? " · " + score + "分" : ""));
            } else {
                row.put("submitStatus", 1);
                row.put("statusText", "继续作答");
            }
        }

        return ResponseEntity.ok(rows);
    }

    /**
     * 获取试卷详情（不一定可作答）。
     * 返回字段：available=false 表示不在可作答时间内（未开始/已结束），
     * 此时只返回元信息（title/description/startTime/endTime/题型数量等），
     * 不返回具体题目内容，保护试题；available=true 时返回题目并允许作答。
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaper(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamHomework exam = examHomeworkMapper.selectById(id);
        if (exam == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "考试/作业不存在"));
        }
        if (!isStudentInClass(exam.getClassId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限查看"));
        }
        LocalDateTime now = LocalDateTime.now();
        boolean available = exam.getStatus() == 1
                && !now.isBefore(exam.getStartTime())
                && !now.isAfter(exam.getEndTime());
        boolean ended = exam.getStatus() == 2 || now.isAfter(exam.getEndTime());
        boolean notStarted = exam.getStatus() == 1 && now.isBefore(exam.getStartTime());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", exam.getId());
        result.put("type", exam.getType());
        result.put("title", exam.getTitle());
        result.put("description", exam.getDescription());
        result.put("timeLimit", exam.getTimeLimit());
        result.put("totalScore", exam.getTotalScore());
        result.put("questionCount", exam.getQuestionCount());
        result.put("startTime", exam.getStartTime() != null ? exam.getStartTime().toString() : "");
        result.put("endTime", exam.getEndTime() != null ? exam.getEndTime().toString() : "");
        result.put("available", available);
        result.put("ended", ended);
        result.put("notStarted", notStarted);

        ExamSubmission submission = examSubmissionMapper.findByExamAndUser(id, userId);
        boolean started = submission != null && submission.getStartedAt() != null;
        boolean isSubmitted = submission != null && "completed".equals(submission.getStatus());
        result.put("started", started);
        result.put("startedAt", started && submission.getStartedAt() != null ? submission.getStartedAt().toString() : null);
        result.put("submitStatus",
                submission == null ? 0 : (isSubmitted ? 2 : 1));
        result.put("isSubmitted", isSubmitted);
        result.put("score", isSubmitted && submission.getTotalScore() != null ? submission.getTotalScore() : null);
        result.put("submittedAt", isSubmitted && submission.getSubmittedAt() != null ? submission.getSubmittedAt().toString() : null);
        result.put("autoScore", isSubmitted && submission.getAutoScore() != null ? submission.getAutoScore() : null);

        // 只有在可作答时间内才返回真实题目内容，避免泄题
        if (!available) {
            String reason;
            if (isSubmitted) {
                reason = "你已完成" + ("exam".equals(exam.getType()) ? "考试" : "作业") + "，" +
                        (submission.getTotalScore() != null ? "得分：" + submission.getTotalScore() + " 分" : "等待评分中") + "。";
            } else if (notStarted) {
                reason = "尚未到开始时间（" + exam.getStartTime() + "），请耐心等待。";
            } else if (ended) {
                reason = "考试/作业已结束，未提交。";
            } else {
                reason = "当前状态不允许作答。";
            }
            result.put("reason", reason);
            result.put("questions", Collections.emptyList());
            result.put("savedAnswers", Collections.emptyList());
            return ResponseEntity.ok(result);
        }

        List<ExamQuestion> questions = examQuestionMapper.findByExamHomeworkId(id);
        List<Map<String, Object>> safeQuestions = new ArrayList<>();
        for (ExamQuestion q : questions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("index", q.getQuestionIndex());
            m.put("type", q.getQuestionType());
            m.put("content", q.getContent());
            m.put("options", parseOptions(q.getOptions()));
            m.put("score", q.getScore());
            m.put("difficulty", q.getDifficulty());
            safeQuestions.add(m);
        }
        result.put("questions", safeQuestions);

        // 如果已经开始，返回已保存的答案
        if (started) {
            List<Map<String, Object>> answers = new ArrayList<>();
            List<ExamSubmissionAnswer> exist = examSubmissionAnswerMapper.findBySubmissionId(submission.getId());
            for (ExamSubmissionAnswer a : exist) {
                answers.add(Map.of("questionId", a.getQuestionId(), "answer", safeStr(a.getUserAnswer())));
            }
            result.put("savedAnswers", answers);
        } else {
            result.put("savedAnswers", Collections.emptyList());
        }

        return ResponseEntity.ok(result);
    }

    /** 开始考试/作业 */
    @PostMapping("/{id}/start")
    @Transactional
    public ResponseEntity<Map<String, Object>> start(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamHomework exam = examHomeworkMapper.selectById(id);
        if (exam == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "不存在"));
        }
        if (!isStudentInClass(exam.getClassId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStatus() != 1 || now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "不在可作答时间范围内"));
        }

        ExamSubmission exist = examSubmissionMapper.findByExamAndUser(id, userId);
        if (exist != null) {
            return ResponseEntity.ok(Map.of("submissionId", exist.getId(), "msg", "已创建作答记录"));
        }

        ExamSubmission submission = new ExamSubmission();
        submission.setExamHomeworkId(id);
        submission.setUserId(userId);
        submission.setStatus("pending");
        submission.setStartedAt(now);
        examSubmissionMapper.insert(submission);

        return ResponseEntity.ok(Map.of("submissionId", submission.getId(), "msg", "开始成功"));
    }

    /** 保存作答进度（仅未提交状态） */
    @PostMapping("/{id}/save-progress")
    @Transactional
    public ResponseEntity<Map<String, Object>> saveProgress(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> body,
                                                            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamSubmission submission = ensureSubmission(id, userId);
        if (submission == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "尚未开始作答"));
        }
        if ("completed".equals(submission.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "已提交，不能修改"));
        }

        saveAnswers(submission.getId(), body.get("answers"), false);
        return ResponseEntity.ok(Map.of("msg", "保存成功"));
    }

    /** 提交答卷 */
    @PostMapping("/{id}/submit")
    @Transactional
    public ResponseEntity<Map<String, Object>> submit(@PathVariable Long id,
                                                      @RequestBody Map<String, Object> body,
                                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamHomework exam = examHomeworkMapper.selectById(id);
        if (exam == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "不存在"));
        }
        if (!isStudentInClass(exam.getClassId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }

        ExamSubmission submission = ensureSubmission(id, userId);
        if (submission == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "尚未开始作答"));
        }
        if ("completed".equals(submission.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "已提交，不能重复提交"));
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(exam.getEndTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "已超过截止时间"));
        }
        if (exam.getTimeLimit() != null && exam.getTimeLimit() > 0 && submission.getStartedAt() != null) {
            long minutes = ChronoUnit.SECONDS.between(submission.getStartedAt(), now) / 60;
            if (minutes > exam.getTimeLimit()) {
                return ResponseEntity.badRequest().body(Map.of("error", "已超过考试限时"));
            }
        }

        // 保存答案并判分
        List<ExamQuestion> questions = examQuestionMapper.findByExamHomeworkId(id);
        Map<Long, ExamQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        List<ExamSubmissionAnswer> saved = saveAnswers(submission.getId(), body.get("answers"), true);

        int totalScore = 0;
        for (ExamSubmissionAnswer a : saved) {
            ExamQuestion q = qMap.get(a.getQuestionId());
            if (q == null) continue;

            int score = 0;
            boolean isCorrect = false;
            String type = q.getQuestionType();
            String userAnswer = safeStr(a.getUserAnswer()).trim();
            String correctAnswer = safeStr(q.getAnswer()).trim();

            switch (type) {
                case "single_choice", "true_false" -> {
                    isCorrect = userAnswer.equalsIgnoreCase(correctAnswer);
                    score = isCorrect ? q.getScore() : 0;
                }
                case "multiple_choice" -> {
                    isCorrect = compareMultipleChoice(userAnswer, correctAnswer);
                    score = isCorrect ? q.getScore() : 0;
                }
                case "fill_blank" -> {
                    isCorrect = userAnswer.equalsIgnoreCase(correctAnswer);
                    score = isCorrect ? q.getScore() : 0;
                }
                case "short_answer" -> {
                    // AI 评分
                    int[] aiResult = scoreShortAnswer(q.getContent(), correctAnswer, userAnswer, q.getScore());
                    score = aiResult[0];
                    a.setAiScore(score);
                    a.setAiComment(aiResult[1] > 0 ? "AI评分完成" : "AI评分失败，按0分计");
                }
            }

            a.setCorrectAnswer(correctAnswer);
            a.setIsCorrect(isCorrect ? 1 : 0);
            a.setScore(score);
            examSubmissionAnswerMapper.updateById(a);
            totalScore += score;
        }

        // 未答题目记为跳过
        int answeredCount = saved.size();
        for (ExamQuestion q : questions) {
            boolean answered = saved.stream().anyMatch(a -> a.getQuestionId().equals(q.getId()));
            if (!answered) {
                ExamSubmissionAnswer skip = new ExamSubmissionAnswer();
                skip.setSubmissionId(submission.getId());
                skip.setQuestionId(q.getId());
                skip.setQuestionIndex(q.getQuestionIndex());
                skip.setQuestionType(q.getQuestionType());
                skip.setQuestion(q.getContent());
                skip.setOptions(q.getOptions());
                skip.setUserAnswer("");
                skip.setCorrectAnswer(q.getAnswer());
                skip.setIsCorrect(-2); // 跳过
                skip.setScore(0);
                examSubmissionAnswerMapper.insert(skip);
            }
        }

        submission.setStatus("completed");
        submission.setTotalScore(totalScore);
        submission.setSubmittedAt(now);
        if (submission.getStartedAt() != null) {
            submission.setDurationSec((int) ChronoUnit.SECONDS.between(submission.getStartedAt(), now));
        }
        examSubmissionMapper.updateById(submission);

        return ResponseEntity.ok(Map.of(
                "submissionId", submission.getId(),
                "totalScore", totalScore,
                "total", exam.getTotalScore(),
                "passScore", exam.getPassScore(),
                "passed", totalScore >= exam.getPassScore(),
                "msg", "提交成功"
        ));
    }

    /** 查看结果 */
    @GetMapping("/{id}/result")
    public ResponseEntity<Map<String, Object>> result(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamHomework exam = examHomeworkMapper.selectById(id);
        if (exam == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "不存在"));
        }
        if (!isStudentInClass(exam.getClassId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }

        ExamSubmission submission = examSubmissionMapper.findByExamAndUser(id, userId);
        if (submission == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "未开始作答"));
        }

        List<ExamSubmissionAnswer> answers = examSubmissionAnswerMapper.findBySubmissionId(submission.getId());
        List<Map<String, Object>> detail = new ArrayList<>();
        for (ExamSubmissionAnswer a : answers) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", a.getQuestionIndex());
            m.put("type", a.getQuestionType());
            m.put("question", a.getQuestion());
            m.put("options", parseOptions(a.getOptions()));
            m.put("userAnswer", a.getUserAnswer());
            m.put("correctAnswer", a.getCorrectAnswer());
            m.put("isCorrect", a.getIsCorrect());
            m.put("score", a.getScore());
            m.put("aiComment", a.getAiComment());
            detail.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", exam.getTitle());
        result.put("type", exam.getType());
        result.put("totalScore", exam.getTotalScore());
        result.put("passScore", exam.getPassScore());
        result.put("score", submission.getTotalScore());
        result.put("passed", submission.getTotalScore() >= exam.getPassScore());
        result.put("durationSec", submission.getDurationSec());
        result.put("submittedAt", submission.getSubmittedAt());
        result.put("detail", detail);
        return ResponseEntity.ok(result);
    }

    // ===== 辅助方法 =====

    private ExamSubmission ensureSubmission(Long examId, Long userId) {
        ExamSubmission s = examSubmissionMapper.findByExamAndUser(examId, userId);
        if (s == null) {
            ExamHomework exam = examHomeworkMapper.selectById(examId);
            if (exam == null || !isStudentInClass(exam.getClassId(), userId)) return null;
            s = new ExamSubmission();
            s.setExamHomeworkId(examId);
            s.setUserId(userId);
            s.setStatus("pending");
            s.setStartedAt(LocalDateTime.now());
            examSubmissionMapper.insert(s);
        }
        return s;
    }

    private List<ExamSubmissionAnswer> saveAnswers(Long submissionId, Object answersObj, boolean requireSnapshot) {
        List<ExamSubmissionAnswer> saved = new ArrayList<>();
        if (answersObj == null) return saved;
        try {
            List<Map<String, Object>> list = json.readValue(json.writeValueAsString(answersObj), List.class);
            for (Map<String, Object> m : list) {
                Long questionId = toLong(m.get("questionId"));
                String answer = safeStr(m.get("answer"));
                if (questionId == null) continue;

                ExamSubmissionAnswer exist = findExistingAnswer(submissionId, questionId);
                if (exist != null) {
                    exist.setUserAnswer(answer);
                    examSubmissionAnswerMapper.updateById(exist);
                    saved.add(exist);
                } else {
                    ExamSubmissionAnswer a = new ExamSubmissionAnswer();
                    a.setSubmissionId(submissionId);
                    a.setQuestionId(questionId);
                    a.setUserAnswer(answer);
                    // 无论是否提交，都补全题号等字段，避免 question_index NOT NULL 无默认值导致插入失败
                    ExamQuestion q = examQuestionMapper.selectById(questionId);
                    if (q != null) {
                        a.setQuestionIndex(q.getQuestionIndex());
                        a.setQuestionType(q.getQuestionType());
                        if (requireSnapshot) {
                            a.setQuestion(q.getContent());
                            a.setOptions(q.getOptions());
                        }
                    } else {
                        a.setQuestionIndex(a.getQuestionIndex() != null ? a.getQuestionIndex() : 0);
                    }
                    examSubmissionAnswerMapper.insert(a);
                    saved.add(a);
                }
            }
        } catch (Exception e) {
            System.out.println("=== 保存答案失败: " + e.getMessage());
        }
        return saved;
    }

    private ExamSubmissionAnswer findExistingAnswer(Long submissionId, Long questionId) {
        List<ExamSubmissionAnswer> list = examSubmissionAnswerMapper.findBySubmissionId(submissionId);
        return list.stream().filter(a -> a.getQuestionId().equals(questionId)).findFirst().orElse(null);
    }

    private boolean isStudentInClass(Long classId, Long userId) {
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE id = ? AND class_id = ? AND role = 1",
                    Integer.class, userId, classId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean compareMultipleChoice(String user, String correct) {
        Set<String> u = Arrays.stream(user.split("[,，;；]"))
                .map(String::trim).filter(s -> !s.isEmpty()).map(String::toUpperCase).collect(Collectors.toSet());
        Set<String> c = Arrays.stream(correct.split("[,，;；]"))
                .map(String::trim).filter(s -> !s.isEmpty()).map(String::toUpperCase).collect(Collectors.toSet());
        return u.equals(c);
    }

    private int[] scoreShortAnswer(String question, String correctAnswer, String userAnswer, int fullScore) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return new int[]{0, 1};
        }
        String prompt = String.format(
                "请对以下简答题进行评分。\n题目：%s\n参考答案：%s\n学生答案：%s\n" +
                "满分 %d 分。请根据要点覆盖程度给出一个 0-%d 的整数分数，并给出一句简短评语。\n" +
                "只输出纯JSON对象：{\"score\":整数,\"comment\":\"评语\"}",
                question, correctAnswer, userAnswer, fullScore, fullScore);
        try {
            String raw = llmService.chat("你是资深教师，擅长主观题评分。", prompt);
            if (raw == null) return new int[]{0, -1};
            String clean = raw.trim();
            if (clean.startsWith("```")) {
                int s = clean.indexOf("{"), e = clean.lastIndexOf("}");
                if (s >= 0 && e > s) clean = clean.substring(s, e + 1);
            }
            JsonNode node = json.readTree(clean);
            int score = node.path("score").asInt(0);
            if (score < 0) score = 0;
            if (score > fullScore) score = fullScore;
            return new int[]{score, 1};
        } catch (Exception e) {
            System.out.println("=== 简答题AI评分失败: " + e.getMessage());
            return new int[]{0, -1};
        }
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isEmpty()) return new ArrayList<>();
        try {
            return json.readValue(optionsJson, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.valueOf(obj.toString()); } catch (Exception e) { return null; }
    }

    private String safeStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }
}
