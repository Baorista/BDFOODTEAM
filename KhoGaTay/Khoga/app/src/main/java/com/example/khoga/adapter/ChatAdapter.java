package com.example.khoga.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.example.khoga.R;
import com.example.khoga.model.ChatMessage;
import com.example.khoga.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChatAdapter — 3 ViewTypes theo Figma:
 *
 * TYPE_USER (0)      — Bubble bên phải, nền tím
 * TYPE_ASSISTANT (1) — Bubble bên trái, nền xám nhạt
 * TYPE_PRODUCT (2)   — Card sản phẩm gợi ý (ảnh + tên + giá + icon giỏ hàng)
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;
    private static final int TYPE_PRODUCT = 2;

    // Mỗi item trong list có thể là ChatMessage hoặc ProductSuggestion
    private final List<Object> items = new ArrayList<>();
    private final OnProductClickListener productClickListener;
    private final OnAddToCartListener addToCartListener;

    // ===== Interfaces =====

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnAddToCartListener {
        void onAddToCart(Product product);
    }

    public ChatAdapter(OnProductClickListener productClick,
                       OnAddToCartListener addToCart) {
        this.productClickListener = productClick;
        this.addToCartListener = addToCart;
    }

    // ====================================================================
    // PUBLIC METHODS - Thêm tin nhắn và sản phẩm
    // ====================================================================

    /**
     * Thêm tin nhắn user
     */
    public void addUserMessage(String text) {
        ChatMessage msg = new ChatMessage("user", text);
        items.add(msg);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Thêm tin nhắn assistant
     */
    public void addAssistantMessage(String text) {
        ChatMessage msg = new ChatMessage("assistant", text);
        items.add(msg);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Thêm card sản phẩm gợi ý (sau tin nhắn assistant)
     */
    public void addProductSuggestions(List<Product> products) {
        if (products == null || products.isEmpty()) return;
        for (Product p : products) {
            items.add(new ProductSuggestion(p));
            notifyItemInserted(items.size() - 1);
        }
    }

    /**
     * Load lịch sử chat từ Firebase
     * Trả về danh sách tin nhắn assistant có chứa suggestedProductIds
     * để caller có thể resolve product data
     */
    public List<ChatMessage> setHistory(List<ChatMessage> history) {
        items.clear();
        List<ChatMessage> messagesWithSuggestions = new ArrayList<>();
        if (history != null) {
            for (ChatMessage msg : history) {
                items.add(msg);
                // Thu thập các tin nhắn có product suggestions để resolve sau
                if (msg.isAssistant() && msg.hasSuggestions()) {
                    messagesWithSuggestions.add(msg);
                }
            }
        }
        notifyDataSetChanged();
        return messagesWithSuggestions;
    }

    /**
     * Chèn product cards ngay sau một tin nhắn assistant cụ thể
     */
    public void insertProductsAfterMessage(ChatMessage afterMsg, List<Product> products) {
        if (products == null || products.isEmpty()) return;
        int msgIndex = items.indexOf(afterMsg);
        if (msgIndex < 0) return;

        int insertAt = msgIndex + 1;
        for (Product p : products) {
            items.add(insertAt, new ProductSuggestion(p));
            insertAt++;
        }
        notifyDataSetChanged();
    }

    public int getLastPosition() {
        return items.size() - 1;
    }

    // ====================================================================
    // VIEW TYPE LOGIC
    // ====================================================================

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof ProductSuggestion) {
            return TYPE_PRODUCT;
        }
        if (item instanceof ChatMessage) {
            return ((ChatMessage) item).isUser() ? TYPE_USER : TYPE_ASSISTANT;
        }
        return TYPE_ASSISTANT;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ====================================================================
    // CREATE VIEW HOLDERS
    // ====================================================================

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER:
                return new UserMessageVH(
                        inflater.inflate(R.layout.item_chat_user, parent, false));
            case TYPE_PRODUCT:
                return new ProductCardVH(
                        inflater.inflate(R.layout.item_chat_product, parent, false));
            default:
                return new AssistantMessageVH(
                        inflater.inflate(R.layout.item_chat_assistant, parent, false));
        }
    }

    // ====================================================================
    // BIND VIEW HOLDERS
    // ====================================================================

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof UserMessageVH && item instanceof ChatMessage) {
            ((UserMessageVH) holder).bind((ChatMessage) item);
        } else if (holder instanceof AssistantMessageVH && item instanceof ChatMessage) {
            ((AssistantMessageVH) holder).bind((ChatMessage) item);
        } else if (holder instanceof ProductCardVH && item instanceof ProductSuggestion) {
            ((ProductCardVH) holder).bind(((ProductSuggestion) item).product);
        }
    }

    // ====================================================================
    // VIEW HOLDER: User message (bubble bên phải, nền tím)
    // ====================================================================

    static class UserMessageVH extends RecyclerView.ViewHolder {
        TextView tvMessage;

        UserMessageVH(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }

        void bind(ChatMessage msg) {
            tvMessage.setText(msg.getContent());
        }
    }

    // ====================================================================
    // VIEW HOLDER: Assistant message (bubble bên trái, nền xám nhạt)
    // ====================================================================

    static class AssistantMessageVH extends RecyclerView.ViewHolder {
        TextView tvMessage;

        AssistantMessageVH(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvAssistantMessage);
        }

        void bind(ChatMessage msg) {
            tvMessage.setText(msg.getContent());
        }
    }

    // ====================================================================
    // VIEW HOLDER: Product suggestion card (ảnh + tên + giá + icon giỏ hàng)
    // ====================================================================

    class ProductCardVH extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvPrice;
        ImageButton btnCart;
        MaterialCardView card;

        ProductCardVH(View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgChatProduct);
            tvName = itemView.findViewById(R.id.tvChatProductName);
            tvPrice = itemView.findViewById(R.id.tvChatProductPrice);
            btnCart = itemView.findViewById(R.id.btnChatAddToCart);
            card = itemView.findViewById(R.id.cardChatProduct);
        }

        void bind(Product product) {
            // === RESET views trước khi bind (tránh lỗi view recycling) ===
            imgProduct.setImageDrawable(null);
            tvName.setText("");
            tvPrice.setText("");
            card.setOnClickListener(null);
            btnCart.setOnClickListener(null);

            if (product == null) {
                card.setVisibility(View.GONE);
                return;
            }
            card.setVisibility(View.VISIBLE);

            tvName.setText(product.getName());
            tvPrice.setText(String.format(java.util.Locale.US, "%,.0fđ", product.getPrice()));

            // Load ảnh sản phẩm
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                Glide.with(imgProduct.getContext())
                        .load(product.getImages().get(0))
                        .centerCrop()
                        .placeholder(R.drawable.bg_rounded_8dp)
                        .into(imgProduct);
            } else {
                imgProduct.setImageResource(R.drawable.bg_rounded_8dp);
            }

            // Click card → mở Product Detail
            card.setOnClickListener(v -> {
                if (productClickListener != null) {
                    productClickListener.onProductClick(product);
                }
            });

            // Click icon giỏ hàng → thêm vào cart
            btnCart.setOnClickListener(v -> {
                if (addToCartListener != null) {
                    addToCartListener.onAddToCart(product);
                }
            });
        }
    }

    // ====================================================================
    // WRAPPER: Phân biệt Product với ChatMessage trong items list
    // ====================================================================

    private static class ProductSuggestion {
        final Product product;
        ProductSuggestion(Product product) {
            this.product = product;
        }
    }
}
