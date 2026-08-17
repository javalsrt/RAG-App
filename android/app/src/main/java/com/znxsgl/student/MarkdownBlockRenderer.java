package com.znxsgl.student;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;

import io.noties.markwon.Markwon;

/**
 * 将 Markdown 按块渲染到 LinearLayout：普通块用 TextView，代码块用独立横向滚动容器。
 * 解决 RecyclerView 嵌套 RecyclerView 的滚动冲突，同时保证代码块完整不被切断。
 */
public class MarkdownBlockRenderer {

    public static void render(@NonNull Context context,
                              @NonNull Markwon markwon,
                              @NonNull String markdown,
                              @NonNull LinearLayout container) {
        container.removeAllViews();
        Node root = markwon.parse(markdown);
        Node child = root.getFirstChild();
        while (child != null) {
            if (child instanceof FencedCodeBlock) {
                addCodeBlock(context, markwon, container, (FencedCodeBlock) child);
            } else {
                addTextBlock(context, markwon, container, child);
            }
            child = child.getNext();
        }
    }

    private static void addTextBlock(@NonNull Context context,
                                     @NonNull Markwon markwon,
                                     @NonNull LinearLayout container,
                                     @NonNull Node node) {
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp2px(context, 8));
        textView.setLayoutParams(params);
        textView.setTextSize(15);
        textView.setTextColor(context.getResources().getColor(R.color.ink));
        textView.setLineSpacing(dp2px(context, 8), 1f);
        Spanned spanned = markwon.render(node);
        textView.setText(spanned);
        container.addView(textView);
    }

    private static void addCodeBlock(@NonNull Context context,
                                     @NonNull Markwon markwon,
                                     @NonNull LinearLayout container,
                                     @NonNull FencedCodeBlock node) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_markdown_code_block, container, false);
        TextView tvCode = view.findViewById(R.id.tv_code);
        ImageView ivCopy = view.findViewById(R.id.iv_copy);

        // 用 Markwon 渲染该代码块节点，触发语法高亮
        Spanned spanned = markwon.render(node);
        tvCode.setText(spanned);

        final String code = node.getLiteral().trim();
        ivCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("code", code));
                Toast.makeText(v.getContext(), "代码已复制", Toast.LENGTH_SHORT).show();
            }
        });

        container.addView(view);
    }

    private static int dp2px(@NonNull Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
