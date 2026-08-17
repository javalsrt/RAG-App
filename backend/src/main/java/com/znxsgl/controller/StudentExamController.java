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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/exam")
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
public class StudentExamController {

    private final ExamHomeworkMapper examHomeworkMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final ExamSubmissionAnswerMapper answerMapper;
    private final LlmService llmService;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public StudentExamController(ExamHomeworkMapper examHomeworkMapper,
                                  ExamQuestionMapper examQuestionMapper,
                                  ExamSubmissionMapper submissionMapper,
                                  ExamSubmissionAnswerMapper answerMapper,
                                  LlmService llmService,
                                  JdbcTemplate jdbc) {
        this.examHomeworkMapper = examHomeworkMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.submissionMapper = submissionMapper;
        this.answerMapper = answerMapper;
        this.llmService = llmService;
        this.jdbc = jdbc;
    }

    @GetMapping("/{examId}")
    public ResponseEntity<Map<String, Object>> getExam(@PathVariable Long examId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamHomework exam = examHomeworkMapper.selectById(examId);
        if (exam == null) return ResponseEntity.badRequest().body(Map.of("error", "考试不存在"));
        if (exam.getStatus() == null || exam.getStatus() < 1)
            return ResponseEntity.badRequest().body(Map.of("error", "考试尚未发布"));

        Long classId = jdbc.queryForObject(
            "SELECT class_id FROM user WHERE id = ?", Long.class, userId);
        boolean isTeacher = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()) || "TEACHER".equals(a.getAuthority()));
        if (!isTeacher && (classId == null || !classId.equals(exam.getClassId())))
            return ResponseEntity.status(403).body(Map.of("error", "无权访问此考试"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()))
            return ResponseEntity.badRequest().body(Map.of("error", "考试尚未开始"));

        List<ExamQuestion> questions = examQuestionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamQuestion>()
                .eq("exam_homework_id", examId).orderByAsc("question_index", "id"));

        List<Map<String, Object>> qList = new ArrayList<>();
        for (ExamQuestion q : questions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("type", q.getQuestionType());
            m.put("content", q.getContent());
            m.put("options", parseOptions(q.getOptions()));
            m.put("score", q.getScore());
            m.put("difficulty", q.getDifficulty());
            qList.add(m);
        }

        Map<String, Object> submissionStatus = null;
        try {
            ExamSubmission existing = submissionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamSubmission>()
                    .eq("exam_homework_id", examId).eq("user_id", userId).last("LIMIT 1"));
            if (existing != null) {
                submissionStatus = new LinkedHashMap<>();
                submissionStatus.put("submissionId", existing.getId());
                submissionStatus.put("status", existing.getStatus());
                submissionStatus.put("totalScore", existing.getTotalScore());
                submissionStatus.put("autoScore", existing.getAutoScore());
            }
        } catch (Exception ignored) {}

        Map<String, Object> examInfo = new LinkedHashMap<>();
        examInfo.put("id", exam.getId());
        examInfo.put("type", exam.getType());
        examInfo.put("title", exam.getTitle());
        examInfo.put("description", exam.getDescription());
        examInfo.put("startTime", exam.getStartTime());
        examInfo.put("endTime", exam.getEndTime());
        examInfo.put("timeLimit", exam.getTimeLimit());
        examInfo.put("totalScore", exam.getTotalScore());
        examInfo.put("passScore", exam.getPassScore());
        examInfo.put("questionCount", exam.getQuestionCount());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exam", examInfo);
        result.put("questions", qList);
        result.put("existingSubmission", submissionStatus);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{examId}/submit")
    @Transactional
    public ResponseEntity<Map<String, Object>> submit(@PathVariable Long examId,
                                                       @RequestBody Map<String, Object> body,
                                                       Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ExamHomework exam = examHomeworkMapper.selectById(examId);
        if (exam == null) return ResponseEntity.badRequest().body(Map.of("error", "考试不存在"));
        Long classId = jdbc.queryForObject(
            "SELECT class_id FROM user WHERE id = ?", Long.class, userId);
        boolean isTeacherSub = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()) || "TEACHER".equals(a.getAuthority()));
        if (!isTeacherSub && (classId == null || !classId.equals(exam.getClassId())))
            return ResponseEntity.status(403).body(Map.of("error", "无权提交"));

        ExamSubmission exist = null;
        try {
            exist = submissionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamSubmission>()
                    .eq("exam_homework_id", examId).eq("user_id", userId).last("LIMIT 1"));
        } catch (Exception ignored) {}
        if (exist != null && "completed".equals(exist.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("error", "您已提交过此考试"));

        List<ExamQuestion> questions = examQuestionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamQuestion>()
                .eq("exam_homework_id", examId).orderByAsc("question_index", "id"));
        Map<Long, ExamQuestion> qMap = new LinkedHashMap<>();
        for (ExamQuestion q : questions) qMap.put(q.getId(), q);

        @SuppressWarnings("unchecked")
        Map<String, Object> answersRaw = (Map<String, Object>) body.getOrDefault("answers", Collections.emptyMap());
        Map<Long, String> userAnswers = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : answersRaw.entrySet()) {
            Long qid;
            try { qid = Long.parseLong(e.getKey()); } catch (Exception ex) { continue; }
            Object v = e.getValue();
            if (v == null) { userAnswers.put(qid, ""); continue; }
            if (v instanceof Collection<?> col) {
                List<String> parts = new ArrayList<>();
                for (Object o : col) if (o != null) parts.add(String.valueOf(o).trim());
                userAnswers.put(qid, String.join(",", parts));
            } else {
                userAnswers.put(qid, String.valueOf(v).trim());
            }
        }

        ExamSubmission submission = exist != null ? exist : new ExamSubmission();
        submission.setExamHomeworkId(examId);
        submission.setUserId(userId);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("completed");
        if (submission.getId() == null) submissionMapper.insert(submission);
        else submissionMapper.updateById(submission);
        Long submissionId = submission.getId();

        jdbc.update("DELETE FROM exam_submission_answer WHERE submission_id = ?", submissionId);

        // 简答题待AI评分列表
        List<Map<String, Object>> shortAnswerList = new ArrayList<>();
        int totalScore = 0;

        int qIndex = 0;
        for (ExamQuestion q : questions) {
            qIndex++;
            ExamSubmissionAnswer ans = new ExamSubmissionAnswer();
            ans.setSubmissionId(submissionId);
            ans.setQuestionId(q.getId());
            ans.setQuestionIndex(qIndex);
            ans.setQuestionType(q.getQuestionType());
            ans.setQuestion(q.getContent());
            ans.setOptions(q.getOptions());
            ans.setUserAnswer(userAnswers.getOrDefault(q.getId(), ""));
            ans.setCorrectAnswer(q.getAnswer());
            ans.setMaxScore(q.getScore());

            String correct = q.getAnswer() == null ? "" : q.getAnswer().trim();
            String userAns = ans.getUserAnswer();
            int scored = 0;

            switch (q.getQuestionType()) {
                case "single_choice":
                case "fill_blank":
                case "true_false":
                    if (correct.equalsIgnoreCase(userAns)) scored = q.getScore();
                    ans.setScore(scored);
                    ans.setIsCorrect(scored > 0 ? 1 : 0);
                    ans.setAiScore(scored);
                    ans.setScoreAdjustCount(0);
                    break;
                case "multiple_choice":
                    Set<String> cSet = Arrays.stream(correct.toLowerCase().split("\\s*,\\s*"))
                            .filter(s -> !s.isEmpty()).collect(Collectors.toCollection(TreeSet::new));
                    Set<String> uSet = Arrays.stream(userAns.toLowerCase().split("\\s*,\\s*"))
                            .filter(s -> !s.isEmpty()).collect(Collectors.toCollection(TreeSet::new));
                    if (!cSet.isEmpty() && cSet.equals(uSet)) scored = q.getScore();
                    else if (!uSet.isEmpty() && cSet.containsAll(uSet)) {
                        scored = (int) Math.round(q.getScore() * 0.4);
                    }
                    ans.setScore(scored);
                    ans.setIsCorrect(scored > 0 ? 1 : 0);
                    ans.setAiScore(scored);
                    ans.setScoreAdjustCount(0);
                    break;
                case "short_answer":
                    shortAnswerList.add(Map.of(
                        "questionId", q.getId(),
                        "content", q.getContent() == null ? "" : q.getContent(),
                        "reference", correct,
                        "userAnswer", userAns == null ? "" : userAns,
                        "maxScore", q.getScore(),
                        "answerEntity", ans
                    ));
                    break;
                default:
                    ans.setScore(0);
                    ans.setIsCorrect(0);
                    ans.setAiScore(0);
                    ans.setScoreAdjustCount(0);
            }
            if (!"short_answer".equals(q.getQuestionType())) {
                answerMapper.insert(ans);
                totalScore += scored;
            }
        }

        // AI 批量评简答题
        if (!shortAnswerList.isEmpty()) {
            int aiTotal = 0;
            try {
                String prompt = buildShortAnswerPrompt(shortAnswerList);
                String aiResp = llmService.chat(prompt,
                    "你是严格的试卷评分老师。仅输出JSON数组，不要任何解释文字。每个元素: {questionId, score, comment}。score必须是整数且不超过maxScore。");
                System.out.println("=== 简答题AI评分返回前500: " + (aiResp == null ? "null" :
                    aiResp.substring(0, Math.min(500, aiResp.length()))));
                JsonNode arr = null;
                try {
                    String clean = extractJsonArray(aiResp);
                    arr = json.readTree(clean);
                } catch (Exception pex) {
                    System.out.println("=== AI评分JSON解析失败: " + pex.getMessage());
                }
                Map<Long, JsonNode> scoredMap = new HashMap<>();
                if (arr != null && arr.isArray()) {
                    for (JsonNode n : arr) {
                        Long qid = n.has("questionId") ? n.path("questionId").asLong() : null;
                        if (qid != null) scoredMap.put(qid, n);
                    }
                }

                for (Map<String, Object> item : shortAnswerList) {
                    Long qid = (Long) item.get("questionId");
                    int maxScore = (int) item.get("maxScore");
                    ExamSubmissionAnswer ans = (ExamSubmissionAnswer) item.get("answerEntity");
                    int aiScore = 0;
                    String comment = "";
                    JsonNode node = scoredMap.get(qid);
                    if (node != null) {
                        aiScore = Math.min(maxScore, Math.max(0, node.path("score").asInt(0)));
                        comment = node.path("comment").asText("");
                    } else {
                        String ua = (String) item.get("userAnswer");
                        if (ua != null && ua.length() > 8) aiScore = maxScore / 2;
                    }
                    ans.setAiScore(aiScore);
                    ans.setScore(aiScore);
                    ans.setIsCorrect(aiScore > 0 ? 1 : 0);
                    ans.setAiComment(comment);
                    ans.setScoreAdjustCount(0);
                    answerMapper.insert(ans);
                    aiTotal += aiScore;
                }
            } catch (Exception ex) {
                System.out.println("=== 简答题AI评分异常，给兜底分数: " + ex.getMessage());
                for (Map<String, Object> item : shortAnswerList) {
                    ExamSubmissionAnswer ans = (ExamSubmissionAnswer) item.get("answerEntity");
                    int maxScore = (int) item.get("maxScore");
                    String ua = (String) item.get("userAnswer");
                    int fallback = (ua != null && ua.length() > 10) ? maxScore / 2 : 0;
                    ans.setAiScore(fallback);
                    ans.setScore(fallback);
                    ans.setIsCorrect(fallback > 0 ? 1 : 0);
                    ans.setAiComment("AI评分服务暂不可用，已给出参考分，教师可手动调整。");
                    ans.setScoreAdjustCount(0);
                    answerMapper.insert(ans);
                    aiTotal += fallback;
                }
            }
            totalScore += aiTotal;
        }

        submission.setAutoScore(totalScore);
        submission.setTotalScore(totalScore);
        submissionMapper.updateById(submission);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("submissionId", submissionId);
        res.put("totalScore", totalScore);
        res.put("autoScore", totalScore);
        res.put("msg", "提交成功");
        return ResponseEntity.ok(res);
    }

    private String buildShortAnswerPrompt(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下 ").append(items.size()).append(" 道简答题进行评分。严格按参考答案评分；若学生回答要点齐全、表述合理可给满分，要点不全给部分分，不相关给0分。\n");
        sb.append("仅输出JSON数组: [{\"questionId\": number, \"score\": number, \"comment\": \"简短评语\"}]\n\n");
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> it = items.get(i);
            sb.append(i + 1).append(". questionId=").append(it.get("questionId"))
              .append(", 满分=").append(it.get("maxScore")).append("分\n");
            sb.append("   题目: ").append(it.get("content")).append("\n");
            sb.append("   参考答案: ").append(it.get("reference")).append("\n");
            sb.append("   学生作答: ").append(it.get("userAnswer")).append("\n\n");
        }
        return sb.toString();
    }

    private String extractJsonArray(String s) {
        if (s == null) return "[]";
        int s1 = s.indexOf('[');
        int e1 = s.lastIndexOf(']');
        if (s1 >= 0 && e1 > s1) return s.substring(s1, e1 + 1);
        return s.trim();
    }

    private List<String> parseOptions(String opt) {
        if (opt == null || opt.isEmpty()) return Collections.emptyList();
        try {
            JsonNode n = json.readTree(opt);
            List<String> out = new ArrayList<>();
            if (n.isArray()) for (JsonNode x : n) out.add(x.asText());
            return out;
        } catch (Exception e) {
            return Arrays.stream(opt.split("[,\\n]"))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }
    }
}
