package com.example.khoga.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.adapter.CartAdapter;
import com.example.khoga.model.CartItem;
import com.example.khoga.ui.activity.CheckoutActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private RecyclerView rcvCart;
    private CartAdapter cartAdapter;
    private List<CartItem> cartList;

    private TextView tvSubtotal, tvVAT, tvFinalTotal;
    private MaterialButton btnCheckout;

    private double finalTotalPrice = 0;
    private String currentUserId;

    private ValueEventListener cartListener;
    private DatabaseReference cartRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Dùng FirebaseAuth để lấy userId thật
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            currentUserId = "guest";
        }

        // Ánh xạ view
        rcvCart      = view.findViewById(R.id.rcvCart);
        tvSubtotal   = view.findViewById(R.id.tvSubtotal);
        tvVAT        = view.findViewById(R.id.tvVAT);
        tvFinalTotal = view.findViewById(R.id.tvFinalTotal);
        btnCheckout  = view.findViewById(R.id.btnCheckout);

        rcvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartList = new ArrayList<>();

        cartRef = FirebaseDatabase.getInstance()
                .getReference("carts").child(currentUserId);

        cartAdapter = new CartAdapter(cartList, cartRef, this::calculateTotal);
        rcvCart.setAdapter(cartAdapter);

        // Lắng nghe thay đổi giỏ hàng từ Firebase
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartList.clear();
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    cartAdapter.syncSelectionWithList();
                    cartAdapter.notifyDataSetChanged();
                    calculateTotal();
                    return;
                }

                long totalItems = snapshot.getChildrenCount();
                final int[] processedCount = {0};

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item == null) {
                        processedCount[0]++;
                        continue;
                    }
                    item.setItemId(itemSnapshot.getKey());

                    // Sync giá mới nhất + check isActive
                    FirebaseDatabase.getInstance().getReference("products")
                            .child(item.getProductId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot prodSnap) {
                                    if (!prodSnap.exists()) {
                                        // SP đã bị xóa → xóa khỏi cart
                                        cartRef.child(item.getItemId()).removeValue();
                                        checkCartDone(processedCount, (int) totalItems);
                                        return;
                                    }

                                    // Check isActive
                                    Boolean isActive = prodSnap.child("isActive")
                                            .getValue(Boolean.class);
                                    if (isActive != null && !isActive) {
                                        cartRef.child(item.getItemId()).removeValue();
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(),
                                                    "\"" + item.getProductName()
                                                            + "\" đã ngừng bán",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        checkCartDone(processedCount, (int) totalItems);
                                        return;
                                    }

                                    // Sync giá mới nhất
                                    Double currentPrice = prodSnap.child("price")
                                            .getValue(Double.class);
                                    Double currentSalePrice = prodSnap.child("salePrice")
                                            .getValue(Double.class);
                                    if (currentPrice != null) {
                                        double displayPrice;
                                        if (currentSalePrice != null
                                                && currentSalePrice > 0
                                                && currentSalePrice < currentPrice) {
                                            displayPrice = currentSalePrice;
                                        } else {
                                            displayPrice = currentPrice;
                                        }
                                        if (Math.abs(item.getPrice() - displayPrice) > 0.01) {
                                            item.setPrice(displayPrice);
                                            cartRef.child(item.getItemId())
                                                    .child("price").setValue(displayPrice);
                                        }
                                    }

                                    cartList.add(item);
                                    checkCartDone(processedCount, (int) totalItems);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    cartList.add(item);
                                    checkCartDone(processedCount, (int) totalItems);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "Lỗi tải giỏ hàng: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        cartRef.addValueEventListener(cartListener);

        // Nút Checkout
        btnCheckout.setOnClickListener(v -> {
            // Lấy danh sách item đã tick từ adapter
            List<CartItem> selectedItems = cartAdapter.getSelectedItems();

            if (cartList.isEmpty()) {
                Toast.makeText(requireContext(), "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 sản phẩm!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            intent.putExtra("TOTAL_PRICE", finalTotalPrice);

            // Truyền danh sách itemId đã tick để Checkout → Review biết xử lý item nào
            ArrayList<String> selectedItemIds = new ArrayList<>();
            for (CartItem item : selectedItems) {
                selectedItemIds.add(item.getItemId());
            }
            intent.putStringArrayListExtra("SELECTED_ITEM_IDS", selectedItemIds);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
        super.onDestroyView();
    }

    // Helper: kiểm tra đã xử lý hết tất cả item chưa
    private void checkCartDone(int[] processedCount, int total) {
        processedCount[0]++;
        if (processedCount[0] >= total) {
            cartAdapter.syncSelectionWithList(); // tự tick item mới, bỏ item đã xóa
            cartAdapter.notifyDataSetChanged();
            calculateTotal();
        }
    }

    private void calculateTotal() {
        double subTotal = 0;
        for (CartItem item : cartList) {
            // Dùng adapter.isSelected() thay vì item.isSelected()
            // vì checkbox cập nhật selectedIds trong adapter, không cập nhật field trên model
            if (cartAdapter.isSelected(item.getItemId())) {
                subTotal += item.getPrice() * item.getQuantity();
            }
        }

        // Phí dịch vụ cố định (không phải VAT)
        double serviceFee = subTotal > 0 ? 10000 : 0;
        finalTotalPrice = subTotal + serviceFee;

        DecimalFormat formatter = new DecimalFormat("###,###,###");
        tvSubtotal.setText(formatter.format(subTotal) + "đ");
        tvVAT.setText(formatter.format(serviceFee) + "đ");
        tvFinalTotal.setText(formatter.format(finalTotalPrice) + "đ");
    }
}