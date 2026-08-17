package com.znxsgl.student;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.znxsgl.student.model.QuizQuestion;

import java.util.List;

public class QuizPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CHOICE = 0;
    private static final int TYPE_JUDGE = 1;
    private static final int TYPE_ANALYSIS = 2;
    private static final int TYPE_FILL = 3;

    private final List<QuizQuestion> questions;
    private OnAnswerListener listener;

    public interface OnAnswerListener {
        void onAnswered(int position, String answer);
        void onAutoSkip(int position);
    }

    public QuizPagerAdapter(List<QuizQuestion> questions) { this.questions = questions; }
    public void setOnAnswerListener(OnAnswerListener l) { this.listener = l; }

    @Override public int getItemViewType(int pos) {
        String t = questions.get(pos).getQuestionType();
        if ("判断".equals(t)) return TYPE_JUDGE;
        if ("解析".equals(t)) return TYPE_ANALYSIS;
        if ("填空".equals(t)) return TYPE_FILL;
        return TYPE_CHOICE;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (type == TYPE_JUDGE) return new JudgeVH(inf.inflate(R.layout.item_quiz_judge, parent, false));
        if (type == TYPE_ANALYSIS) return new AnalysisVH(inf.inflate(R.layout.item_quiz_analysis, parent, false));
        if (type == TYPE_FILL) return new FillVH(inf.inflate(R.layout.item_quiz_fill, parent, false));
        return new ChoiceVH(inf.inflate(R.layout.item_quiz_choice, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        QuizQuestion q = questions.get(pos);
        if (h instanceof ChoiceVH) bindChoice((ChoiceVH) h, q, pos);
        else if (h instanceof JudgeVH) bindJudge((JudgeVH) h, q, pos);
        else if (h instanceof AnalysisVH) bindAnalysis((AnalysisVH) h, q, pos);
        else if (h instanceof FillVH) bindFill((FillVH) h, q, pos);
    }

    @Override public int getItemCount() { return questions.size(); }

    // ===== 选择题 =====
    private void bindChoice(ChoiceVH vh, QuizQuestion q, int pos) {
        vh.tvTag.setText("📝 单选题");
        vh.tvNum.setText((pos + 1) + "/" + questions.size());
        vh.tvQuestion.setText(q.getQuestion());
        vh.llOptions.removeAllViews();

        List<String> options = q.getOptions();
        if (options != null) {
            for (String optText : options) {
                boolean selected = optText.equals(q.getUserAnswer());
                TextView tv = new TextView(vh.itemView.getContext());
                tv.setText(optText);
                tv.setTextSize(16);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(20, 18, 20, 18);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(14);
                bg.setColor(selected ? Color.parseColor("#5E6AD2") : Color.WHITE);
                bg.setStroke(1, Color.parseColor("#E5E5EA"));
                tv.setBackground(bg);
                tv.setTextColor(selected ? Color.WHITE : Color.parseColor("#1D1D1F"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                tv.setLayoutParams(lp);

                final String answer = optText;
                tv.setOnClickListener(v -> {
                    // 点击反馈动画
                    v.setAlpha(0.6f);
                    v.postDelayed(() -> v.setAlpha(1.0f), 150);
                    q.setUserAnswer(answer);
                    q.setModifiedCount(q.getModifiedCount() + 1);
                    notifyItemChanged(pos);
                    if (listener != null) listener.onAnswered(pos, answer);
                });
                vh.llOptions.addView(tv);
            }
        }
    }

    // ===== 判断题 =====
    private void bindJudge(JudgeVH vh, QuizQuestion q, int pos) {
        vh.tvTag.setText("⚡ 判断题");
        vh.tvNum.setText((pos + 1) + "/" + questions.size());
        vh.tvQuestion.setText(q.getQuestion());

        boolean selectedTrue = "正确".equals(q.getUserAnswer());
        boolean selectedFalse = "错误".equals(q.getUserAnswer());

        // 统一配色：选中浅灰，未选中白色卡片
        vh.btnTrue.setBackground(getJudgeBg(selectedTrue));
        vh.btnFalse.setBackground(getJudgeBg(selectedFalse));

        vh.btnTrue.setOnClickListener(v -> {
            if (selectedTrue) return; // 已是选中状态
            q.setUserAnswer("正确");
            notifyItemChanged(pos); // 刷新两个按钮状态
            v.postDelayed(() -> {
                if (listener != null) listener.onAnswered(pos, "正确");
            }, 300);
        });
        vh.btnFalse.setOnClickListener(v -> {
            if (selectedFalse) return;
            q.setUserAnswer("错误");
            notifyItemChanged(pos);
            v.postDelayed(() -> {
                if (listener != null) listener.onAnswered(pos, "错误");
            }, 300);
        });
    }

    private GradientDrawable getJudgeBg(boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12);
        bg.setColor(selected ? Color.parseColor("#EEEEF2") : Color.WHITE);
        bg.setStroke(1, Color.parseColor("#E5E5EA"));
        return bg;
    }

    // ===== 解析题（无提交按钮，由长按提交统管）=====
    private void bindAnalysis(AnalysisVH vh, QuizQuestion q, int pos) {
        vh.tvTag.setText("✍️ 解析题");
        vh.tvNum.setText((pos + 1) + "/" + questions.size());
        vh.tvQuestion.setText(q.getQuestion());
        if (q.getUserAnswer() == null || q.getUserAnswer().isEmpty()) {
            vh.etAnswer.setText("");
        } else if (!vh.etAnswer.getText().toString().equals(q.getUserAnswer())) {
            vh.etAnswer.setText(q.getUserAnswer());
        }
    }

    // ===== 填空题（无提交按钮，由长按提交统管）=====
    private void bindFill(FillVH vh, QuizQuestion q, int pos) {
        vh.tvTag.setText("🔤 填空题");
        vh.tvNum.setText((pos + 1) + "/" + questions.size());
        vh.tvQuestion.setText(q.getQuestion());
        if (q.getUserAnswer() == null || q.getUserAnswer().isEmpty()) {
            vh.etAnswer.setText("");
        } else if (!vh.etAnswer.getText().toString().equals(q.getUserAnswer())) {
            vh.etAnswer.setText(q.getUserAnswer());
        }
    }

    /** 提交前收集解析/填空题的文本答案 */
    public void collectTextAnswers() {
        // 暂存当前输入，供 submitQuiz 使用
    }

    // ===== ViewHolders =====
    static class ChoiceVH extends RecyclerView.ViewHolder {
        TextView tvTag, tvNum, tvQuestion; LinearLayout llOptions;
        ChoiceVH(View v) { super(v);
            tvTag = v.findViewById(R.id.tv_quiz_tag); tvNum = v.findViewById(R.id.tv_quiz_num);
            tvQuestion = v.findViewById(R.id.tv_quiz_question); llOptions = v.findViewById(R.id.ll_options); }
    }
    static class JudgeVH extends RecyclerView.ViewHolder {
        TextView tvTag, tvNum, tvQuestion, btnTrue, btnFalse;
        JudgeVH(View v) { super(v);
            tvTag = v.findViewById(R.id.tv_quiz_tag); tvNum = v.findViewById(R.id.tv_quiz_num);
            tvQuestion = v.findViewById(R.id.tv_quiz_question);
            btnTrue = v.findViewById(R.id.btn_true); btnFalse = v.findViewById(R.id.btn_false); }
    }
    public static class AnalysisVH extends RecyclerView.ViewHolder {
        public TextView tvTag, tvNum, tvQuestion; public EditText etAnswer;
        AnalysisVH(View v) { super(v);
            tvTag = v.findViewById(R.id.tv_quiz_tag); tvNum = v.findViewById(R.id.tv_quiz_num);
            tvQuestion = v.findViewById(R.id.tv_quiz_question); etAnswer = v.findViewById(R.id.et_answer); }
    }
    public static class FillVH extends RecyclerView.ViewHolder {
        public TextView tvTag, tvNum, tvQuestion; public EditText etAnswer;
        FillVH(View v) { super(v);
            tvTag = v.findViewById(R.id.tv_quiz_tag); tvNum = v.findViewById(R.id.tv_quiz_num);
            tvQuestion = v.findViewById(R.id.tv_quiz_question); etAnswer = v.findViewById(R.id.et_answer); }
    }
}
