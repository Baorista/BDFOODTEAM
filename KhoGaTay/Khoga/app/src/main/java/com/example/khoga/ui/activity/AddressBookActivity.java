package com.example.khoga.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.adapter.AddressAdapter;
import com.example.khoga.model.Address;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddressBookActivity extends AppCompatActivity {

    private RecyclerView recyclerViewAddresses;
    private AddressAdapter adapter;
    private List<Address> addressList;
    private MaterialButton btnAddAddress;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        recyclerViewAddresses = findViewById(R.id.recyclerViewAddresses);
        btnAddAddress = findViewById(R.id.btnAddAddress);
        btnBack = findViewById(R.id.btnBack);

        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(this));
        addressList = new ArrayList<>();

        adapter = new AddressAdapter(addressList, new AddressAdapter.OnAddressClickListener() {
            @Override
            public void onEditClick(Address address) {
                Intent intent = new Intent(AddressBookActivity.this, AddressFormActivity.class);
                intent.putExtra("ADDRESS_ID", address.addressId);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Address address) {
                deleteAddressFromFirebase(address.addressId);
            }
        });

        recyclerViewAddresses.setAdapter(adapter);
        btnBack.setOnClickListener(v -> finish());
        btnAddAddress.setOnClickListener(v ->
                startActivity(new Intent(this, AddressFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddressesFromFirebase();
    }

    private void loadAddressesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("addresses").child(user.getUid());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                addressList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Address address = data.getValue(Address.class);
                    if (address != null) addressList.add(address);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddressBookActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAddressFromFirebase(String addressId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("addresses")
                .child(user.getUid()).child(addressId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show();
                    loadAddressesFromFirebase();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show());
    }
}