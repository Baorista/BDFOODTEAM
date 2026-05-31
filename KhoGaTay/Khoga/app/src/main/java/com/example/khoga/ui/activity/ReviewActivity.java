package com.example.khoga.ui.activity;

import com.example.khoga.R;
import com.example.khoga.model.CartItem;
import com.example.khoga.model.Order;
import com.example.khoga.payment.VnPayHelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {

    private LinearLayout   btnBack;
    private MaterialButton btnSubmitOrder;
    private TextView       tvReviewSubtotal, tvReviewShipping, tvReviewPayment, tvReviewFinalTotal;
    private TextView       tvReviewName, tvReviewPhone, tvReviewAddress;

    private boolean isSubmitting = false;

    private double finalTotal;
    private double cartTotal;
    private double shippingFee;
    private String shippingMethod;
    private String paymentMethod;
    private String currentUserId;

    // Địa chỉ nhận từ CheckoutActivity
    private String addrName, addrPhone, addrProvince, addrDistrict, addrWard, addrDetail;

    // Danh sách itemId đã tick từ CartFragment
    private java.util.ArrayList<String> selectedItemIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#5A4B75"));
        setContentView(R.layout.activity_review);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        initViews();
        btnBack.setOnClickListener(v -> finish());

        // Nhận dữ liệu đơn hàng
        cartTotal      = getIntent().getDoubleExtra("CART_TOTAL", 0);
        shippingFee    = getIntent().getDoubleExtra("SHIPPING_FEE", 0);
        shippingMethod = getIntent().getStringExtra("SHIPPING_METHOD");
        paymentMethod  = getIntent().getStringExtra("PAYMENT_METHOD");
        finalTotal     = cartTotal + shippingFee;

        // Nhận địa chỉ từ CheckoutActivity
        addrName     = getIntent().getStringExtra("ADDRESS_NAME");
        addrPhone    = getIntent().getStringExtra("ADDRESS_PHONE");
        addrProvince = getIntent().getStringExtra("ADDRESS_PROVINCE");
        addrDistrict = getIntent().getStringExtra("ADDRESS_DISTRICT");
        addrWard     = getIntent().getStringExtra("ADDRESS_WARD");
        addrDetail   = getIntent().getStringExtra("ADDRESS_DETAIL");
        selectedItemIds = getIntent().getStringArrayListExtra("SELECTED_ITEM_IDS");

        // Hiển thị địa chỉ giao hàng
        tvReviewName.setText("Họ và tên: " + (addrName != null ? addrName : ""));
        tvReviewPhone.setText("Số điện thoại: " + (addrPhone != null ? addrPhone : ""));
        String fullAddress = addrDetail != null ? addrDetail : "";
        if (addrDistrict != null && !addrDistrict.isEmpty()) fullAddress += ", " + addrDistrict;
        if (addrProvince != null && !addrProvince.isEmpty()) fullAddress += ", " + addrProvince;
        tvReviewAddress.setText("Địa chỉ: " + fullAddress);

        // Hiển thị chi tiết đơn hàng
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        tvReviewSubtotal.setText(formatter.format(cartTotal) + "đ");
        tvReviewShipping.setText(shippingMethod);
        tvReviewPayment.setText(paymentMethod);
        tvReviewFinalTotal.setText(formatter.format(finalTotal) + "đ");

        btnSubmitOrder.setOnClickListener(v -> {
            if (isSubmitting) return;
            isSubmitting = true;
            btnSubmitOrder.setEnabled(false);
            btnSubmitOrder.setText("Đang xử lý...");

            if ("vnpay".equals(paymentMethod) || "momo".equals(paymentMethod)) {
                requestOnlinePayment();
            } else {
                saveOrderToFirebase("pending");
            }
        });
    }

    private String vnpayTxnRef;

    private void requestOnlinePayment() {
        try {
            vnpayTxnRef = System.currentTimeMillis() + "_" + (int)(Math.random() * 9000 + 1000);
            long amountLong = Math.round(finalTotal);
            String paymentUrl = VnPayHelper.generatePaymentUrl(amountLong, vnpayTxnRef);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl)));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khởi tạo thanh toán", Toast.LENGTH_SHORT).show();
            isSubmitting = false;
            btnSubmitOrder.setEnabled(true);
            btnSubmitOrder.setText("Tiếp tục");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null && "myapp".equals(data.getScheme()) && "vnpay".equals(data.getHost())) {
            // Verify hash chống giả mạo
            if (!VnPayHelper.verifyReturnUrl(data)) {
                Toast.makeText(this, "Chữ ký không hợp lệ!", Toast.LENGTH_LONG).show();
                isSubmitting = false;
                btnSubmitOrder.setEnabled(true);
                btnSubmitOrder.setText("Tiếp tục");
                return;
            }

            String responseCode = data.getQueryParameter("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                saveOrderToFirebase("paid");
            } else {
                String msg = "Giao dịch thất bại";
                if ("24".equals(responseCode)) msg = "Bạn đã hủy giao dịch";
                else if ("11".equals(responseCode)) msg = "Giao dịch hết hạn";
                else if ("51".equals(responseCode)) msg = "Tài khoản không đủ số dư";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                isSubmitting = false;
                btnSubmitOrder.setEnabled(true);
                btnSubmitOrder.setText("Tiếp tục");
            }
        }
    }

    private void saveOrderToFirebase(String initialPaymentStatus) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        String orderId = ordersRef.push().getKey();
        if (orderId == null) {
            Toast.makeText(this, "Lỗi tạo mã đơn hàng", Toast.LENGTH_SHORT).show();
            isSubmitting = false;
            btnSubmitOrder.setEnabled(true);
            btnSubmitOrder.setText("Tiếp tục");
            return;
        }

        long now = System.currentTimeMillis();

        FirebaseDatabase.getInstance().getReference("carts")
                .child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Order.OrderItem> itemsList = new ArrayList<>();
                        // Lưu productId → quantity để check stock + trừ stock
                        Map<String, Integer> productQuantities = new HashMap<>();
                        // Lưu key item đã chọn để xóa khỏi cart sau
                        List<String> itemKeysToRemove = new ArrayList<>();

                        for (DataSnapshot itemSnap : snapshot.getChildren()) {
                            String itemKey = itemSnap.getKey();
                            // CHỈ lấy item đã tick ở CartFragment
                            if (selectedItemIds != null && !selectedItemIds.contains(itemKey)) {
                                continue;
                            }
                            CartItem cartItem = itemSnap.getValue(CartItem.class);
                            if (cartItem != null) {
                                Order.OrderItem orderItem = new Order.OrderItem(
                                        cartItem.getProductId(),
                                        cartItem.getProductName(),
                                        cartItem.getProductImage(),
                                        cartItem.getPrice(),
                                        cartItem.getQuantity(),
                                        cartItem.getSelectedColor(),
                                        cartItem.getSelectedSize()
                                );
                                itemsList.add(orderItem);
                                // Cộng dồn quantity nếu trùng productId
                                int existing = productQuantities.containsKey(cartItem.getProductId())
                                        ? productQuantities.get(cartItem.getProductId()) : 0;
                                productQuantities.put(cartItem.getProductId(),
                                        existing + cartItem.getQuantity());
                                itemKeysToRemove.add(itemSnap.getKey());
                            }
                        }

                        if (itemsList.isEmpty()) {
                            Toast.makeText(ReviewActivity.this,
                                    "Không có sản phẩm nào được chọn", Toast.LENGTH_SHORT).show();
                            isSubmitting = false;
                            btnSubmitOrder.setEnabled(true);
                            btnSubmitOrder.setText("Tiếp tục");
                            return;
                        }

                        // ── Kiểm tra tồn kho trước khi tạo đơn ──────────
                        DatabaseReference productsRef = FirebaseDatabase.getInstance()
                                .getReference("products");
                        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot prodSnapshot) {
                                for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
                                    String productId = entry.getKey();
                                    int requestedQty = entry.getValue();
                                    DataSnapshot pSnap = prodSnapshot.child(productId);

                                    Integer stock = pSnap.child("stock").getValue(Integer.class);
                                    String pName = pSnap.child("name").getValue(String.class);
                                    if (pName == null) pName = productId;

                                    if (stock == null || stock < requestedQty) {
                                        int available = (stock != null) ? stock : 0;
                                        Toast.makeText(ReviewActivity.this,
                                                "\"" + pName + "\" chỉ còn " + available
                                                        + " sản phẩm, bạn đang đặt " + requestedQty,
                                                Toast.LENGTH_LONG).show();
                                        isSubmitting = false;
                                        btnSubmitOrder.setEnabled(true);
                                        btnSubmitOrder.setText("Tiếp tục");
                                        return;
                                    }
                                }

                                // ── Tạo Order ────────────────────────────
                                Order.ShippingAddress address = new Order.ShippingAddress(
                                        addrName != null ? addrName : "Người dùng",
                                        addrPhone != null ? addrPhone : "",
                                        addrProvince != null ? addrProvince : "",
                                        addrDistrict != null ? addrDistrict : "",
                                        addrWard != null ? addrWard : "",
                                        addrDetail != null ? addrDetail : ""
                                );

                                Order newOrder = new Order(
                                        orderId, currentUserId, itemsList,
                                        cartTotal, shippingFee, address,
                                        shippingMethod, paymentMethod,
                                        initialPaymentStatus, "pending", now, now
                                );

                                ordersRef.child(orderId).setValue(newOrder)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                // ── Trừ stock + cộng totalSold ──
                                                for (Map.Entry<String, Integer> entry
                                                        : productQuantities.entrySet()) {
                                                    String pid = entry.getKey();
                                                    int qty = entry.getValue();

                                                    productsRef.child(pid).child("stock")
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot s) {
                                                                    Integer cur = s.getValue(Integer.class);
                                                                    if (cur != null) {
                                                                        s.getRef().setValue(Math.max(0, cur - qty));
                                                                    }
                                                                }
                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError e) {}
                                                            });

                                                    productsRef.child(pid).child("totalSold")
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot s) {
                                                                    Integer sold = s.getValue(Integer.class);
                                                                    s.getRef().setValue((sold != null ? sold : 0) + qty);
                                                                }
                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError e) {}
                                                            });
                                                }

                                                // ── Xóa CHỈ item đã chọn khỏi cart ──
                                                DatabaseReference cartDelRef = FirebaseDatabase.getInstance()
                                                        .getReference("carts").child(currentUserId);
                                                for (String key : itemKeysToRemove) {
                                                    cartDelRef.child(key).removeValue();
                                                }

                                                // ── Chuyển sang thành công + clear stack ──
                                                Intent intent = new Intent(ReviewActivity.this,
                                                        OrderSuccessActivity.class);
                                                intent.putExtra("ORDER_ID", orderId);
                                                intent.putExtra("ADDRESS_NAME", addrName);
                                                intent.putExtra("ADDRESS_PHONE", addrPhone);
                                                intent.putExtra("ADDRESS_DETAIL", addrDetail);
                                                intent.putExtra("ADDRESS_DISTRICT", addrDistrict);
                                                intent.putExtra("ADDRESS_PROVINCE", addrProvince);
                                                intent.putExtra("SHIPPING_METHOD", shippingMethod);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finishAffinity();
                                            } else {
                                                Toast.makeText(ReviewActivity.this,
                                                        "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                                                isSubmitting = false;
                                                btnSubmitOrder.setEnabled(true);
                                                btnSubmitOrder.setText("Tiếp tục");
                                            }
                                        });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(ReviewActivity.this,
                                        "Lỗi kiểm tra tồn kho", Toast.LENGTH_SHORT).show();
                                isSubmitting = false;
                                btnSubmitOrder.setEnabled(true);
                                btnSubmitOrder.setText("Tiếp tục");
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ReviewActivity.this,
                                "Lỗi đọc giỏ hàng", Toast.LENGTH_SHORT).show();
                        isSubmitting = false;
                        btnSubmitOrder.setEnabled(true);
                        btnSubmitOrder.setText("Tiếp tục");
                    }
                });
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        btnSubmitOrder     = findViewById(R.id.btnSubmitOrder);
        tvReviewName       = findViewById(R.id.tvReviewName);
        tvReviewPhone      = findViewById(R.id.tvReviewPhone);
        tvReviewAddress    = findViewById(R.id.tvReviewAddress);
        tvReviewSubtotal   = findViewById(R.id.tvReviewSubtotal);
        tvReviewShipping   = findViewById(R.id.tvReviewShipping);
        tvReviewPayment    = findViewById(R.id.tvReviewPayment);
        tvReviewFinalTotal = findViewById(R.id.tvReviewFinalTotal);
    }
}