package com.example.khoga.ui.activity;

import com.example.khoga.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class OrderSuccessActivity extends AppCompatActivity {

    private MaterialButton btnMyOrders, btnHome;
    private TextView tvOrderId, tvSuccessName, tvSuccessPhone, tvSuccessAddress, tvExpectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#5A4B75"));
        setContentView(R.layout.activity_order_success);

        btnMyOrders = findViewById(R.id.btnMyOrders);
        btnHome = findViewById(R.id.btnHome);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvSuccessName = findViewById(R.id.tvSuccessName);
        tvSuccessPhone = findViewById(R.id.tvSuccessPhone);
        tvSuccessAddress = findViewById(R.id.tvSuccessAddress);
        tvExpectedDate = findViewById(R.id.tvExpectedDate);

        // Nhận dữ liệu
        String orderId = getIntent().getStringExtra("ORDER_ID");
        String addrName = getIntent().getStringExtra("ADDRESS_NAME");
        String addrPhone = getIntent().getStringExtra("ADDRESS_PHONE");
        String addrDetail = getIntent().getStringExtra("ADDRESS_DETAIL");
        String addrDistrict = getIntent().getStringExtra("ADDRESS_DISTRICT");
        String addrProvince = getIntent().getStringExtra("ADDRESS_PROVINCE");
        String shippingMethod = getIntent().getStringExtra("SHIPPING_METHOD");

        // Hiển thị Order ID
        if (tvOrderId != null && orderId != null) {
            tvOrderId.setText("Order #" + orderId.substring(Math.max(0, orderId.length() - 7)));
        }

        // Hiển thị địa chỉ
        if (tvSuccessName != null) {
            tvSuccessName.setText("Họ và tên: " + (addrName != null ? addrName : ""));
        }
        if (tvSuccessPhone != null) {
            tvSuccessPhone.setText("Số điện thoại: " + (addrPhone != null ? addrPhone : ""));
        }
        if (tvSuccessAddress != null) {
            String fullAddress = addrDetail != null ? addrDetail : "";
            if (addrDistrict != null && !addrDistrict.isEmpty()) fullAddress += ", " + addrDistrict;
            if (addrProvince != null && !addrProvince.isEmpty()) fullAddress += ", " + addrProvince;
            tvSuccessAddress.setText("Địa chỉ: " + fullAddress);
        }

        // Ngày giao dự kiến
        if (tvExpectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            int daysMin, daysMax;
            if ("express".equals(shippingMethod)) {
                daysMin = 1; daysMax = 2;
            } else {
                daysMin = 4; daysMax = 5;
            }

            cal.add(Calendar.DAY_OF_MONTH, daysMin);
            String dateFrom = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, daysMax - daysMin);
            String dateTo = sdf.format(cal.getTime());

            tvExpectedDate.setText("Ngày dự kiến giao: " + dateFrom + " - " + dateTo);
        }

        // Xử lý nút back hệ thống
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToHome();
            }
        });

        // Nút Trang chủ
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> goToHome());
        }

        // Nút Đơn mua → chuyển sang lịch sử mua hàng
        if (btnMyOrders != null) {
            btnMyOrders.setOnClickListener(v -> goToOrderList());
        }
    }

    private void goToHome() {
        Intent intent = new Intent(OrderSuccessActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void goToOrderList() {
        Intent intent = new Intent(OrderSuccessActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("NAVIGATE_TO", "orderList");
        startActivity(intent);
        finish();
    }


}