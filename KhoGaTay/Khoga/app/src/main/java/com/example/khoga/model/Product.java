package com.example.khoga.model;

import com.google.firebase.database.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Product {

    private String productId;
    private String name;
    private String description;
    private String categoryId;
    private String brand;
    private double price;
    private double salePrice;
    private List<String> images;
    private List<String> colors;
    private List<String> sizes;
    private int stock;
    private double avgRating;
    private int totalReviews;
    private int totalSold;
    private boolean isActive;
    private long createdAt;

    // Constructor rỗng BẮT BUỘC cho Firebase
    public Product() {
        this.images   = new ArrayList<>();
        this.colors   = new ArrayList<>();
        this.sizes    = new ArrayList<>();
        this.isActive = true;
    }

    public Product(String productId, String name, String description,
                   String categoryId, String brand,
                   double price, double salePrice,
                   List<String> images, List<String> colors, List<String> sizes,
                   int stock, double avgRating,
                   int totalReviews, int totalSold,
                   boolean isActive, long createdAt) {
        this.productId    = productId;
        this.name         = name;
        this.description  = description;
        this.categoryId   = categoryId;
        this.brand        = brand;
        this.price        = price;
        this.salePrice    = salePrice;
        this.images       = images  != null ? images  : new ArrayList<>();
        this.colors       = colors  != null ? colors  : new ArrayList<>();
        this.sizes        = sizes   != null ? sizes   : new ArrayList<>();
        this.stock        = stock;
        this.avgRating    = avgRating;
        this.totalReviews = totalReviews;
        this.totalSold    = totalSold;
        this.isActive     = isActive;
        this.createdAt    = createdAt;
    }

    // Getters
    public String getProductId()    { return productId; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public String getCategoryId()   { return categoryId; }
    public String getBrand()        { return brand; }
    public double getPrice()        { return price; }
    public double getSalePrice()    { return salePrice; }
    public List<String> getImages() { return images; }
    public List<String> getColors() { return colors; }
    public List<String> getSizes()  { return sizes; }
    public int getStock()           { return stock; }
    public double getAvgRating()    { return avgRating; }
    public int getTotalReviews()    { return totalReviews; }
    public int getTotalSold()       { return totalSold; }
    public long getCreatedAt()      { return createdAt; }

    @PropertyName("isActive")
    public boolean isActive()       { return isActive; }

    // Setters
    public void setProductId(String v)      { this.productId = v; }
    public void setName(String v)           { this.name = v; }
    public void setDescription(String v)    { this.description = v; }
    public void setCategoryId(String v)     { this.categoryId = v; }
    public void setBrand(String v)          { this.brand = v; }
    public void setPrice(double v)          { this.price = v; }
    public void setSalePrice(double v)      { this.salePrice = v; }
    public void setStock(int v)             { this.stock = v; }
    public void setAvgRating(double v)      { this.avgRating = v; }
    public void setTotalReviews(int v)      { this.totalReviews = v; }
    public void setTotalSold(int v)         { this.totalSold = v; }
    public void setCreatedAt(long v)        { this.createdAt = v; }

    @PropertyName("isActive")
    public void setActive(boolean v)        { this.isActive = v; }

    // ── QUAN TRỌNG: Setter an toàn cho List fields ──
    // Firebase có thể trả về String thay vì List khi data không đúng format
    // Các setter này xử lý cả 2 trường hợp để tránh crash

    @SuppressWarnings("unchecked")
    public void setImages(Object v) {
        this.images = safeToList(v);
    }

    @SuppressWarnings("unchecked")
    public void setColors(Object v) {
        this.colors = safeToList(v);
    }

    @SuppressWarnings("unchecked")
    public void setSizes(Object v) {
        this.sizes = safeToList(v);
    }

    // Hàm chuyển đổi an toàn: nhận cả String, List, hoặc null
    @SuppressWarnings("unchecked")
    private List<String> safeToList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            // Trường hợp bình thường: Firebase trả về List
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        if (value instanceof String) {
            // Trường hợp lỗi data: Firebase trả về String đơn lẻ
            List<String> result = new ArrayList<>();
            String s = (String) value;
            if (!s.isEmpty()) result.add(s);
            return result;
        }
        // Trường hợp khác (Map, Number...): bỏ qua
        return new ArrayList<>();
    }

    // Helper
    public String getFirstImage() {
        return (images != null && !images.isEmpty()) ? images.get(0) : "";
    }

    public double getDisplayPrice() {
        return (salePrice > 0 && salePrice < price) ? salePrice : price;
    }

    public static String formatPrice(double price) {
        String raw = String.valueOf((long) price);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = raw.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) sb.insert(0, '.');
            sb.insert(0, raw.charAt(i));
            count++;
        }
        return sb + "đ";
    }
}