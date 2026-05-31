package com.example.khoga.model;

import com.google.firebase.database.PropertyName;

public class Banner {

    private String bannerId;
    private String imageUrl;
    private String linkTo;
    private int order;
    private boolean isActive;

    public Banner() { this.isActive = true; }

    public Banner(String bannerId, String imageUrl, String linkTo, int order, boolean isActive) {
        this.bannerId = bannerId;
        this.imageUrl = imageUrl;
        this.linkTo   = linkTo;
        this.order    = order;
        this.isActive = isActive;
    }

    public String getBannerId() { return bannerId; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkTo()   { return linkTo; }
    public int    getOrder()    { return order; }

    @PropertyName("isActive")
    public boolean isActive()   { return isActive; }

    public void setBannerId(String v) { this.bannerId = v; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public void setLinkTo(String v)   { this.linkTo = v; }
    public void setOrder(int v)       { this.order = v; }

    @PropertyName("isActive")
    public void setActive(boolean v)  { this.isActive = v; }
}