package com.example.khoga.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khoga.R;

import java.util.List;

/**
 * SelectImageAdapter - Hiển thị ảnh và video đã chọn trong form đánh giá
 * Mỗi item có nút X để xóa; video hiển thị thêm icon play ở giữa
 */
public class SelectImageAdapter
        extends RecyclerView.Adapter<SelectImageAdapter.ViewHolder> {

    public static class MediaItem {
        public final Uri uri;
        public final boolean isVideo;

        public MediaItem(Uri uri, boolean isVideo) {
            super();
            this.uri = uri;
            this.isVideo = isVideo;
        }
    }

    private final List<MediaItem> mediaItems;
    private final OnRemoveListener onRemoveListener;

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public SelectImageAdapter(List<MediaItem> mediaItems, OnRemoveListener listener) {
        this.mediaItems = mediaItems;
        this.onRemoveListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);

        Glide.with(holder.imageView.getContext())
                .load(item.uri)
                .centerCrop()
                .into(holder.imageView);

        holder.playIcon.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);

        holder.btnRemove.setOnClickListener(v -> {
            if (onRemoveListener != null) {
                onRemoveListener.onRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView playIcon;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgSelected);
            playIcon = itemView.findViewById(R.id.imgPlayIcon);
            btnRemove = itemView.findViewById(R.id.btnRemoveImage);
        }
    }
}
