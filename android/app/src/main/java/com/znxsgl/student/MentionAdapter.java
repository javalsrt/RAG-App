package com.znxsgl.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @提及提示弹窗的适配器，支持按关键字过滤
 */
public class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.VH> {

    private final List<MentionItem> allItems;
    private final List<MentionItem> displayItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MentionItem item);
    }

    public MentionAdapter(List<MentionItem> items, OnItemClickListener listener) {
        this.allItems = new ArrayList<>(items);
        this.displayItems = new ArrayList<>(items);
        this.listener = listener;
    }

    /**
     * 替换全部数据（用于从网络加载联系人后）
     */
    public void setItems(List<MentionItem> items) {
        allItems.clear();
        allItems.addAll(items);
        displayItems.clear();
        displayItems.addAll(items);
        notifyDataSetChanged();
    }

    /**
     * 根据@后的关键字过滤列表
     */
    public void filter(String keyword) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        displayItems.clear();
        for (MentionItem item : allItems) {
            if (kw.isEmpty()
                    || item.name.toLowerCase().contains(kw)
                    || item.label.toLowerCase().contains(kw)
                    || item.tag.toLowerCase().contains(kw)) {
                displayItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mention, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MentionItem item = displayItems.get(position);
        holder.tvAvatar.setText(item.avatarText);
        holder.tvName.setText(item.label);
        holder.tvTag.setText(item.tag);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvTag;
        VH(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_mention_avatar);
            tvName = v.findViewById(R.id.tv_mention_name);
            tvTag = v.findViewById(R.id.tv_mention_tag);
        }
    }
}
