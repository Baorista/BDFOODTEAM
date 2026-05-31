package com.example.khoga.model;

/**
 * BrowsingHistory — Model cho node browsingHistory/{userId}/{productId}
 * Dùng để cung cấp context cho AI chatbot
 */
public class BrowsingHistory {

    private String productId;
    private String categoryId;
    private String productName;
    private int viewCount;
    private long lastViewedAt;

    public BrowsingHistory() {}

    public BrowsingHistory(String productId, String categoryId, String productName) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.productName = productName;
        this.viewCount = 1;
        this.lastViewedAt = System.currentTimeMillis();
    }

    // ===== Getters & Setters =====

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public long getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(long lastViewedAt) { this.lastViewedAt = lastViewedAt; }
}
