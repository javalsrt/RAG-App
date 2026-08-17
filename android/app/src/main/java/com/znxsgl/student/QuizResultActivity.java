package com.znxsgl.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizResultActivity extends AppCompatActivity {

    private String token;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        SharedPreferences prefs = getSharedPreferences("znxsgl", 0);
        token = "Bearer " + prefs.getString("token", "");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("测评报告");

        Intent it = getIntent();
        int correct = it.getIntExtra("correctCount", 0);
        int skip = it.getIntExtra("skipCount", 0);
        int total = it.getIntExtra("totalQuestions", 0);
        int totalSec = it.getIntExtra("totalDurationSec", 0);
        String suggestion = it.getStringExtra("suggestion");

        int answered = total - skip;
        int accuracy = total > 0 ? correct * 100 / total : 0;

        ((TextView) findViewById(R.id.tv_accuracy)).setText(accuracy + "%");
        ((TextView) findViewById(R.id.tv_correct_count)).setText(correct + "/" + total);
        ((TextView) findViewById(R.id.tv_answered_count)).setText(String.valueOf(answered));
        ((TextView) findViewById(R.id.tv_skip_count)).setText(String.valueOf(skip));
        ((TextView) findViewById(R.id.tv_duration)).setText(formatDuration(totalSec));

        // 六维评分
        @SuppressWarnings("unchecked")
        Map<String, Object> scores = (Map<String, Object>) it.getSerializableExtra("scores");
        renderScores(scores);

        // 优劣势
        renderTags(R.id.ll_strengths, it.getStringArrayListExtra("strengths"), "暂无突出优势");
        renderTags(R.id.ll_weaknesses, it.getStringArrayListExtra("weaknesses"), "暂无薄弱环节");

        // 综合建议
        TextView tvSuggestion = findViewById(R.id.tv_suggestion);
        if (suggestion != null && !suggestion.isEmpty()) {
            tvSuggestion.setText(suggestion);
        } else {
            tvSuggestion.setText("继续保持学习节奏，多复习薄弱章节。");
        }

        // 学习计划
        renderStudyPlan(it.getStringArrayListExtra("studyPlan"));

        // 本次错题
        renderWrongAnswers(it.getStringExtra("wrongAnswersJson"));

        findViewById(R.id.btn_wrong_analysis).setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("action", "wrong_analysis"));
            finish();
        });

        findViewById(R.id.btn_review).setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("action", "review"));
            finish();
        });

        findViewById(R.id.btn_done).setOnClickListener(v -> finish());
    }

    private String formatDuration(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        if (m > 0) return String.format(Locale.getDefault(), "%d分%d秒", m, s);
        return s + "秒";
    }

    private void renderScores(Map<String, Object> scores) {
        LinearLayout container = findViewById(R.id.ll_scores);
        container.removeAllViews();
        if (scores == null || scores.isEmpty()) {
            findViewById(R.id.card_scores).setVisibility(View.GONE);
            return;
        }
        for (Map.Entry<String, Object> e : scores.entrySet()) {
            int score = 0;
            Object v = e.getValue();
            if (v instanceof Number) score = ((Number) v).intValue();
            View row = LayoutInflater.from(this).inflate(R.layout.item_score_bar, container, false);
            ((TextView) row.findViewById(R.id.tv_score_name)).setText(e.getKey());
            TextView tvBar = row.findViewById(R.id.tv_score_bar);
            TextView tvValue = row.findViewById(R.id.tv_score_value);
            tvValue.setText(String.valueOf(score));
            // 动态宽度
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tvBar.getLayoutParams();
            lp.weight = Math.max(1, score);
            tvBar.setLayoutParams(lp);
            container.addView(row);
        }
    }

    private void renderTags(int containerId, ArrayList<String> tags, String emptyText) {
        LinearLayout container = findViewById(containerId);
        container.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(emptyText);
            tv.setTextSize(13);
            tv.setTextColor(0xFF8E8E93);
            container.addView(tv);
            return;
        }
        for (String tag : tags) {
            TextView tv = new TextView(this);
            tv.setText(tag);
            tv.setTextSize(13);
            tv.setTextColor(0xFF5E6AD2);
            tv.setBackgroundResource(R.drawable.bg_tag_blue);
            tv.setPadding(dp(10), dp(4), dp(10), dp(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), dp(8));
            tv.setLayoutParams(lp);
            container.addView(tv);
        }
    }

    private void renderStudyPlan(ArrayList<String> plan) {
        LinearLayout container = findViewById(R.id.ll_study_plan);
        container.removeAllViews();
        if (plan == null || plan.isEmpty()) {
            findViewById(R.id.card_plan).setVisibility(View.GONE);
            return;
        }
        for (int i = 0; i < plan.size(); i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_study_plan, container, false);
            ((TextView) row.findViewById(R.id.tv_plan_index)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.tv_plan_text)).setText(plan.get(i));
            container.addView(row);
        }
    }

    private void renderWrongAnswers(String wrongJson) {
        RecyclerView rv = findViewById(R.id.rv_wrong_answers);
        TextView tvEmpty = findViewById(R.id.tv_wrong_empty);

        List<Map<String, String>> wrongAnswers = new ArrayList<>();
        if (wrongJson != null && !wrongJson.isEmpty()) {
            try {
                wrongAnswers = gson.fromJson(wrongJson, new TypeToken<List<Map<String, String>>>(){}.getType());
            } catch (Exception ignored) {}
        }

        if (wrongAnswers == null || wrongAnswers.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new WrongAnswerAdapter(wrongAnswers));
    }

    private static class WrongAnswerAdapter extends RecyclerView.Adapter<WrongAnswerAdapter.VH> {
        private final List<Map<String, String>> data;

        WrongAnswerAdapter(List<Map<String, String>> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz_wrong, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, String> item = data.get(pos);
            String type = item.getOrDefault("questionType", "单选");
            if ("单选".equals(type)) type = "单选题";
            else if ("判断".equals(type)) type = "判断题";
            else if ("解析".equals(type)) type = "解析题";
            else if ("填空".equals(type)) type = "填空题";

            h.tvType.setText(type);
            h.tvQuestion.setText(item.getOrDefault("question", ""));

            String ua = item.get("userAnswer");
            if (ua == null || ua.isEmpty() || "不会".equals(ua)) {
                h.tvUserAnswer.setText("未作答 / 不会");
            } else {
                h.tvUserAnswer.setText(ua);
            }
            h.tvCorrectAnswer.setText(item.getOrDefault("correctAnswer", ""));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvType, tvQuestion, tvUserAnswer, tvCorrectAnswer;
            VH(View v) { super(v);
                tvType = v.findViewById(R.id.tv_type);
                tvQuestion = v.findViewById(R.id.tv_question);
                tvUserAnswer = v.findViewById(R.id.tv_user_answer);
                tvCorrectAnswer = v.findViewById(R.id.tv_correct_answer);
            }
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}