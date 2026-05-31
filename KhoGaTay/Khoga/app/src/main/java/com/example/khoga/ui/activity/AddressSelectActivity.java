package com.example.khoga.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.model.Address;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị danh sách địa chỉ để người dùng chọn cho đơn hàng.
 * Trả về addressId qua setResult.
 */
public class AddressSelectActivity extends AppCompatActivity {

    private RecyclerView rvAddresses;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private List<Address> addressList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#5A4B75"));
        setContentView(R.layout.activity_address_select);

        btnBack = findViewById(R.id.btnBack);
        rvAddresses = findViewById(R.id.rvAddresses);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        btnBack.setOnClickListener(v -> finish());

        loadAddresses();
    }

    private void loadAddresses() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("addresses").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        addressList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Address addr = child.getValue(Address.class);
                            if (addr != null) addressList.add(addr);
                        }

                        if (addressList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvAddresses.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvAddresses.setVisibility(View.VISIBLE);
                            rvAddresses.setAdapter(new SelectAdapter());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddressSelectActivity.this, "Lỗi tải địa chỉ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Adapter hiển thị danh sách địa chỉ — click để chọn
    private class SelectAdapter extends RecyclerView.Adapter<SelectAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_address_select, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Address addr = addressList.get(position);
            holder.tvName.setText(addr.recipientName);
            holder.tvPhone.setText(addr.phone);

            String full = addr.detail;
            if (addr.district != null && !addr.district.isEmpty()) full += ", " + addr.district;
            if (addr.province != null && !addr.province.isEmpty()) full += ", " + addr.province;
            holder.tvAddress.setText(full);

            holder.tvDefault.setVisibility(addr.isDefault ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                // Trả kết quả về CheckoutActivity
                Intent result = new Intent();
                result.putExtra("SELECTED_ADDRESS_ID", addr.addressId);
                setResult(RESULT_OK, result);
                finish();
            });
        }

        @Override
        public int getItemCount() { return addressList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvAddress, tvDefault;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvPhone = v.findViewById(R.id.tvPhone);
                tvAddress = v.findViewById(R.id.tvAddress);
                tvDefault = v.findViewById(R.id.tvDefaultBadge);
            }
        }
    }
}