package com.example.khoga.model;

public class Category {

    private String categoryId;
    private String name;
    private String imageUrl;
    private int order;

    public Category() {}

    public Category(String categoryId, String name, String imageUrl, int order) {
        this.categoryId = categoryId;
        this.name       = name;
        this.imageUrl   = imageUrl;
        this.order      = order;
    }

    public String getCategoryId() { return categoryId; }
    public String getName()       { return name; }
    public String getImageUrl()   { return imageUrl; }
    public int    getOrder()      { return order; }

    public void setCategoryId(String v) { this.categoryId = v; }
    public void setName(String v)       { this.name = v; }
    public void setImageUrl(String v)   { this.imageUrl = v; }
    public void setOrder(int v)         { this.order = v; }
}