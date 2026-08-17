package com.znxsgl.student;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.znxsgl.student.model.Chapter;
import com.znxsgl.student.model.Lesson;
import com.znxsgl.student.network.ApiService;
import com.znxsgl.student.network.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import io.noties.markwon.syntax.Prism4jThemeDarkula;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChapterLearnActivity extends AppCompatActivity {

    private long courseId;
    private String token;
    private TextView tvProgress;
    private RecyclerView rvChapters;
    private ChapterAdapter adapter;

    private final List<Chapter> chapters = new ArrayList<>();
    private final Set<Long> completedIds = new HashSet<>();
    private final Set<Integer> expandedPositions = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_learn);

        courseId = getIntent().getLongExtra("courseId", 0);
        String courseName = getIntent().getStringExtra("courseName");
        if (courseName == null) courseName = "章节学习";

        SharedPreferences prefs = getSharedPreferences("znxsgl", 0);
        token = "Bearer " + prefs.getString("token", "");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText(courseName);
        tvProgress = findViewById(R.id.tv_progress);
        rvChapters = findViewById(R.id.rv_chapters);
        rvChapters.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChapterAdapter();
        rvChapters.setAdapter(adapter);

        loadCompletedThenChapters();
    }

    private void loadCompletedThenChapters() {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getCompletedLessons(token, courseId).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                try {
                    if (r.isSuccessful() && r.body() != null) {
                        JSONObject obj = new JSONObject(r.body().string());
                        JSONArray arr = obj.optJSONArray("lessonIds");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) completedIds.add(arr.getLong(i));
                        }
                    }
                } catch (Exception ignored) {}
                loadChapters();
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) { loadChapters(); }
        });
    }

    private void loadChapters() {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getCourseChapters(token, courseId).enqueue(new Callback<List<Chapter>>() {
            @Override public void onResponse(Call<List<Chapter>> c, Response<List<Chapter>> r) {
                chapters.clear();
                if (r.isSuccessful() && r.body() != null) {
                    chapters.addAll(r.body());
                }
                expandedPositions.clear();
                if (!chapters.isEmpty()) expandedPositions.add(0);
                adapter.notifyDataSetChanged();
                updateProgress();
            }
            @Override public void onFailure(Call<List<Chapter>> c, Throwable t) {
                Toast.makeText(ChapterLearnActivity.this, "网络错误：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProgress() {
        int total = 0;
        for (Chapter ch : chapters) total += ch.getLessons().size();
        int done = 0;
        for (Chapter ch : chapters) for (Lesson l : ch.getLessons()) if (completedIds.contains(l.getId())) done++;
        tvProgress.setText("已完成 " + done + "/" + total + " 课时");
    }

    private void markComplete(Lesson lesson, TextView btnComplete) {
        if (completedIds.contains(lesson.getId())) return;
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("lessonId", lesson.getId());
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.markLessonComplete(token, body).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                if (r.isSuccessful()) {
                    completedIds.add(lesson.getId());
                    btnComplete.setText("✓已完成");
                    btnComplete.setBackgroundColor(0xFF34C759);
                    updateProgress();
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ChapterLearnActivity.this, "标记失败", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) {
                Toast.makeText(ChapterLearnActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ChapterAdapter extends RecyclerView.Adapter<ChapterVH> {
        private final Handler delayHandler = new Handler(Looper.getMainLooper());
        private final Markwon markwon = Markwon.builder(ChapterLearnActivity.this)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(
                        new Prism4j(new PrismGrammarLocator()),
                        Prism4jThemeDarkula.create()
                ))
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                        builder.codeBlockTextColor(0xFFE2E8F0)
                                .codeBlockTypeface(Typeface.MONOSPACE)
                                .codeBlockMargin(0)
                                .blockMargin(0)
                                .headingBreakColor(0xFFE0E6F0);
                    }
                })
                .build();

        @NonNull @Override
        public ChapterVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter, parent, false);
            return new ChapterVH(v);
        }

        @Override public void onBindViewHolder(@NonNull ChapterVH h, int pos) {
            Chapter ch = chapters.get(pos);
            h.tvNo.setText("第 " + ch.getChapterNo() + " 章");
            h.tvTitle.setText(ch.getChapterName());
            if (ch.getDescription() != null && !ch.getDescription().isEmpty()) {
                h.tvDesc.setVisibility(View.VISIBLE);
                h.tvDesc.setText(ch.getDescription());
            } else {
                h.tvDesc.setVisibility(View.GONE);
            }

            int total = ch.getLessons().size();
            int doneCount = 0;
            for (Lesson l : ch.getLessons()) if (completedIds.contains(l.getId())) doneCount++;
            h.tvProgress.setText("已完成 " + doneCount + "/" + total + " 课时");
            h.ivNode.setImageResource(doneCount == total && total > 0
                    ? R.drawable.circle_node_complete : R.drawable.circle_node_normal);

            boolean expanded = expandedPositions.contains(pos);
            h.tvExpandIcon.setText(expanded ? "▼" : "▶");
            h.llLessons.setVisibility(expanded ? View.VISIBLE : View.GONE);
            h.llHeader.setOnClickListener(v -> {
                if (expandedPositions.contains(pos)) {
                    expandedPositions.remove(pos);
                } else {
                    expandedPositions.add(pos);
                }
                notifyItemChanged(pos);
            });

            h.llLessons.removeAllViews();
            for (Lesson l : ch.getLessons()) {
                // 章节学习不展示视频类课时
                if (l.getResourceType() != null && l.getResourceType().toLowerCase().contains("video")) continue;
                View lv = LayoutInflater.from(h.itemView.getContext()).inflate(R.layout.item_lesson, h.llLessons, false);
                ((TextView) lv.findViewById(R.id.tv_lesson_name)).setText(l.getLessonName());
                ((TextView) lv.findViewById(R.id.tv_lesson_type)).setText(typeLabel(l.getResourceType()));
                ((TextView) lv.findViewById(R.id.tv_lesson_icon)).setText(typeIcon(l.getResourceType()));
                LinearLayout llContentArea = lv.findViewById(R.id.ll_lesson_content_area);
                TextView btnComplete = lv.findViewById(R.id.btn_complete);
                LinearLayout llMarkdownContent = lv.findViewById(R.id.ll_markdown_content);
                if (l.getContent() != null && !l.getContent().isEmpty()) {
                    String formatted = MarkdownUtils.autoFormatIfNeeded(l.getContent());
                    MarkdownBlockRenderer.render(ChapterLearnActivity.this, markwon, formatted, llMarkdownContent);
                }
                boolean done = completedIds.contains(l.getId());
                Runnable showBtnTask = () -> {
                    if (!completedIds.contains(l.getId()) && llContentArea.getVisibility() == View.VISIBLE) {
                        btnComplete.setVisibility(View.VISIBLE);
                    }
                };
                btnComplete.setTag(showBtnTask);
                if (done) {
                    btnComplete.setVisibility(View.VISIBLE);
                    btnComplete.setText("✓ 已完成");
                    btnComplete.setBackgroundResource(R.drawable.bg_btn_success);
                    btnComplete.setTextColor(0xFFFFFFFF);
                } else {
                    btnComplete.setVisibility(View.GONE);
                    btnComplete.setText("完成学习");
                    btnComplete.setBackgroundResource(R.drawable.bg_btn_primary);
                    btnComplete.setTextColor(0xFFFFFFFF);
                }
                // 点击课时行展开/折叠 content
                lv.setOnClickListener(v -> {
                    if (llContentArea.getVisibility() == View.VISIBLE) {
                        llContentArea.setVisibility(View.GONE);
                        Object tag = btnComplete.getTag();
                        if (tag instanceof Runnable) delayHandler.removeCallbacks((Runnable) tag);
                        if (!completedIds.contains(l.getId())) btnComplete.setVisibility(View.GONE);
                    } else if (l.getContent() != null && !l.getContent().isEmpty()) {
                        llContentArea.setVisibility(View.VISIBLE);
                        if (!completedIds.contains(l.getId())) {
                            btnComplete.setVisibility(View.GONE);
                            delayHandler.removeCallbacks((Runnable) btnComplete.getTag());
                            delayHandler.postDelayed((Runnable) btnComplete.getTag(), 5000);
                        }
                    }
                });
                btnComplete.setOnClickListener(v -> markComplete(l, btnComplete));
                h.llLessons.addView(lv);
            }
        }

        @Override public int getItemCount() { return chapters.size(); }
    }

    static class ChapterVH extends RecyclerView.ViewHolder {
        ImageView ivNode;
        TextView tvNo;
        TextView tvTitle;
        TextView tvDesc;
        TextView tvProgress;
        TextView tvExpandIcon;
        LinearLayout llHeader;
        LinearLayout llLessons;
        ChapterVH(View v) {
            super(v);
            ivNode = v.findViewById(R.id.iv_chapter_node);
            tvNo = v.findViewById(R.id.tv_chapter_no);
            tvTitle = v.findViewById(R.id.tv_chapter_title);
            tvDesc = v.findViewById(R.id.tv_chapter_desc);
            tvProgress = v.findViewById(R.id.tv_chapter_progress);
            tvExpandIcon = v.findViewById(R.id.tv_expand_icon);
            llHeader = v.findViewById(R.id.ll_chapter_header);
            llLessons = v.findViewById(R.id.ll_lessons);
        }
    }

    private String typeLabel(String type) {
        if (type == null) return "资料";
        String t = type.toLowerCase();
        if (t.contains("video")) return "视频";
        if (t.contains("doc")) return "文档";
        if (t.contains("quiz") || t.contains("test")) return "测验";
        if (t.contains("discuss")) return "讨论";
        return type;
    }

    private String typeIcon(String type) {
        if (type == null) return "资";
        String t = type.toLowerCase();
        if (t.contains("video")) return "▶";
        if (t.contains("doc")) return "文";
        if (t.contains("quiz") || t.contains("test")) return "测";
        if (t.contains("discuss")) return "讨";
        return "资";
    }
}
