package com.example.khoga.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.example.khoga.model.BrowsingHistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * BrowsingHistoryRepository — Ghi lại sản phẩm user đã xem
 *
 * Node: browsingHistory/{userId}/{productId}
 * Dùng Firebase Transaction để đảm bảo viewCount chính xác
 */
public class BrowsingHistoryRepository {

    private static final String TAG = "BrowsingHistoryRepo";
    private static final long EXPIRY_MS = 30L * 24 * 60 * 60 * 1000; // 30 ngày

    private final DatabaseReference browsingRef;

    public BrowsingHistoryRepository() {
        browsingRef = FirebaseDatabase.getInstance().getReference("browsingHistory");
    }

    // ====================================================================
    // 1. GHI LẠI LƯỢT XEM SẢN PHẨM (Transaction)
    // ====================================================================

    /**
     * Ghi lại lượt xem sản phẩm
     * - Nếu đã có: tăng viewCount + 1
     * - Nếu chưa có: tạo mới với viewCount = 1
     * - Bỏ qua nếu user là admin
     */
    public void recordView(String userId, String productId,
                       String categoryId, String productName) {

        DatabaseReference ref = browsingRef.child(userId).child(productId);

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                BrowsingHistory bh = currentData.getValue(BrowsingHistory.class);

                if (bh == null) {
                    bh = new BrowsingHistory(productId, categoryId, productName);
                } else {
                    bh.setViewCount(bh.getViewCount() + 1);
                    bh.setLastViewedAt(System.currentTimeMillis());
                }

                currentData.setValue(bh);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "recordView transaction failed", error.toException());
                }
            }
        });
    }

    // ====================================================================
    // 2. LẤY TOP SẢN PHẨM ĐÃ XEM (cho AI context)
    // ====================================================================

    public void getTopViewedProducts(String userId, int limit,
                                     OnSuccessListener<List<BrowsingHistory>> listener) {
        browsingRef.child(userId)
                .orderByChild("viewCount")
                .limitToLast(limit)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<BrowsingHistory> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            BrowsingHistory bh = child.getValue(BrowsingHistory.class);
                            if (bh != null) list.add(bh);
                        }
                        // Sort giảm dần theo viewCount
                        Collections.sort(list, (a, b) -> b.getViewCount() - a.getViewCount());
                        listener.onSuccess(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onSuccess(new ArrayList<>());
                    }
                });
    }

    // ====================================================================
    // 3. XÓA LỊCH SỬ HẾT HẠN (> 30 ngày)
    // ====================================================================

    public void cleanExpiredHistory(String userId) {
        long expiredBefore = System.currentTimeMillis() - EXPIRY_MS;

        browsingRef.child(userId)
                .orderByChild("lastViewedAt")
                .endAt(expiredBefore)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }
                        long count = snapshot.getChildrenCount();
                        if (count > 0) {
                            Log.d(TAG, "Cleaned " + count + " expired browsing entries");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
