package com.example.khoga.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import android.graphics.Paint;

import com.bumptech.glide.Glide;
import com.example.khoga.R;
import com.example.khoga.model.Product;
import com.example.khoga.model.Review;
import com.example.khoga.repository.BrowsingHistoryRepository;
import com.example.khoga.ui.activity.CheckoutActivity;
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

public class ProductDetailFragment extends Fragment {

    private ProductViewModel viewModel;

    private ViewPager2   vpImages;
    private TextView     tvName, tvPrice, tvSalePrice, tvSold, tvStock, tvRating, tvDesc;
    private TextView     tvReviewSummary, tvNoReviews;
    private RecyclerView rvReviews;
    private Button       btnBuy, btnAddCart;
    private ImageButton  btnBack, btnWishlist;

    private ReviewAdapter reviewAdapter;
    private final BrowsingHistoryRepository browsingHistoryRepo = new BrowsingHistoryRepository();
    private boolean isInWishlist = false;

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupReviewsList();
        viewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        viewModel.getSelected().observe(getViewLifecycleOwner(), product -> {
            if (product != null) {
                bindProduct(product);
                loadReviews(product.getProductId());
                checkWishlistStatus(product.getProductId());

                // Ghi lại lượt xem sản phẩm cho AI chatbot context
                String uid = getCurrentUserId();
                if (uid != null) {
                    browsingHistoryRepo.recordView(uid,
                            product.getProductId(),
                            product.getCategoryId(),
                            product.getName());
                }
            }
        });

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        // ═══════════════════════════════════════════
        // NÚT ❤ YÊU THÍCH
        // ═══════════════════════════════════════════
        btnWishlist.setOnClickListener(v -> {
            Product product = viewModel.getSelected().getValue();
            if (product == null) return;

            String userId = getCurrentUserId();
            if (userId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("wishlists").child(userId).child(product.getProductId());

            if (isInWishlist) {
                // Đang yêu thích → xoá
                ref.removeValue();
                isInWishlist = false;
                updateWishlistIcon();
                Toast.makeText(getContext(), "Đã xoá khỏi yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                // Chưa yêu thích → thêm
                ref.setValue(System.currentTimeMillis());
                isInWishlist = true;
                updateWishlistIcon();
                Toast.makeText(getContext(), "Đã thêm vào yêu thích ❤", Toast.LENGTH_SHORT).show();
            }
        });

        // ═══════════════════════════════════════════
        // NÚT "MUA NGAY"
        // ═══════════════════════════════════════════
        btnBuy.setOnClickListener(v -> {
            Product product = viewModel.getSelected().getValue();
            if (product == null) return;

            String userId = getCurrentUserId();
            if (userId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            if (product.getStock() <= 0) {
                Toast.makeText(getContext(), "Sản phẩm đã hết hàng!", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference cartRef = FirebaseDatabase.getInstance()
                    .getReference("carts").child(userId);

            cartRef.orderByChild("productId").equalTo(product.getProductId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            double price = getProductPrice(product);

                            if (snapshot.exists()) {
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    Integer qty = child.child("quantity").getValue(Integer.class);
                                    child.getRef().child("quantity").setValue((qty != null ? qty : 0) + 1);
                                }
                            } else {
                                String key = cartRef.push().getKey();
                                if (key == null) return;
                                cartRef.child(key).setValue(buildCartItem(product, price));
                            }

                            if (isAdded()) {
                                Intent intent = new Intent(requireContext(), CheckoutActivity.class);
                                intent.putExtra("TOTAL_PRICE", price);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // ═══════════════════════════════════════════
        // NÚT "THÊM VÀO GIỎ HÀNG"
        // ═══════════════════════════════════════════
        btnAddCart.setOnClickListener(v -> {
            Product product = viewModel.getSelected().getValue();
            if (product == null) return;

            String userId = getCurrentUserId();
            if (userId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            if (product.getStock() <= 0) {
                Toast.makeText(getContext(), "Sản phẩm đã hết hàng!", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference cartRef = FirebaseDatabase.getInstance()
                    .getReference("carts").child(userId);

            // Đọc stock mới nhất trước khi thêm
            FirebaseDatabase.getInstance().getReference("products")
                    .child(product.getProductId()).child("stock")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot stockSnap) {
                            Integer stock = stockSnap.getValue(Integer.class);
                            if (stock == null || stock <= 0) {
                                Toast.makeText(getContext(), "Sản phẩm đã hết hàng!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            int currentStock = stock;

                            cartRef.orderByChild("productId").equalTo(product.getProductId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                for (DataSnapshot child : snapshot.getChildren()) {
                                                    Integer qty = child.child("quantity").getValue(Integer.class);
                                                    int newQty = (qty != null ? qty : 0) + 1;
                                                    if (newQty > currentStock) {
                                                        Toast.makeText(getContext(),
                                                                "Không đủ hàng (còn " + currentStock + ")",
                                                                Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        child.getRef().child("quantity").setValue(newQty);
                                                        Toast.makeText(getContext(), "Đã tăng số lượng trong giỏ hàng", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            } else {
                                                String key = cartRef.push().getKey();
                                                if (key == null) return;
                                                double price = getProductPrice(product);
                                                cartRef.child(key).setValue(buildCartItem(product, price));
                                                Toast.makeText(getContext(), "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        });
    }

    // ── Kiểm tra sản phẩm đã trong wishlist chưa ──
    private void checkWishlistStatus(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            isInWishlist = false;
            updateWishlistIcon();
            return;
        }

        FirebaseDatabase.getInstance().getReference("wishlists")
                .child(userId).child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInWishlist = snapshot.exists();
                        if (isAdded()) updateWishlistIcon();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isInWishlist = false;
                        if (isAdded()) updateWishlistIcon();
                    }
                });
    }

    // ── Cập nhật icon ❤ ──
    private void updateWishlistIcon() {
        if (btnWishlist == null) return;
        if (isInWishlist) {
            // Đỏ = đã yêu thích
            btnWishlist.setColorFilter(android.graphics.Color.parseColor("#E53935"));
        } else {
            // Xám = chưa yêu thích
            btnWishlist.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"));
        }
    }

    private double getProductPrice(Product product) {
        return (product.getSalePrice() > 0 && product.getSalePrice() < product.getPrice())
                ? product.getSalePrice() : product.getPrice();
    }

    private Map<String, Object> buildCartItem(Product product, double price) {
        String imageUrl = (product.getImages() != null && !product.getImages().isEmpty())
                ? product.getImages().get(0) : "";

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", product.getProductId());
        cartItem.put("productName", product.getName());
        cartItem.put("productImage", imageUrl);
        cartItem.put("price", price);
        cartItem.put("quantity", 1);
        cartItem.put("addedAt", System.currentTimeMillis());
        return cartItem;
    }

    private void bindViews(View v) {
        vpImages        = v.findViewById(R.id.vpProductImages);
        tvName          = v.findViewById(R.id.tvDetailName);
        tvPrice         = v.findViewById(R.id.tvDetailPrice);
        tvSalePrice     = v.findViewById(R.id.tvDetailSalePrice);
        tvSold          = v.findViewById(R.id.tvDetailSold);
        tvStock         = v.findViewById(R.id.tvDetailStock);
        tvRating        = v.findViewById(R.id.tvDetailRating);
        tvDesc          = v.findViewById(R.id.tvDetailDescription);
        btnBuy          = v.findViewById(R.id.btnBuy);
        btnAddCart       = v.findViewById(R.id.btnAddCart);
        btnBack         = v.findViewById(R.id.btnBack);
        btnWishlist     = v.findViewById(R.id.btnWishlist);
        tvReviewSummary = v.findViewById(R.id.tvReviewSummary);
        tvNoReviews     = v.findViewById(R.id.tvNoReviews);
        rvReviews       = v.findViewById(R.id.rvReviews);
    }

    private void setupReviewsList() {
        reviewAdapter = new ReviewAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                // Disable RecyclerView's own scrolling — let NestedScrollView handle it
                return false;
            }
        };
        rvReviews.setLayoutManager(layoutManager);
        rvReviews.setNestedScrollingEnabled(false);
        rvReviews.setHasFixedSize(false);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void bindProduct(Product product) {
        tvName.setText(product.getName());
        tvDesc.setText(product.getDescription());
        tvSold.setText("Đã bán: " + product.getTotalSold());
        tvStock.setText("Còn lại: " + product.getStock());

        if (product.getSalePrice() > 0 && product.getSalePrice() < product.getPrice()) {
            tvPrice.setVisibility(View.VISIBLE);
            tvSalePrice.setVisibility(View.VISIBLE);
            tvPrice.setText(Product.formatPrice(product.getPrice()));
            tvPrice.setPaintFlags(tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvSalePrice.setText(Product.formatPrice(product.getSalePrice()));
        } else {
            tvPrice.setVisibility(View.GONE);
            tvPrice.setPaintFlags(tvPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tvSalePrice.setVisibility(View.VISIBLE);
            tvSalePrice.setText(Product.formatPrice(product.getPrice()));
        }

        if (product.getAvgRating() > 0) {
            tvRating.setVisibility(View.VISIBLE);
            tvRating.setText(String.format("⭐ %.1f  (%d đánh giá)",
                    product.getAvgRating(), product.getTotalReviews()));
        } else {
            tvRating.setVisibility(View.GONE);
        }

        // Disable nút khi hết hàng
        if (product.getStock() <= 0) {
            btnAddCart.setEnabled(false);
            btnAddCart.setText("Hết hàng");
            btnBuy.setEnabled(false);
            btnBuy.setText("Hết hàng");
        } else {
            btnAddCart.setEnabled(true);
            btnAddCart.setText("Thêm vào giỏ");
            btnBuy.setEnabled(true);
            btnBuy.setText("Mua ngay");
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            vpImages.setAdapter(new RecyclerView.Adapter<ImageViewHolder>() {
                @NonNull
                @Override
                public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
                    ImageView img = new ImageView(parent.getContext());
                    img.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    return new ImageViewHolder(img);
                }
                @Override
                public void onBindViewHolder(@NonNull ImageViewHolder holder, int pos) {
                    Glide.with(holder.img.getContext())
                            .load(product.getImages().get(pos))
                            .placeholder(new android.graphics.drawable.ColorDrawable(
                                    android.graphics.Color.parseColor("#E0E0E0")))
                            .into(holder.img);
                }
                @Override
                public int getItemCount() { return product.getImages().size(); }
            });
        }
    }

    private void loadReviews(String productId) {
        if (productId == null || productId.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference()
                .child("reviews").child(productId)
                .orderByChild("createdAt")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Review> reviews = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r != null) reviews.add(r);
                        }

                        // Đảo ngược: hiển thị đánh giá mới nhất lên đầu
                        java.util.Collections.reverse(reviews);

                        if (reviews.isEmpty()) {
                            tvNoReviews.setVisibility(View.VISIBLE);
                            rvReviews.setVisibility(View.GONE);
                            tvReviewSummary.setText("");
                        } else {
                            tvNoReviews.setVisibility(View.GONE);
                            rvReviews.setVisibility(View.VISIBLE);
                            tvReviewSummary.setText(reviews.size() + " đánh giá");
                            reviewAdapter.setReviews(reviews);

                            // Force RecyclerView re-measure inside NestedScrollView
                            rvReviews.post(() -> {
                                rvReviews.requestLayout();
                                rvReviews.invalidate();
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvNoReviews.setVisibility(View.VISIBLE);
                        tvNoReviews.setText("Không thể tải đánh giá.");
                        rvReviews.setVisibility(View.GONE);
                    }
                });
    }

    // ══════════════════════════════════════════
    // REVIEW ADAPTER
    // ══════════════════════════════════════════
    static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewVH> {
        private List<Review> reviews = new ArrayList<>();

        void setReviews(List<Review> list) {
            this.reviews = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ReviewVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.widget.LinearLayout layout = new android.widget.LinearLayout(parent.getContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int pad = dpToPx(parent.getContext(), 12);
            layout.setPadding(0, pad, 0, pad);
            return new ReviewVH(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewVH holder, int pos) {
            holder.bind(reviews.get(pos));
        }

        @Override
        public int getItemCount() { return reviews.size(); }

        static class ReviewVH extends RecyclerView.ViewHolder {
            private final android.widget.LinearLayout container;
            ReviewVH(View itemView) { super(itemView); container = (android.widget.LinearLayout) itemView; }

            void bind(Review review) {
                container.removeAllViews();
                android.content.Context ctx = container.getContext();

                TextView tvHeader = new TextView(ctx);
                tvHeader.setText(review.getUserName() + "  " + review.getStarsDisplay());
                tvHeader.setTextSize(14);
                tvHeader.setTextColor(android.graphics.Color.parseColor("#333333"));
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                container.addView(tvHeader);

                TextView tvDate = new TextView(ctx);
                tvDate.setText(review.getFormattedDate());
                tvDate.setTextSize(12);
                tvDate.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                tvDate.setPadding(0, dpToPx(ctx, 2), 0, 0);
                container.addView(tvDate);

                if (review.getComment() != null && !review.getComment().isEmpty()) {
                    TextView tvComment = new TextView(ctx);
                    tvComment.setText(review.getComment());
                    tvComment.setTextSize(13);
                    tvComment.setTextColor(android.graphics.Color.parseColor("#424242"));
                    tvComment.setPadding(0, dpToPx(ctx, 4), 0, 0);
                    container.addView(tvComment);
                }

                // Hiển thị ảnh đánh giá (nếu có)
                if (review.getImages() != null && !review.getImages().isEmpty()) {
                    android.widget.LinearLayout imgRow = new android.widget.LinearLayout(ctx);
                    imgRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    imgRow.setPadding(0, dpToPx(ctx, 6), 0, 0);

                    for (String imageUrl : review.getImages()) {
                        ImageView img = new ImageView(ctx);
                        int size = dpToPx(ctx, 64);
                        android.widget.LinearLayout.LayoutParams imgLp =
                                new android.widget.LinearLayout.LayoutParams(size, size);
                        imgLp.setMarginEnd(dpToPx(ctx, 6));
                        img.setLayoutParams(imgLp);
                        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        img.setClipToOutline(true);
                        Glide.with(ctx).load(imageUrl).centerCrop().into(img);
                        imgRow.addView(img);
                    }
                    container.addView(imgRow);
                }

                // Hiển thị video (nếu có)
                if (review.getVideoUrl() != null && !review.getVideoUrl().isEmpty()) {
                    TextView tvVideo = new TextView(ctx);
                    tvVideo.setText("\u25B6 Xem video đánh giá");
                    tvVideo.setTextSize(13);
                    tvVideo.setTextColor(android.graphics.Color.parseColor("#5A4B75"));
                    tvVideo.setPadding(0, dpToPx(ctx, 4), 0, 0);
                    tvVideo.setOnClickListener(v -> {
                        android.content.Intent videoIntent = new android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(review.getVideoUrl()));
                        ctx.startActivity(videoIntent);
                    });
                    container.addView(tvVideo);
                }

                View divider = new View(ctx);
                divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"));
                android.widget.LinearLayout.LayoutParams lp =
                        (android.widget.LinearLayout.LayoutParams) divider.getLayoutParams();
                lp.topMargin = dpToPx(ctx, 12);
                container.addView(divider);
            }
        }

        private static int dpToPx(android.content.Context ctx, int dp) {
            return (int) (dp * ctx.getResources().getDisplayMetrics().density);
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        ImageViewHolder(ImageView v) { super(v); img = v; }
    }
}