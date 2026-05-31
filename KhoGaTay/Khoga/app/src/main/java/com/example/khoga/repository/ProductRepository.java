package com.example.khoga.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.khoga.model.Banner;
import com.example.khoga.model.Category;
import com.example.khoga.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private static final String TAG = "ProductRepository";

    // ── Callback interfaces ──────────────────────
    public interface OnSuccessCallback<T> {
        void onSuccess(T data);
    }

    public interface OnErrorCallback {
        void onError(String message);
    }

    private final DatabaseReference db;

    public ProductRepository() {
        db = FirebaseDatabase.getInstance().getReference();
    }

    // ─────────────────────────────────────────────
    // CATEGORIES
    // ─────────────────────────────────────────────

    public void loadCategories(OnSuccessCallback<List<Category>> onSuccess,
                               OnErrorCallback onError) {
        db.child("categories")
                .orderByChild("order")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Category> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Category c = child.getValue(Category.class);
                            if (c != null) list.add(c);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────
    // BANNERS
    // ─────────────────────────────────────────────

    public void loadActiveBanners(OnSuccessCallback<List<Banner>> onSuccess,
                                  OnErrorCallback onError) {
        db.child("banners")
                .orderByChild("order")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Banner> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Banner b = child.getValue(Banner.class);
                            if (b != null && b.isActive()) list.add(b);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────
    // PRODUCTS — parse an toàn, không crash khi data lỗi
    // ─────────────────────────────────────────────

    public void loadProducts(OnSuccessCallback<List<Product>> onSuccess,
                             OnErrorCallback onError) {
        db.child("products")
                .orderByChild("createdAt")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Product> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = parseProduct(child);
                            if (p != null && p.isActive()) list.add(p);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    public void loadProductsByCategory(String categoryId,
                                       OnSuccessCallback<List<Product>> onSuccess,
                                       OnErrorCallback onError) {
        db.child("products")
                .orderByChild("categoryId")
                .equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Product> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = parseProduct(child);
                            if (p != null && p.isActive()) list.add(p);
                        }
                        onSuccess.onSuccess(list);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    public void getProductDetail(String productId,
                                 OnSuccessCallback<Product> onSuccess,
                                 OnErrorCallback onError) {
        db.child("products").child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Product p = parseProduct(snapshot);
                        if (p != null) onSuccess.onSuccess(p);
                        else onError.onError("Không tìm thấy sản phẩm.");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onError.onError(error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────
    // PARSE PRODUCT AN TOÀN
    // Firebase CustomClassMapper crash khi field List<String>
    // nhận được String đơn lẻ từ database.
    // Giải pháp: thử auto-parse trước, nếu fail thì parse thủ công.
    // ─────────────────────────────────────────────

    private Product parseProduct(DataSnapshot snap) {
        try {
            // Thử cách nhanh: auto deserialize
            return snap.getValue(Product.class);
        } catch (Exception e) {
            // Nếu lỗi (thường do images/colors/sizes là String thay vì List)
            // → parse thủ công từng field
            Log.w(TAG, "Auto-parse failed for " + snap.getKey() + ", fallback to manual parse", e);
            try {
                Product p = new Product();
                p.setProductId(getStr(snap, "productId"));
                p.setName(getStr(snap, "name"));
                p.setDescription(getStr(snap, "description"));
                p.setCategoryId(getStr(snap, "categoryId"));
                p.setBrand(getStr(snap, "brand"));
                p.setPrice(getDbl(snap, "price"));
                p.setSalePrice(getDbl(snap, "salePrice"));
                p.setStock(getInt(snap, "stock"));
                p.setAvgRating(getDbl(snap, "avgRating"));
                p.setTotalReviews(getInt(snap, "totalReviews"));
                p.setTotalSold(getInt(snap, "totalSold"));
                p.setCreatedAt(getLng(snap, "createdAt"));

                // Boolean đặc biệt
                Boolean active = snap.child("isActive").getValue(Boolean.class);
                p.setActive(active != null ? active : true);

                // List fields — xử lý cả String lẫn List
                p.setImages(snap.child("images").getValue());
                p.setColors(snap.child("colors").getValue());
                p.setSizes(snap.child("sizes").getValue());

                return p;
            } catch (Exception ex) {
                Log.e(TAG, "Manual parse also failed for " + snap.getKey(), ex);
                return null;
            }
        }
    }

    // Helper đọc field an toàn
    private String getStr(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        return v != null ? v.toString() : "";
    }

    private double getDbl(DataSnapshot snap, String key) {
        Double v = snap.child(key).getValue(Double.class);
        return v != null ? v : 0.0;
    }

    private int getInt(DataSnapshot snap, String key) {
        Long v = snap.child(key).getValue(Long.class);
        return v != null ? v.intValue() : 0;
    }

    private long getLng(DataSnapshot snap, String key) {
        Long v = snap.child(key).getValue(Long.class);
        return v != null ? v : 0L;
    }

    // ─────────────────────────────────────────────
    // SEARCH (client-side)
    // ─────────────────────────────────────────────

    public void searchProducts(String query,
                               OnSuccessCallback<List<Product>> onSuccess,
                               OnErrorCallback onError) {
        loadProducts(all -> {
            List<Product> result = new ArrayList<>();
            String lower = removeDiacritics(query);
            for (Product p : all) {
                String name  = removeDiacritics(p.getName());
                String brand = removeDiacritics(p.getBrand());
                if (name.contains(lower) || brand.contains(lower)) {
                    result.add(p);
                }
            }
            onSuccess.onSuccess(result);
        }, onError);
    }

    // Bỏ dấu tiếng Việt
    private static final java.util.regex.Pattern DIACRITICS_PATTERN =
            java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static String removeDiacritics(String input) {
        if (input == null) return "";
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = java.text.Normalizer.normalize(replaced, java.text.Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    public void addProduct(Product product,
                           OnSuccessCallback<String> onSuccess,
                           OnErrorCallback onError) {
        DatabaseReference ref = db.child("products").push();
        String newId = ref.getKey() != null ? ref.getKey() : "";
        product.setProductId(newId);
        product.setCreatedAt(System.currentTimeMillis());
        ref.setValue(product)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(newId))
                .addOnFailureListener(e -> onError.onError(
                        e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
    }

    public void updateProduct(Product product,
                              OnSuccessCallback<Void> onSuccess,
                              OnErrorCallback onError) {
        db.child("products").child(product.getProductId())
                .setValue(product)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(null))
                .addOnFailureListener(e -> onError.onError(
                        e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
    }

    public void deactivateProduct(String productId,
                                  OnSuccessCallback<Void> onSuccess,
                                  OnErrorCallback onError) {
        db.child("products").child(productId).child("isActive")
                .setValue(false)
                .addOnSuccessListener(unused -> onSuccess.onSuccess(null))
                .addOnFailureListener(e -> onError.onError(
                        e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
    }
}