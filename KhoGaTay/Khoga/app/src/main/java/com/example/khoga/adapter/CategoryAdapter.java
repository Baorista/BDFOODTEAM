package com.example.khoga.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onClick(String categoryId); // null = xem tất cả
    }

    private List<Category> categories = new ArrayList<>();
    private String selectedId = null;
    private OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> list) {
        this.categories = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Position 0 = "Tất cả"
        if (position == 0) {
            holder.tvName.setText("Tất cả");
            boolean sel = selectedId == null;
            holder.tvName.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
            holder.tvName.setTextColor(sel ? Color.WHITE : Color.DKGRAY);
            holder.itemView.setOnClickListener(v -> {
                selectedId = null;
                notifyDataSetChanged();
                listener.onClick(null);
            });
        } else {
            Category cat = categories.get(position - 1);
            holder.tvName.setText(cat.getName());
            boolean sel = cat.getCategoryId().equals(selectedId);
            holder.tvName.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
            holder.tvName.setTextColor(sel ? Color.WHITE : Color.DKGRAY);
            holder.itemView.setOnClickListener(v -> {
                String newId = sel ? null : cat.getCategoryId();
                selectedId = newId;
                notifyDataSetChanged();
                listener.onClick(newId);
            });
        }
    }

    @Override
    public int getItemCount() { return categories.size() + 1; } // +1 cho "Tất cả"

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}