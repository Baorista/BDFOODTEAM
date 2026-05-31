package com.example.khoga.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.example.khoga.model.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrderRepository — TV4 đọc đơn hàng + hủy đơn
 *
 * Node: orders/{orderId}
 * TV3 tạo đơn, TV4 chỉ đọc và hủy
 */
public class OrderRepository {

    private static final String TAG = "OrderRepository";
    private final DatabaseReference ordersRef;

    public OrderRepository() {
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
    }

    // ====================================================================
    // 1. LẤY DANH SÁCH ĐƠN HÀNG CỦA USER
    // ====================================================================

    public void getOrdersByUser(String userId, MutableLiveData<List<Order>> liveData) {
        ordersRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Order> orders = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order order = child.getValue(Order.class);
                            if (order != null) {
                                orders.add(order);
                            }
                        }
                        // Sắp xếp mới nhất lên đầu
                        Collections.sort(orders,
                                (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        liveData.postValue(orders);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Load orders failed", error.toException());
                        liveData.postValue(new ArrayList<>());
                    }
                });
    }

    // ====================================================================
    // 2. LẤY CHI TIẾT 1 ĐƠN HÀNG
    // ====================================================================

    public void getOrderById(String orderId, MutableLiveData<Order> liveData) {
        ordersRef.child(orderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Order order = snapshot.getValue(Order.class);
                        liveData.postValue(order);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Load order detail failed", error.toException());
                        liveData.postValue(null);
                    }
                });
    }

    // ====================================================================
    // 3. HỦY ĐƠN HÀNG
    //    - Chỉ cho hủy khi status = "pending" hoặc "confirmed"
    //    - Cập nhật: orderStatus → "cancelled", cancelReason, updatedAt
    // ====================================================================

    public void cancelOrder(String orderId, String cancelReason,
                            OnSuccessListener<Void> onSuccess,
                            OnFailureListener onFailure) {

        // Bước 1: Kiểm tra trạng thái hiện tại
        ordersRef.child(orderId).child("orderStatus")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String currentStatus = snapshot.getValue(String.class);

                        if (!"pending".equals(currentStatus)
                                && !"confirmed".equals(currentStatus)) {
                            onFailure.onFailure(
                                    new Exception("Không thể hủy đơn hàng ở trạng thái này"));
                            return;
                        }

                        // Bước 2: Cập nhật trạng thái
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("orderStatus", "cancelled");
                        updates.put("cancelReason", cancelReason);
                        updates.put("updatedAt", ServerValue.TIMESTAMP);

                        ordersRef.child(orderId).updateChildren(updates)
                                .addOnSuccessListener(onSuccess)
                                .addOnFailureListener(onFailure);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onFailure.onFailure(error.toException());
                    }
                });
    }
}
