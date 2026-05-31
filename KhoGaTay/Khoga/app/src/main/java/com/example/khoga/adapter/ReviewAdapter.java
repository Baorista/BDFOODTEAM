package com.example.khoga.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khoga.R;
import com.example.khoga.model.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ReviewAdapter - Hiển thị danh sách đánh giá trong màn Product Detail
 *
 * Theo Figma:
 * ┌──────────────────────────────────┐
 * │ [Avatar] Nguyễn Kim Bảo         │
 * │          2026-02-01   ★★★★★     │
 * │          Cũng ngon               │
 * │          [ảnh1] [ảnh2]           │
 * └──────────────────────────────────┘
 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<Review> reviews;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void updateData(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        // Tên user
        holder.tvUserName.setText(review.getUserName());

        // Avatar
        if (review.getUserAvatar() != null && !review.getUserAvatar().isEmpty()) {
            Glide.with(holder.imgAvatar.getContext())
                    .load(review.getUserAvatar())
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        // Ngày đánh giá
        holder.tvDate.setText(dateFormat.format(new Date(review.getCreatedAt())));

        // Sao đánh giá (hiển thị bằng LinearLayout chứa 5 star nhỏ)
        setupStars(holder.layoutStars, review.getRating());

        // Comment
        if (review.getComment() != null && !review.getComment().isEmpty()) {
            holder.tvComment.setVisibility(View.VISIBLE);
            holder.tvComment.setText(review.getComment());
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        // Ảnh đánh giá (nếu có)
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            holder.rvReviewImages.setVisibility(View.VISIBLE);
            if (holder.imageAdapter == null) {
                holder.imageAdapter = new ReviewImageAdapter(review.getImages());
                holder.rvReviewImages.setAdapter(holder.imageAdapter);
            } else {
                holder.imageAdapter.updateData(review.getImages());
            }
        } else {
            holder.rvReviewImages.setVisibility(View.GONE);
        }
    }

    /**
     * Tạo n sao vàng + (5-n) sao xám
     */
    private void setupStars(LinearLayout layout, int rating) {
        layout.removeAllViews();
        for (int i = 0; i < 5; i++) {
            ImageView star = new ImageView(layout.getContext());
            int size = dpToPx(layout.getContext(), 14);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(dpToPx(layout.getContext(), 1));
            star.setLayoutParams(params);
            star.setImageResource(i < rating
                    ? R.drawable.ic_star_filled
                    : R.drawable.ic_star_outline);
            layout.addView(star);
        }
    }

    private int dpToPx(android.content.Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUserName, tvDate, tvComment;
        LinearLayout layoutStars;
        RecyclerView rvReviewImages;
        ReviewImageAdapter imageAdapter;

        ViewHolder(View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            layoutStars = itemView.findViewById(R.id.layoutStarsSmall);
            rvReviewImages = itemView.findViewById(R.id.rvReviewImages);
            rvReviewImages.setLayoutManager(
                    new LinearLayoutManager(itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL, false));
        }
    }

    // ================================================================
    // INNER ADAPTER: Ảnh trong mỗi review
    // ================================================================

    static class ReviewImageAdapter
            extends RecyclerView.Adapter<ReviewImageAdapter.ImgVH> {

        private List<String> imageUrls;

        ReviewImageAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        void updateData(List<String> newUrls) {
            this.imageUrls = newUrls;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ImgVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView img = new ImageView(parent.getContext());
            int size = (int) (64 * parent.getContext().getResources()
                    .getDisplayMetrics().density);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMarginEnd((int) (6 * parent.getContext().getResources()
                    .getDisplayMetrics().density));
            img.setLayoutParams(params);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setClipToOutline(true);
            img.setBackgroundResource(R.drawable.bg_rounded_8dp);
            return new ImgVH(img);
        }

        @Override
        public void onBindViewHolder(@NonNull ImgVH holder, int position) {
            Glide.with(holder.imageView.getContext())
                    .load(imageUrls.get(position))
                    .centerCrop()
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImgVH extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImgVH(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }
}
