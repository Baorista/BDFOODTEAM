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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onClick(Product product);
    }

    private List<Product> products = new ArrayList<>();
    private OnProductClickListener listener;

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> list) {
        this.products = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(Product.formatPrice(product.getDisplayPrice()));

        // Rating
        if (product.getAvgRating() > 0) {
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText(String.format("⭐ %.1f  •  Đã bán %d",
                    product.getAvgRating(), product.getTotalSold()));
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }

        // Load ảnh bằng Glide
        if (!product.getFirstImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getFirstImage())
                    .placeholder(new ColorDrawable(Color.parseColor("#E0E0E0")))
                    .centerCrop()
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageDrawable(new ColorDrawable(Color.parseColor("#E0E0E0")));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(product));
    }

    @Override
    public int getItemCount() { return products.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView  tvName, tvPrice, tvRating;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName     = itemView.findViewById(R.id.tvProductName);
            tvPrice    = itemView.findViewById(R.id.tvProductPrice);
            tvRating   = itemView.findViewById(R.id.tvProductRating);
        }
    }
}