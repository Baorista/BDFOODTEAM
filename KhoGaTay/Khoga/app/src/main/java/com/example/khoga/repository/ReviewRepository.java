package com.example.khoga.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.example.khoga.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewRepository {

    private final DatabaseReference reviewsRef;
    private final DatabaseReference productsRef;
    private final DatabaseReference ordersRef;
    private final FirebaseAuth auth;

    public ReviewRepository() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reviewsRef = db.getReference("reviews");
        productsRef = db.getReference("products");
        ordersRef = db.getReference("orders");
        auth = FirebaseAuth.getInstance();
    }

    // ====================================================================
    // INTERFACE CALLBACKS
    // ====================================================================

    public interface OnReviewSubmitListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnReviewsLoadListener {
        void onSuccess(List<Review> reviews);
        void onFailure(String error);
    }

    public interface OnCheckReviewListener {
        void onResult(boolean alreadyReviewed);
    }

    // ====================================================================
    // 1. KIỂM TRA ĐÃ ĐÁNH GIÁ CHƯA
    //    - Mỗi user chỉ được đánh giá 1 lần cho mỗi sản phẩm trong 1 đơn
    // ====================================================================

    public void checkAlreadyReviewed(String productId, String orderId,
                                     OnCheckReviewListener listener) {
        String userId = auth.getCurrentUser().getUid();

        reviewsRef.child(productId)
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r != null && orderId.equals(r.getOrderId())) {
                                found = true;
                                break;
                            }
                        }
                        listener.onResult(found);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onResult(false);
                    }
                });
    }

    // ====================================================================
    // 1b. ĐỒNG BỘ FLAG reviewed TRONG ORDERS
    //     Gọi khi load đơn hàng delivered để đảm bảo flag khớp thực tế.
    //     Nếu review đã bị xóa khỏi DB, tự động reset orders/{orderId}/reviewed = false.
    // ====================================================================

    public void syncReviewedFlag(String orderId, String productId) {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        reviewsRef.child(productId)
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r != null && orderId.equals(r.getOrderId())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            // Review không còn tồn tại → reset flag để UI cập nhật đúng
                            ordersRef.child(orderId).child("reviewed").setValue(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ====================================================================
    // 2. GỬI ĐÁNH GIÁ (CORE)
    //    - Lưu review vào reviews/{productId}/{reviewId}
    //    - Đánh dấu orders/{orderId}/reviewed = true
    //    - Cập nhật avgRating + totalReviews trong products/{productId}
    //      dùng Transaction để tránh race condition
    // ====================================================================

    public void submitReview(Review review, OnReviewSubmitListener listener) {
        String reviewId = reviewsRef.child(review.getProductId()).push().getKey();
        if (reviewId == null) {
            listener.onFailure("Không thể tạo review ID");
            return;
        }
        review.setReviewId(reviewId);
        review.setCreatedAt(System.currentTimeMillis());

        reviewsRef.child(review.getProductId()).child(reviewId)
                .setValue(review)
                .addOnSuccessListener(unused -> {
                    // Đánh dấu đơn hàng đã được đánh giá để cập nhật UI trong OrderAdapter
                    ordersRef.child(review.getOrderId()).child("reviewed").setValue(true);
                    // Cập nhật avgRating + totalReviews bằng Transaction
                    updateProductRating(review.getProductId(), review.getRating(), listener);
                })
                .addOnFailureListener(e ->
                        listener.onFailure("Lưu đánh giá thất bại: " + e.getMessage()));
    }

    /**
     * Cập nhật avgRating và totalReviews trong products/{productId}
     * Dùng Transaction để đảm bảo tính nhất quán khi nhiều user đánh giá cùng lúc.
     *
     * Công thức:
     *   newTotal = oldTotal + 1
     *   newAvg   = (oldAvg * oldTotal + newRating) / newTotal
     */
    private void updateProductRating(String productId, int newRating,
                                     OnReviewSubmitListener listener) {
        productsRef.child(productId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Double currentAvg = mutableData.child("avgRating").getValue(Double.class);
                Integer currentTotal = mutableData.child("totalReviews").getValue(Integer.class);

                if (currentAvg == null) currentAvg = 0.0;
                if (currentTotal == null) currentTotal = 0;

                int newTotal = currentTotal + 1;
                double newAvg = (currentAvg * currentTotal + newRating) / newTotal;
                newAvg = Math.round(newAvg * 10.0) / 10.0;

                mutableData.child("avgRating").setValue(newAvg);
                mutableData.child("totalReviews").setValue(newTotal);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot currentData) {
                if (error != null) {
                    listener.onFailure("Cập nhật rating thất bại: " + error.getMessage());
                } else {
                    listener.onSuccess();
                }
            }
        });
    }

    // ====================================================================
    // 3. LẤY DANH SÁCH ĐÁNH GIÁ CỦA SẢN PHẨM
    //    - Sắp xếp theo thời gian mới nhất
    // ====================================================================

    public void getReviewsByProduct(String productId, OnReviewsLoadListener listener) {
        reviewsRef.child(productId)
                .orderByChild("createdAt")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Review> reviews = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review review = child.getValue(Review.class);
                            if (review != null) {
                                reviews.add(review);
                            }
                        }
                        java.util.Collections.reverse(reviews);
                        listener.onSuccess(reviews);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onFailure(error.getMessage());
                    }
                });
    }

    // ====================================================================
    // 4. LẤY ĐÁNH GIÁ CỦA USER CHO 1 ĐƠN HÀNG CỤ THỂ
    //    - Dùng khi hiển thị trạng thái "đã đánh giá" trong lịch sử đơn
    // ====================================================================

    public void getUserReviewForOrder(String productId, String orderId,
                                      OnReviewsLoadListener listener) {
        String userId = auth.getCurrentUser().getUid();

        reviewsRef.child(productId)
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Review> reviews = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r != null && orderId.equals(r.getOrderId())) {
                                reviews.add(r);
                            }
                        }
                        listener.onSuccess(reviews);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onFailure(error.getMessage());
                    }
                });
    }
}
