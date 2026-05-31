package com.example.khoga.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khoga.R;
import com.example.khoga.model.Product;

import java.util.ArrayList;
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private List<Product> products = new ArrayList<>();
    private OnWishlistActionListener listener;

    public interface OnWishlistActionListener {
        void onRemoveWishlist(Product product);
        void onProductClick(Product product);
        void onAddToCart(Product product);
    }

    public WishlistAdapter(OnWishlistActionListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> list) {
        this.products = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        Product product = products.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductPrice.setText(Product.formatPrice(product.getDisplayPrice()));

        // Load ảnh
        String imageUrl = product.getFirstImage();
        if (!imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(new ColorDrawable(Color.parseColor("#E0E0E0")))
                    .centerCrop()
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageDrawable(new ColorDrawable(Color.parseColor("#E0E0E0")));
        }

        // Nút heart → xoá khỏi wishlist
        holder.btnHeart.setOnClickListener(v -> listener.onRemoveWishlist(product));

        // Click vào sản phẩm
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));

        // Nút giỏ hàng
        holder.btnAddToCart.setOnClickListener(v -> listener.onAddToCart(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct, btnHeart, btnAddToCart;
        TextView tvProductName, tvProductPrice;

        WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnHeart = itemView.findViewById(R.id.btnHeart);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }
    }
}