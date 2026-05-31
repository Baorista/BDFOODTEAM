package com.example.khoga.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.khoga.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar, btnBack;
    private TextInputEditText edtFullName, edtPhone;
    private MaterialButton btnSaveProfile;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgAvatar.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        imgAvatar = findViewById(R.id.imgAvatar);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        loadCurrentUserData();

        btnSaveProfile.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();
            String name = edtFullName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.isEmpty() && !phone.matches("^0\\d{9}$")) {
                edtPhone.setError("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
                edtPhone.requestFocus();
                return;
            }

            btnSaveProfile.setEnabled(false);

            if (selectedImageUri != null) {
                uploadImageAndSaveProfile(userId, name, phone);
            } else {
                saveProfileToFirebase(userId, name, phone, null);
            }
        });
    }

    private void loadCurrentUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("displayName").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String photoUrl = snapshot.child("photoUrl").getValue(String.class);

                if (name != null) edtFullName.setText(name);
                if (phone != null) edtPhone.setText(phone);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(EditProfileActivity.this)
                            .load(photoUrl)
                            .circleCrop()
                            .into(imgAvatar);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageAndSaveProfile(String userId, String name, String phone) {
        MediaManager.get().upload(selectedImageUri)
                .unsigned("android_app_upload")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveProfileToFirebase(userId, name, phone, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, "Lỗi tải ảnh lên: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch(EditProfileActivity.this);
    }

    private void saveProfileToFirebase(String userId, String name, String phone, String imageUrl) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        Map<String, Object> updates = new HashMap<>();

        if (!name.isEmpty()) updates.put("displayName", name);
        if (!phone.isEmpty()) updates.put("phone", phone);
        if (imageUrl != null) updates.put("photoUrl", imageUrl);
        updates.put("updatedAt", System.currentTimeMillis());

        dbRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu dữ liệu", Toast.LENGTH_SHORT).show();
                });
    }
}