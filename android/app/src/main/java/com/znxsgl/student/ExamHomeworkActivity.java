package com.znxsgl.student;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.znxsgl.student.network.ApiService;
import com.znxsgl.student.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 学生考试/作业作答页
 */
public class ExamHomeworkActivity extends AppCompatActivity {

    private long examId;
    private String examTitle;
    private String examType;
    private String courseName;
    private String token;

    private TextView tvTitle, tvTimer, tvProgress;
    private ProgressBar progressBar;
    private ViewPager2 vpQuestions;
    private Button btnPrev, btnNext, btnSubmit;

    private final List<Map<String, Object>> questions = new ArrayList<>();
    private final Map<Long, String> answers = new HashMap<>();
    private QuestionAdapter adapter;

    private int timeLimit = 0;
    private long startedAt = 0;
    private long endTimeMillis = 0;
    private boolean hasStarted = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private Runnable saveRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_homework);

        examId = getIntent().getLongExtra("exam_id", 0);
        examTitle = getIntent().getStringExtra("exam_title");
        examType = getIntent().getStringExtra("exam_type");
        courseName = getIntent().getStringExtra("course_name");

        SharedPreferences prefs = getSharedPreferences("znxsgl", 0);
        token = "Bearer " + prefs.getString("token", "");

        initViews();
        loadPaper();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvTimer = findViewById(R.id.tv_timer);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        vpQuestions = findViewById(R.id.vp_questions);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnSubmit = findViewById(R.id.btn_submit);

        tvTitle.setText(examTitle != null ? examTitle : ("exam".equals(examType) ? "考试" : "作业"));
        findViewById(R.id.tv_back).setOnClickListener(v -> confirmExit());

        vpQuestions.setUserInputEnabled(false);
        adapter = new QuestionAdapter();
        vpQuestions.setAdapter(adapter);
        vpQuestions.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                updateProgress(pos);
            }
        });

        btnPrev.setOnClickListener(v -> moveTo(vpQuestions.getCurrentItem() - 1));
        btnNext.setOnClickListener(v -> moveTo(vpQuestions.getCurrentItem() + 1));
        btnSubmit.setOnClickListener(v -> confirmSubmit());
    }

    private void loadPaper() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("加载试卷中...");
        pd.setCancelable(false);
        pd.show();

        if (tvTitle != null && examTitle != null && !examTitle.isEmpty()) {
            tvTitle.setText(examTitle);
        }

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getExamHomeworkPaper(token, examId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                pd.dismiss();
                // HTTP 错误码：直接提示 + 不finish，让用户可点返回
                if (!resp.isSuccessful() || resp.body() == null) {
                    String msg = "加载失败：" + (resp.errorBody() != null ? resp.code() : "未知");
                    showUnavailableDialog(msg, true, true);
                    return;
                }
                Map<String, Object> body = resp.body();
                if (body.containsKey("error")) {
                    showUnavailableDialog(String.valueOf(body.get("error")), true, true);
                    return;
                }

                // 不可作答状态（未到开始时间/已结束）：弹窗说明，但保留页面显示考试信息
                Object availableObj = body.get("available");
                boolean available = availableObj == null || Boolean.TRUE.equals(availableObj);
                Object notStartedObj = body.get("notStarted");
                boolean notStarted = Boolean.TRUE.equals(notStartedObj);
                Object endedObj = body.get("ended");
                boolean ended = Boolean.TRUE.equals(endedObj);
                Object reasonObj = body.get("reason");
                Object titleObj = body.get("title");
                Object descObj = body.get("description");
                Object startTimeObj = body.get("startTime");
                Object endTimeObj = body.get("endTime");
                Object questionCountObj = body.get("questionCount");
                Object totalScoreObj = body.get("totalScore");
                Object timeLimitObj = body.get("timeLimit");

                StringBuilder info = new StringBuilder();
                if (titleObj != null) info.append(titleObj).append("\n\n");
                if (descObj != null && !String.valueOf(descObj).isEmpty()) {
                    info.append("说明：").append(descObj).append("\n\n");
                }
                info.append("题量：").append(questionCountObj == null ? "-" : questionCountObj).append(" 题\n");
                info.append("总分：").append(totalScoreObj == null ? "-" : totalScoreObj).append("\n");
                if (timeLimitObj instanceof Number && ((Number) timeLimitObj).intValue() > 0) {
                    info.append("时长：").append(timeLimitObj).append(" 分钟\n");
                }
                if (startTimeObj != null) info.append("开始：").append(startTimeObj).append("\n");
                if (endTimeObj != null) info.append("截止：").append(endTimeObj).append("\n");

                if (!available) {
                    parsePaperMeta(body);
                    // 按钮全部禁用
                    if (btnPrev != null) btnPrev.setEnabled(false);
                    if (btnNext != null) btnNext.setEnabled(false);
                    if (btnSubmit != null) btnSubmit.setEnabled(false);

                    // 已提交完成时，标题显示「已完成」，优先展示后端返回的 reason（含分数）
                    Object isSubmittedObj = body.get("isSubmitted");
                    boolean isSubmitted = Boolean.TRUE.equals(isSubmittedObj);
                    String reason = reasonObj != null ? String.valueOf(reasonObj)
                            : (isSubmitted ? "已完成作答" :
                            (notStarted ? "尚未到开始时间" : (ended ? "考试已结束" : "不可作答")));
                    String title = isSubmitted ? "已完成" :
                            (notStarted ? "尚未开考" : (ended ? "已结束" : "暂不可作答"));

                    // 不直接finish，弹窗提示 + 显示考试信息（用户可返回）
                    new AlertDialog.Builder(ExamHomeworkActivity.this)
                            .setTitle(title)
                            .setMessage(reason + "\n\n" + info)
                            .setCancelable(false)
                            .setPositiveButton("返回我的课程", (d, w) -> finish())
                            .show();
                    return;
                }

                parsePaper(body);
                if (!hasStarted) {
                    startExam();
                } else {
                    updateTimer();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                pd.dismiss();
                showUnavailableDialog("网络错误：" + t.getMessage(), true, true);
            }
        });
    }

    /** 仅解析元信息，不解析真实题目（available=false 时使用） */
    private void parsePaperMeta(Map<String, Object> body) {
        Object timeLimitObj = body.get("timeLimit");
        timeLimit = timeLimitObj instanceof Number ? ((Number) timeLimitObj).intValue() : 0;
        Object endTimeObj = body.get("endTime");
        if (endTimeObj != null) endTimeMillis = parseDateTime(String.valueOf(endTimeObj));
        questions.clear();
        answers.clear();
        if (adapter == null) {
            adapter = new QuestionAdapter();
            if (vpQuestions != null) vpQuestions.setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
        updateProgress(0);
    }

    private void showUnavailableDialog(String msg, boolean cancelable, boolean allowFinish) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(msg == null ? "无法加载" : msg)
                .setCancelable(cancelable)
                .setPositiveButton("返回", (d, w) -> finish())
                .setOnCancelListener(d -> { if (allowFinish) finish(); })
                .show();
    }

    @SuppressWarnings("unchecked")
    private void parsePaper(Map<String, Object> body) {
        Object startedObj = body.get("started");
        hasStarted = Boolean.TRUE.equals(startedObj);

        Object timeLimitObj = body.get("timeLimit");
        timeLimit = timeLimitObj instanceof Number ? ((Number) timeLimitObj).intValue() : 0;

        Object startedAtObj = body.get("startedAt");
        startedAt = parseTime(startedAtObj);

        Object endTimeObj = body.get("endTime");
        if (endTimeObj != null) {
            endTimeMillis = parseDateTime(String.valueOf(endTimeObj));
        }

        Object qs = body.get("questions");
        if (qs instanceof List) {
            questions.clear();
            questions.addAll((List<Map<String, Object>>) qs);
        }

        Object saved = body.get("savedAnswers");
        if (saved instanceof List) {
            for (Map<String, Object> m : (List<Map<String, Object>>) saved) {
                Object qid = m.get("questionId");
                Object ans = m.get("answer");
                if (qid != null && ans != null) {
                    answers.put(((Number) qid).longValue(), String.valueOf(ans));
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateProgress(0);
    }

    private void startExam() {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.startExamHomework(token, examId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    hasStarted = true;
                    startedAt = System.currentTimeMillis();
                    updateTimer();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateTimer() {
        if (timeLimit > 0) {
            mainHandler.post(timerRunnable);
        } else {
            tvTimer.setVisibility(View.GONE);
        }
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long remainSec;
            if (timeLimit > 0 && startedAt > 0) {
                long elapsedSec = (System.currentTimeMillis() - startedAt) / 1000;
                remainSec = timeLimit * 60L - elapsedSec;
                if (remainSec <= 0) {
                    autoSubmit();
                    return;
                }
            } else {
                remainSec = 0;
            }
            long min = remainSec / 60;
            long sec = remainSec % 60;
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            mainHandler.postDelayed(this, 1000);
        }
    };

    private void moveTo(int pos) {
        if (pos < 0 || pos >= questions.size()) return;
        vpQuestions.setCurrentItem(pos, true);
    }

    private void updateProgress(int pos) {
        tvProgress.setText((pos + 1) + " / " + questions.size());
        progressBar.setMax(questions.size());
        progressBar.setProgress(pos + 1);

        btnPrev.setEnabled(pos > 0);
        btnPrev.setAlpha(pos > 0 ? 1.0f : 0.5f);

        boolean last = pos == questions.size() - 1;
        btnNext.setVisibility(last ? View.GONE : View.VISIBLE);
        btnSubmit.setVisibility(last ? View.VISIBLE : View.GONE);
    }

    private void scheduleSave() {
        saveHandler.removeCallbacks(saveRunnable);
        saveRunnable = this::saveProgress;
        saveHandler.postDelayed(saveRunnable, 2000);
    }

    private void saveProgress() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, String> e : answers.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", e.getKey());
            m.put("answer", e.getValue());
            list.add(m);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("answers", list);

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.saveExamHomeworkProgress(token, examId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {}
            @Override public void onFailure(Call<Map<String, Object>> c, Throwable t) {}
        });
    }

    private void confirmSubmit() {
        new AlertDialog.Builder(this)
                .setTitle("确认交卷？")
                .setMessage("交卷后将不能修改答案。")
                .setPositiveButton("交卷", (d, w) -> submit())
                .setNegativeButton("取消", null)
                .show();
    }

    private void submit() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("提交中...");
        pd.show();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, String> e : answers.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", e.getKey());
            m.put("answer", e.getValue());
            list.add(m);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("answers", list);

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.submitExamHomework(token, examId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                pd.dismiss();
                if (resp.isSuccessful() && resp.body() != null) {
                    Toast.makeText(ExamHomeworkActivity.this, "提交成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ExamHomeworkActivity.this, ExamHomeworkResultActivity.class);
                    intent.putExtra("exam_id", examId);
                    intent.putExtra("exam_title", examTitle);
                    intent.putExtra("exam_type", examType);
                    startActivity(intent);
                    finish();
                } else {
                    String msg = resp.errorBody() != null ? "提交失败：" + resp.code() : "提交失败";
                    Toast.makeText(ExamHomeworkActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                pd.dismiss();
                Toast.makeText(ExamHomeworkActivity.this, "提交失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void autoSubmit() {
        Toast.makeText(this, "考试时间已到，自动交卷", Toast.LENGTH_SHORT).show();
        submit();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("确认退出？")
                .setMessage("退出后已作答内容会自动保存。")
                .setPositiveButton("退出", (d, w) -> {
                    saveProgress();
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(timerRunnable);
        saveHandler.removeCallbacks(saveRunnable);
        saveProgress();
    }

    // ===== 工具方法 =====

    private long parseTime(Object obj) {
        if (obj == null) return System.currentTimeMillis();
        return parseDateTime(String.valueOf(obj));
    }

    private long parseDateTime(String s) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
            return sdf.parse(s).getTime();
        } catch (Exception e) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                return sdf.parse(s).getTime();
            } catch (Exception e2) {
                return System.currentTimeMillis();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getOptions(Object obj) {
        List<String> list = new ArrayList<>();
        if (obj instanceof List) {
            for (Object o : (List<Object>) obj) {
                list.add(String.valueOf(o));
            }
        }
        return list;
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "single_choice" -> "单选题";
            case "multiple_choice" -> "多选题";
            case "true_false" -> "判断题";
            case "fill_blank" -> "填空题";
            case "short_answer" -> "简答题";
            default -> type;
        };
    }

    // ===== Adapter =====

    private class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exam_question, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> q = questions.get(pos);
            h.tvIndex.setText("第 " + (pos + 1) + " 题");
            String type = q.get("type") != null ? q.get("type").toString() : "single_choice";
            h.tvType.setText(typeLabel(type));
            h.tvScore.setText(q.get("score") + " 分");
            h.tvContent.setText(String.valueOf(q.get("content")));

            long qid = ((Number) q.get("id")).longValue();
            String currentAnswer = answers.getOrDefault(qid, "");

            h.llOptions.removeAllViews();
            switch (type) {
                case "single_choice" -> renderSingleChoice(h.llOptions, qid, getOptions(q.get("options")), currentAnswer);
                case "multiple_choice" -> renderMultipleChoice(h.llOptions, qid, getOptions(q.get("options")), currentAnswer);
                case "true_false" -> renderTrueFalse(h.llOptions, qid, currentAnswer);
                case "fill_blank", "short_answer" -> renderTextInput(h.llOptions, qid, currentAnswer, "short_answer".equals(type));
            }
        }

        @Override
        public int getItemCount() { return questions.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIndex, tvType, tvScore, tvContent;
            LinearLayout llOptions;
            VH(View v) {
                super(v);
                tvIndex = v.findViewById(R.id.tv_index);
                tvType = v.findViewById(R.id.tv_type);
                tvScore = v.findViewById(R.id.tv_score);
                tvContent = v.findViewById(R.id.tv_content);
                llOptions = v.findViewById(R.id.ll_options);
            }
        }
    }

    private void renderSingleChoice(LinearLayout container, long qid, List<String> options, String current) {
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < options.size(); i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(options.get(i));
            rb.setTextSize(14);
            rb.setTextColor(Color.parseColor("#1D1D1F"));
            rb.setPadding(8, 16, 8, 16);
            rb.setId(View.generateViewId());
            if (options.get(i).equals(current)) {
                rb.setChecked(true);
            }
            final String val = options.get(i);
            rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    answers.put(qid, val);
                    scheduleSave();
                }
            });
            rg.addView(rb);
        }
        container.addView(rg);
    }

    private void renderMultipleChoice(LinearLayout container, long qid, List<String> options, String current) {
        List<String> selected = new ArrayList<>();
        if (!current.isEmpty()) {
            selected.addAll(Arrays.asList(current.split("[,，;；]")));
        }
        for (String opt : options) {
            CheckBox cb = new CheckBox(this);
            cb.setText(opt);
            cb.setTextSize(14);
            cb.setTextColor(Color.parseColor("#1D1D1F"));
            cb.setPadding(8, 16, 8, 16);
            cb.setChecked(selected.contains(opt));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selected.add(opt);
                else selected.remove(opt);
                answers.put(qid, TextUtils.join(",", selected));
                scheduleSave();
            });
            container.addView(cb);
        }
    }

    private void renderTrueFalse(LinearLayout container, long qid, String current) {
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        String[] opts = {"正确", "错误"};
        for (String opt : opts) {
            RadioButton rb = new RadioButton(this);
            rb.setText(opt);
            rb.setTextSize(14);
            rb.setTextColor(Color.parseColor("#1D1D1F"));
            rb.setPadding(8, 16, 8, 16);
            rb.setId(View.generateViewId());
            if (opt.equals(current)) rb.setChecked(true);
            rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    answers.put(qid, opt);
                    scheduleSave();
                }
            });
            rg.addView(rb);
        }
        container.addView(rg);
    }

    private void renderTextInput(LinearLayout container, long qid, String current, boolean multiLine) {
        EditText et = new EditText(this);
        et.setText(current);
        et.setTextSize(14);
        et.setTextColor(Color.parseColor("#1D1D1F"));
        et.setHint("请输入答案");
        et.setBackgroundResource(R.drawable.bg_input_gray);
        et.setPadding(16, 16, 16, 16);
        if (multiLine) {
            et.setMinLines(4);
            et.setMaxLines(6);
            et.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        }
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                answers.put(qid, s.toString());
                scheduleSave();
            }
        });
        container.addView(et);
    }
}
