package com.example.khoga.ui.activity;

import com.example.khoga.R;
import com.example.khoga.model.Address;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CheckoutActivity extends AppCompatActivity {

    private LinearLayout  btnBack;
    private LinearLayout  layoutShippingStandard, layoutShippingExpress;
    private RadioButton   rbStandard, rbExpress;
    private LinearLayout  layoutPaymentCOD, layoutPaymentVnPay;
    private RadioButton   rbCOD, rbVnPay;
    private MaterialButton btnContinue;
    private ImageButton   btnEditAddress;
    private TextView      tvName, tvPhone, tvAddressDetails;

    private double cartTotal    = 0;
    private double shippingFee  = 0;

    private String shippingMethod = "standard";
    private String paymentMethod  = "cod";

    // Địa chỉ đã chọn — sẽ truyền sang ReviewActivity
    private Address selectedAddress;

    // Danh sách itemId đã tick trong giỏ hàng
    private java.util.ArrayList<String> selectedItemIds;

    // Launcher để nhận kết quả từ AddressSelectActivity
    private final ActivityResultLauncher<Intent> addressSelectLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String addressId = result.getData().getStringExtra("SELECTED_ADDRESS_ID");
                    if (addressId != null) {
                        loadAddressById(addressId);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#5A4B75"));
        setContentView(R.layout.activity_checkout);

        cartTotal = getIntent().getDoubleExtra("TOTAL_PRICE", 0);
        selectedItemIds = getIntent().getStringArrayListExtra("SELECTED_ITEM_IDS");

        initViews();

        // Load địa chỉ mặc định
        loadDefaultAddress();

        btnBack.setOnClickListener(v -> finish());

        // Ấn nút edit → mở chọn địa chỉ
        btnEditAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressSelectActivity.class);
            addressSelectLauncher.launch(intent);
        });

        layoutShippingStandard.setOnClickListener(v -> selectShipping(true));
        layoutShippingExpress.setOnClickListener(v -> selectShipping(false));
        layoutPaymentCOD.setOnClickListener(v -> selectPayment(true));
        layoutPaymentVnPay.setOnClickListener(v -> selectPayment(false));

        btnContinue.setOnClickListener(v -> {
            if (selectedAddress == null) {
                Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(CheckoutActivity.this, ReviewActivity.class);
            intent.putExtra("CART_TOTAL",      cartTotal);
            intent.putExtra("SHIPPING_FEE",    shippingFee);
            intent.putExtra("SHIPPING_METHOD", shippingMethod);
            intent.putExtra("PAYMENT_METHOD",  paymentMethod);
            // Truyền địa chỉ
            intent.putExtra("ADDRESS_NAME",    selectedAddress.recipientName);
            intent.putExtra("ADDRESS_PHONE",   selectedAddress.phone);
            intent.putExtra("ADDRESS_PROVINCE", selectedAddress.province);
            intent.putExtra("ADDRESS_DISTRICT", selectedAddress.district);
            intent.putExtra("ADDRESS_WARD",    selectedAddress.ward);
            intent.putExtra("ADDRESS_DETAIL",  selectedAddress.detail);
            intent.putStringArrayListExtra("SELECTED_ITEM_IDS", selectedItemIds);
            startActivity(intent);
        });
    }

    private void loadDefaultAddress() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("addresses").child(user.getUid())
                .orderByChild("isDefault").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Address addr = child.getValue(Address.class);
                            if (addr != null) {
                                selectedAddress = addr;
                                displayAddress(addr);
                                return;
                            }
                        }
                        // Không có địa chỉ mặc định → lấy cái đầu tiên
                        loadFirstAddress();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        displayNoAddress();
                    }
                });
    }

    private void loadFirstAddress() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("addresses").child(user.getUid())
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Address addr = child.getValue(Address.class);
                            if (addr != null) {
                                selectedAddress = addr;
                                displayAddress(addr);
                                return;
                            }
                        }
                        displayNoAddress();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        displayNoAddress();
                    }
                });
    }

    private void loadAddressById(String addressId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("addresses")
                .child(user.getUid()).child(addressId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Address addr = snapshot.getValue(Address.class);
                        if (addr != null) {
                            selectedAddress = addr;
                            displayAddress(addr);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void displayAddress(Address addr) {
        tvName.setText("Họ và tên: " + addr.recipientName);
        tvPhone.setText("Số điện thoại: " + addr.phone);
        String fullAddress = addr.detail;
        if (addr.district != null && !addr.district.isEmpty()) fullAddress += ", " + addr.district;
        if (addr.province != null && !addr.province.isEmpty()) fullAddress += ", " + addr.province;
        tvAddressDetails.setText("Địa chỉ: " + fullAddress);
    }

    private void displayNoAddress() {
        tvName.setText("Chưa có địa chỉ");
        tvPhone.setText("Vui lòng thêm địa chỉ giao hàng");
        tvAddressDetails.setText("Ấn biểu tượng ✏ để chọn");
    }

    private void selectShipping(boolean isStandard) {
        rbStandard.setChecked(isStandard);
        rbExpress.setChecked(!isStandard);
        if (isStandard) {
            layoutShippingStandard.setBackgroundResource(R.drawable.bg_gray_rounded);
            layoutShippingExpress.setBackgroundResource(android.R.color.transparent);
            shippingFee = 0;
            shippingMethod = "standard";
        } else {
            layoutShippingStandard.setBackgroundResource(android.R.color.transparent);
            layoutShippingExpress.setBackgroundResource(R.drawable.bg_gray_rounded);
            shippingFee = 136000;
            shippingMethod = "express";
        }
    }

    private void selectPayment(boolean isCOD) {
        rbCOD.setChecked(isCOD);
        rbVnPay.setChecked(!isCOD);
        if (isCOD) {
            layoutPaymentCOD.setBackgroundResource(R.drawable.bg_gray_rounded);
            layoutPaymentVnPay.setBackgroundResource(android.R.color.transparent);
            paymentMethod = "cod";
        } else {
            layoutPaymentCOD.setBackgroundResource(android.R.color.transparent);
            layoutPaymentVnPay.setBackgroundResource(R.drawable.bg_gray_rounded);
            paymentMethod = "vnpay";
        }
    }

    private void initViews() {
        btnBack                = findViewById(R.id.btnBack);
        btnContinue            = findViewById(R.id.btnContinue);
        btnEditAddress         = findViewById(R.id.btnEditAddress);
        tvName                 = findViewById(R.id.tvName);
        tvPhone                = findViewById(R.id.tvPhone);
        tvAddressDetails       = findViewById(R.id.tvAddressDetails);
        layoutShippingStandard = findViewById(R.id.layoutShippingStandard);
        layoutShippingExpress  = findViewById(R.id.layoutShippingExpress);
        rbStandard             = findViewById(R.id.rbStandard);
        rbExpress              = findViewById(R.id.rbExpress);
        layoutPaymentCOD       = findViewById(R.id.layoutPaymentCOD);
        layoutPaymentVnPay     = findViewById(R.id.layoutPaymentVnPay);
        rbCOD                  = findViewById(R.id.rbCOD);
        rbVnPay                = findViewById(R.id.rbVnPay);
    }
}