package com.example.khoga.model;

import com.google.firebase.database.PropertyName;

public class User {

    @PropertyName("uid")
    private String uid;

    @PropertyName("email")
    private String email;

    @PropertyName("displayName")
    private String displayName;

    @PropertyName("photoUrl")
    private String photoUrl;

    @PropertyName("phone")
    private String phone;

    @PropertyName("role")
    private String role;

    @PropertyName("createdAt")
    private long createdAt;

    @PropertyName("updatedAt")
    private long updatedAt;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String uid, String email, String displayName, String role) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @PropertyName("uid")
    public String getUid() { return uid; }

    @PropertyName("uid")
    public void setUid(String uid) { this.uid = uid; }

    @PropertyName("email")
    public String getEmail() { return email; }

    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("displayName")
    public String getDisplayName() { return displayName; }

    @PropertyName("displayName")
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @PropertyName("photoUrl")
    public String getPhotoUrl() { return photoUrl; }

    @PropertyName("photoUrl")
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    @PropertyName("phone")
    public String getPhone() { return phone; }

    @PropertyName("phone")
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("role")
    public String getRole() { return role; }

    @PropertyName("role")
    public void setRole(String role) { this.role = role; }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAt; }

    @PropertyName("createdAt")
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("updatedAt")
    public long getUpdatedAt() { return updatedAt; }

    @PropertyName("updatedAt")
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
