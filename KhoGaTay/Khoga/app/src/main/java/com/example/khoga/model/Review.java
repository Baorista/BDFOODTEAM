package com.example.khoga.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Review {
    private String reviewId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String orderId;
    private String productId;
    private int rating;          // 1-5
    private String comment;
    private List<String> images; // URL ảnh đánh giá (Firebase Storage)
    private String videoUrl;     // URL video đánh giá (nếu có)
    private long createdAt;

    // Constructor rỗng bắt buộc cho Firebase
    public Review() {}

    public Review(String reviewId, String userId, String userName, String userAvatar,
                  String orderId, String productId, int rating, String comment,
                  List<String> images, String videoUrl, long createdAt) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.orderId = orderId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.images = images;
        this.videoUrl = videoUrl;
        this.createdAt = createdAt;
    }

    // ===== Getters & Setters =====

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }

    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            sb.append("⭐"); 
        }
        return sb.toString();
    }
}
