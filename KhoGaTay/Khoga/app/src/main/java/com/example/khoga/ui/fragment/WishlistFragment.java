package com.example.khoga.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.adapter.WishlistAdapter;
import com.example.khoga.model.Product;
import com.example.khoga.viewmodel.ProductViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.ViewModelProvider;

public class WishlistFragment extends Fragment {

    private RecyclerView recyclerViewWishlist;
    private TextView tvEmpty;
    private WishlistAdapter adapter;
    private List<Product> wishlistProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wishlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewWishlist = view.findViewById(R.id.recyclerViewWishlist);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        // Nút quay lại
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        recyclerViewWishlist.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new WishlistAdapter(new WishlistAdapter.OnWishlistActionListener() {
            @Override
            public void onRemoveWishlist(Product product) {
                removeFromWishlist(product.getProductId());
            }

            @Override
            public void onProductClick(Product product) {
                // Set product vào ViewModel rồi navigate tới detail
                ProductViewModel vm = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
                vm.selectProduct(product);

                Bundle args = new Bundle();
                args.putString("productId", product.getProductId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_wishlist_to_detail, args);
            }

            @Override
            public void onAddToCart(Product product) {
                addToCart(product);
            }
        });

        recyclerViewWishlist.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("wishlists").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) ids.add(child.getKey());
                        if (ids.isEmpty()) { showEmpty(true); return; }
                        loadProductsByIds(ids);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadProductsByIds(List<String> productIds) {
        FirebaseDatabase.getInstance().getReference("products")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        wishlistProducts.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null && productIds.contains(p.getProductId()))
                                wishlistProducts.add(p);
                        }
                        showEmpty(wishlistProducts.isEmpty());
                        adapter.setProducts(wishlistProducts);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void removeFromWishlist(String productId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseDatabase.getInstance().getReference("wishlists")
                .child(user.getUid()).child(productId).removeValue()
                .addOnSuccessListener(v -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Đã xoá khỏi yêu thích", Toast.LENGTH_SHORT).show();
                        loadWishlist();
                    }
                });
    }

    private void addToCart(Product product) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts").child(user.getUid());
        cartRef.orderByChild("productId").equalTo(product.getProductId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot c : snapshot.getChildren()) {
                                Integer q = c.child("quantity").getValue(Integer.class);
                                c.getRef().child("quantity").setValue((q != null ? q : 0) + 1);
                            }
                            if (isAdded()) Toast.makeText(requireContext(), "Đã tăng số lượng trong giỏ", Toast.LENGTH_SHORT).show();
                        } else {
                            String key = cartRef.push().getKey();
                            if (key == null) return;
                            Map<String, Object> item = new HashMap<>();
                            item.put("productId", product.getProductId());
                            item.put("productName", product.getName());
                            item.put("productImage", product.getFirstImage());
                            item.put("price", product.getDisplayPrice());
                            item.put("quantity", 1);
                            item.put("addedAt", System.currentTimeMillis());
                            cartRef.child(key).setValue(item);
                            if (isAdded()) Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showEmpty(boolean empty) {
        if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewWishlist.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}