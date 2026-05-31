package com.example.khoga.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.example.khoga.R;
import com.example.khoga.model.Order;
import com.example.khoga.viewmodel.OrderViewModel;

import java.util.Locale;

/**
 * OrderDetailActivity — Chi tiết đơn hàng (theo Figma)
 *
 * Figma layout:
 * ┌───────────────────────────────┐
 * │ « Quay lại    Chi tiết đơn    │
 * │                               │
 * │ Chi tiết địa chỉ giao hàng   │
 * │ Họ và tên: ...                │
 * │ Số điện thoại: ...            │
 * │ Địa chỉ: ...                 │
 * │                               │
 * │ [Ảnh SP] Tên SP   202.202đ x1│
 * │                               │
 * │ Chi tiết đơn hàng             │
 * │ Tổng tiền:        202.202đ    │
 * │ Shipping:   Tiêu chuẩn - Free │
 * │ Phương thức thanh toán: COD   │
 * │                               │
 * │      [Hủy đơn hàng]           │
 * └───────────────────────────────┘
 */
public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvRecipientName, tvPhone, tvAddress;
    private LinearLayout layoutItems;
    private TextView tvSubtotal, tvShipping, tvPaymentMethod;
    private MaterialButton btnAction;

    private OrderViewModel orderViewModel;
    private String orderId;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Thiếu thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupViewModel();
    }

    private void initViews() {
        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        layoutItems = findViewById(R.id.layoutOrderItems);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShipping = findViewById(R.id.tvShipping);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        btnAction = findViewById(R.id.btnOrderAction);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupViewModel() {
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        orderViewModel.getSelectedOrder().observe(this, order -> {
            if (order != null) {
                currentOrder = order;
                displayOrder(order);
            }
        });

        orderViewModel.getCancelSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        orderViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        orderViewModel.loadOrderById(orderId);
    }

    // ================================================================
    // HIỂN THỊ THÔNG TIN ĐƠN HÀNG
    // ================================================================

    private void displayOrder(Order order) {
        // === Địa chỉ giao hàng ===
        Order.ShippingAddress addr = order.getShippingAddress();
        if (addr != null) {
            tvRecipientName.setText("Họ và tên: " + nvl(addr.getRecipientName()));
            tvPhone.setText("Số điện thoại: " + nvl(addr.getPhone()));
            tvAddress.setText("Địa chỉ: " + nvl(addr.getFullAddress()));
        }

        // === Danh sách sản phẩm ===
        layoutItems.removeAllViews();
        if (order.getItems() != null) {
            for (Order.OrderItem item : order.getItems()) {
                addItemView(item);
            }
        }

        // === Chi tiết thanh toán ===
        tvSubtotal.setText(formatCurrency(order.getSubtotal()));

        if (order.getShippingFee() == 0) {
            tvShipping.setText("Tiêu chuẩn - Miễn phí");
        } else {
            tvShipping.setText("Tiêu chuẩn - " + formatCurrency(order.getShippingFee()));
        }

        String paymentLabel = "COD";
        if ("zalopay".equals(order.getPaymentMethod())) paymentLabel = "ZaloPay";
        else if ("momo".equals(order.getPaymentMethod())) paymentLabel = "MoMo";
        tvPaymentMethod.setText(paymentLabel);

        // === Button action theo trạng thái ===
        setupActionButton(order);
    }

    /**
     * Thêm 1 dòng sản phẩm vào layoutItems
     */
    private void addItemView(Order.OrderItem item) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_order_detail_product, layoutItems, false);

        ImageView imgProduct = view.findViewById(R.id.imgDetailProduct);
        TextView tvName = view.findViewById(R.id.tvDetailProductName);
        TextView tvPrice = view.findViewById(R.id.tvDetailProductPrice);
        TextView tvQty = view.findViewById(R.id.tvDetailQuantity);

        tvName.setText(item.getProductName());
        tvPrice.setText(formatCurrency(item.getPrice()));
        tvQty.setText("x" + item.getQuantity());

        if (item.getProductImage() != null) {
            Glide.with(this)
                    .load(item.getProductImage())
                    .centerCrop()
                    .placeholder(R.drawable.bg_rounded_8dp)
                    .into(imgProduct);
        }

        layoutItems.addView(view);
    }

    // ================================================================
    // BUTTON ACTION THEO TRẠNG THÁI
    // ================================================================

    private void setupActionButton(Order order) {
        if (order.canCancel()) {
            // Pending / Confirmed → nút "Hủy đơn hàng"
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText("Hủy đơn hàng");
            btnAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF44336)); // Đỏ

            btnAction.setOnClickListener(v -> {
                CancelOrderDialog dialog =
                        CancelOrderDialog.newInstance(order.getOrderId());
                dialog.setOnCancelConfirmListener((id, reason) -> {
                    orderViewModel.cancelOrder(id, reason);
                });
                dialog.show(getSupportFragmentManager(), "cancel");
            });

        } else if (order.isDelivered()) {
            // Delivered → nút "Đánh giá sản phẩm"
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText("Đánh giá sản phẩm");
            btnAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF7B5EBF)); // Tím

            btnAction.setOnClickListener(v -> {
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    Order.OrderItem firstItem = order.getItems().get(0);

                    Intent intent = new Intent(this,
                            com.example.khoga.ui.activity.ReviewActivity.class);
                    intent.putExtra("productId", firstItem.getProductId());
                    intent.putExtra("productName", firstItem.getProductName());
                    intent.putExtra("productImage", firstItem.getProductImage());
                    intent.putExtra("orderId", order.getOrderId());
                    startActivity(intent);
                }
            });

        } else {
            // Shipping / Cancelled → ẩn nút
            btnAction.setVisibility(View.GONE);
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%,.0fđ", amount);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
