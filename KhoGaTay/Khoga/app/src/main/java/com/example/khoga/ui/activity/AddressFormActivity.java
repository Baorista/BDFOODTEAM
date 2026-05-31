package com.example.khoga.ui.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.khoga.R;
import com.example.khoga.model.Address;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddressFormActivity extends AppCompatActivity {

    private TextInputEditText edtReceiverName, edtPhone, edtCity, edtDistrict, edtDetailAddress;
    private SwitchCompat switchDefault;
    private MaterialButton btnSaveAddress;
    private ImageView btnBack;
    private TextView tvFormTitle;
    private String currentAddressId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_form);

        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtPhone = findViewById(R.id.edtPhone);
        edtCity = findViewById(R.id.edtCity);
        edtDistrict = findViewById(R.id.edtDistrict);
        edtDetailAddress = findViewById(R.id.edtDetailAddress);
        switchDefault = findViewById(R.id.switchDefault);
        btnSaveAddress = findViewById(R.id.btnSaveAddress);
        btnBack = findViewById(R.id.btnBack);
        tvFormTitle = findViewById(R.id.tvFormTitle);

        currentAddressId = getIntent().getStringExtra("ADDRESS_ID");
        if (currentAddressId != null) {
            if (tvFormTitle != null) tvFormTitle.setText("Sửa địa chỉ");
            loadExistingData();
        }

        btnBack.setOnClickListener(v -> finish());
        btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void loadExistingData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("addresses").child(user.getUid()).child(currentAddressId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Address address = snapshot.getValue(Address.class);
                if (address != null) {
                    edtReceiverName.setText(address.recipientName);
                    edtPhone.setText(address.phone);
                    edtCity.setText(address.province);
                    edtDistrict.setText(address.district);
                    edtDetailAddress.setText(address.detail);
                    switchDefault.setChecked(address.isDefault);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddressFormActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAddress() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String name = edtReceiverName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String city = edtCity.getText().toString().trim();
        String district = edtDistrict.getText().toString().trim();
        String detail = edtDetailAddress.getText().toString().trim();
        boolean isDefault = switchDefault.isChecked();

        if (name.isEmpty() || phone.isEmpty() || city.isEmpty() || district.isEmpty() || detail.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^0\\d{9}$")) {
            edtPhone.setError("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
            edtPhone.requestFocus();
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("addresses").child(uid);

        if (currentAddressId == null) {
            currentAddressId = dbRef.push().getKey();
        }

        Address address = new Address();
        address.addressId = currentAddressId;
        address.recipientName = name;
        address.phone = phone;
        address.province = city;
        address.district = district;
        address.ward = "";
        address.detail = detail;
        address.isDefault = isDefault;

        if (isDefault) {
            dbRef.orderByChild("isDefault").equalTo(true)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                if (!data.getKey().equals(address.addressId)) {
                                    data.getRef().child("isDefault").setValue(false);
                                }
                            }
                            pushAddress(dbRef, address);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(AddressFormActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            pushAddress(dbRef, address);
        }
    }

    private void pushAddress(DatabaseReference dbRef, Address address) {
        dbRef.child(address.addressId).setValue(address)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Lưu địa chỉ thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lưu dữ liệu", Toast.LENGTH_SHORT).show());
    }
}