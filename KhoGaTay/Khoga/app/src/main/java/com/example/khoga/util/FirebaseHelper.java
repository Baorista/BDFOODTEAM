package com.example.khoga.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Singleton helper class providing Firebase service references.
 */
public class FirebaseHelper {

    private static volatile FirebaseHelper instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase firebaseDatabase;
    private final FirebaseStorage firebaseStorage;

    private FirebaseHelper() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            synchronized (FirebaseHelper.class) {
                if (instance == null) {
                    instance = new FirebaseHelper();
                }
            }
        }
        return instance;
    }

    // ─── Auth ────────────────────────────────────────────
    public FirebaseAuth getAuth() {
        return firebaseAuth;
    }

    public String getCurrentUserId() {
        return firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : null;
    }

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // ─── Database References ─────────────────────────────
    public DatabaseReference getDatabaseReference() {
        return firebaseDatabase.getReference();
    }

    public DatabaseReference getUsersRef() {
        return firebaseDatabase.getReference(Constants.NODE_USERS);
    }

    public DatabaseReference getUserRef(String uid) {
        return firebaseDatabase.getReference(Constants.NODE_USERS).child(uid);
    }

    public DatabaseReference getAddressesRef(String uid) {
        return firebaseDatabase.getReference(Constants.NODE_ADDRESSES).child(uid);
    }

    public DatabaseReference getWishlistsRef(String uid) {
        return firebaseDatabase.getReference(Constants.NODE_WISHLISTS).child(uid);
    }

    public DatabaseReference getCategoriesRef() {
        return firebaseDatabase.getReference(Constants.NODE_CATEGORIES);
    }

    public DatabaseReference getProductsRef() {
        return firebaseDatabase.getReference(Constants.NODE_PRODUCTS);
    }

    public DatabaseReference getProductRef(String productId) {
        return firebaseDatabase.getReference(Constants.NODE_PRODUCTS).child(productId);
    }

    public DatabaseReference getBannersRef() {
        return firebaseDatabase.getReference(Constants.NODE_BANNERS);
    }

    public DatabaseReference getCartsRef(String uid) {
        return firebaseDatabase.getReference(Constants.NODE_CARTS).child(uid);
    }

    public DatabaseReference getOrdersRef() {
        return firebaseDatabase.getReference(Constants.NODE_ORDERS);
    }

    public DatabaseReference getOrderRef(String orderId) {
        return firebaseDatabase.getReference(Constants.NODE_ORDERS).child(orderId);
    }

    public DatabaseReference getReviewsRef(String productId) {
        return firebaseDatabase.getReference(Constants.NODE_REVIEWS).child(productId);
    }

    public DatabaseReference getChatHistoryRef(String uid) {
        return firebaseDatabase.getReference(Constants.NODE_CHAT_HISTORY).child(uid);
    }

    public DatabaseReference getBrowsingHistoryRef(String uid) {
        return firebaseDatabase.getReference(Constants.NODE_BROWSING_HISTORY).child(uid);
    }

    // ─── Storage ─────────────────────────────────────────
    public StorageReference getStorageReference() {
        return firebaseStorage.getReference();
    }

    public StorageReference getProductImagesRef() {
        return firebaseStorage.getReference().child("product_images");
    }

    public StorageReference getUserAvatarsRef() {
        return firebaseStorage.getReference().child("user_avatars");
    }
}
