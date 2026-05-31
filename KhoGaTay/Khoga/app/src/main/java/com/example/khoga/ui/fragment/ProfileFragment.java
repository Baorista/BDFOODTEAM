package com.example.khoga.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.khoga.R;
import com.example.khoga.ui.activity.AddressBookActivity;
import com.example.khoga.ui.activity.ChangePasswordActivity;
import com.example.khoga.ui.activity.EditProfileActivity;
import com.example.khoga.ui.activity.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvUserEmail;
    private ImageView imgAvatar;
    private TextView btnEditProfile, btnAddressBook, btnOrderHistory, btnWishlist, btnChangePassword, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        imgAvatar = view.findViewById(R.id.imgAvatar);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnAddressBook = view.findViewById(R.id.btnAddressBook);
        btnOrderHistory = view.findViewById(R.id.btnOrderHistory);
        btnWishlist = view.findViewById(R.id.btnWishlist);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadUserInfo();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        tvUserEmail.setText(user.getEmail());

        FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        String name = snapshot.child("displayName").getValue(String.class);
                        String photoUrl = snapshot.child("photoUrl").getValue(String.class);

                        tvUserName.setText(name != null && !name.isEmpty() ? name : "Người dùng");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(ProfileFragment.this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .into(imgAvatar);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) tvUserName.setText("Người dùng");
                    }
                });
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        btnAddressBook.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddressBookActivity.class)));

        btnOrderHistory.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_global_orderList));

        // Navigate tới WishlistFragment trong nav_graph (thay vì mở Activity)
        btnWishlist.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_global_wishlist));

        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}