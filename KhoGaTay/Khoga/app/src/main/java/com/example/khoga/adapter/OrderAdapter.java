package com.example.khoga.adapter;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.example.khoga.R;
import com.example.khoga.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OrderAdapter — Hiển thị danh sách đơn hàng theo Figma
 *
 * Mỗi card đơn hàng hiển thị:
 * - Sản phẩm đầu tiên (ảnh + tên + giá + số lượng)
 * - Tổng tiền + số sản phẩm
 * - Buttons tùy theo trạng thái:
 *     pending/confirmed → "Chi tiết" + "Hủy đơn"
 *     shipping          → "Chi tiết"
 *     delivered         → "Chi tiết" + "Đánh giá"
 *     cancelled         → "Chi tiết" + badge "Đã hủy"
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<Order> orders = new ArrayList<>();
    private final OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onDetailClick(Order order);
        void onCancelClick(Order order);
        void onReviewClick(Order order);
    }

    public OrderAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Order> newOrders) {
        this.orders = newOrders != null ? newOrders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    // ====================================================================
    // VIEW HOLDER
    // ====================================================================

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProduct;
        TextView tvProductName, tvPrice, tvQuantity, tvTotalSummary;
        TextView tvViewMoreItems, tvOrderTopStatus;
        LinearLayout layoutButtons, layoutAdditionalItems;
        MaterialButton btnDetail, btnCancel, btnReview;

        ViewHolder(View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgOrderProduct);
            tvProductName = itemView.findViewById(R.id.tvOrderProductName);
            tvPrice = itemView.findViewById(R.id.tvOrderProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvOrderQuantity);
            tvTotalSummary = itemView.findViewById(R.id.tvOrderTotalSummary);
            tvViewMoreItems = itemView.findViewById(R.id.tvViewMoreItems);
            layoutAdditionalItems = itemView.findViewById(R.id.layoutAdditionalItems);
            tvOrderTopStatus = itemView.findViewById(R.id.tvOrderTopStatus);
            btnDetail = itemView.findViewById(R.id.btnOrderDetail);
            btnCancel = itemView.findViewById(R.id.btnOrderCancel);
            btnReview = itemView.findViewById(R.id.btnOrderReview);
            layoutButtons = itemView.findViewById(R.id.layoutOrderButtons);
        }

        void bind(Order order) {
            // === Hiển thị sản phẩm đầu tiên ===
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Order.OrderItem firstItem = order.getItems().get(0);

                tvProductName.setText(firstItem.getProductName());
                tvPrice.setText(formatCurrency(firstItem.getPrice()));
                tvQuantity.setText("x" + firstItem.getQuantity());

                if (firstItem.getProductImage() != null) {
                    Glide.with(imgProduct.getContext())
                            .load(firstItem.getProductImage())
                            .centerCrop()
                            .placeholder(R.drawable.bg_rounded_8dp)
                            .into(imgProduct);
                }
            }

            int totalItems = order.getItems() != null ? order.getItems().size() : 0;

            // === Xử lý Xem thêm ===
            if (totalItems > 1) {
                tvViewMoreItems.setVisibility(View.VISIBLE);
                tvViewMoreItems.setText("Xem thêm");
                layoutAdditionalItems.setVisibility(View.GONE);
                layoutAdditionalItems.removeAllViews(); // Reset để không bị duplicate khi tái sử dụng ViewHolder

                tvViewMoreItems.setOnClickListener(v -> {
                    if (layoutAdditionalItems.getVisibility() == View.GONE) {
                        // Kích hoạt hiển thị
                        if (layoutAdditionalItems.getChildCount() == 0) {
                            for (int i = 1; i < totalItems; i++) {
                                Order.OrderItem item = order.getItems().get(i);
                                View extraView = LayoutInflater.from(v.getContext())
                                        .inflate(R.layout.item_order_detail_product, layoutAdditionalItems, false);

                                ImageView imgP = extraView.findViewById(R.id.imgDetailProduct);
                                TextView tvN = extraView.findViewById(R.id.tvDetailProductName);
                                TextView tvP = extraView.findViewById(R.id.tvDetailProductPrice);
                                TextView tvQ = extraView.findViewById(R.id.tvDetailQuantity);

                                tvN.setText(item.getProductName());
                                tvP.setText(formatCurrency(item.getPrice()));
                                tvQ.setText("x" + item.getQuantity());

                                if (item.getProductImage() != null) {
                                    Glide.with(v.getContext())
                                            .load(item.getProductImage())
                                            .centerCrop()
                                            .placeholder(R.drawable.bg_rounded_8dp_dark)
                                            .into(imgP);
                                }
                                layoutAdditionalItems.addView(extraView);
                            }
                        }
                        layoutAdditionalItems.setVisibility(View.VISIBLE);
                        tvViewMoreItems.setText("Thu gọn");
                    } else {
                        // Đóng lại
                        layoutAdditionalItems.setVisibility(View.GONE);
                        tvViewMoreItems.setText("Xem thêm");
                    }
                });
            } else {
                tvViewMoreItems.setVisibility(View.GONE);
                layoutAdditionalItems.setVisibility(View.GONE);
            }

            // === Tổng tiền ===
            String amountStr = formatCurrency(order.getTotalAmount());
            String fullText = String.format(Locale.getDefault(),
                    "Tổng số tiền (%d sản phẩm): %s",
                    totalItems, amountStr);

            SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);

            // Bold toàn bộ text
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                    0, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Bôi đỏ giá tiền (mã màu #E53935)
            int startAmount = fullText.lastIndexOf(amountStr);
            if (startAmount != -1) {
                ssb.setSpan(new ForegroundColorSpan(0xFFE53935),
                        startAmount, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            tvTotalSummary.setText(ssb);

            // === Buttons theo trạng thái ===
            btnDetail.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.GONE);
            btnReview.setVisibility(View.GONE);
            btnReview.setEnabled(true);
            btnReview.setAlpha(1.0f);
            tvOrderTopStatus.setVisibility(View.GONE);

            // Mặc định style cho nút (Outlined) - sẽ đổi lại nếu là tab Đã giao
            btnReview.setBackgroundTintList(null);
            btnReview.setTextColor(imgProduct.getContext().getResources().getColor(R.color.primary));

            String status = order.getOrderStatus();
            if (status == null) status = "pending";

            switch (status) {
                case "pending":
                case "confirmed":
                    // Chờ xác nhận → "Chi tiết" + "Hủy đơn"
                    btnCancel.setVisibility(View.VISIBLE);
                    break;

                case "shipping":
                    // Đang giao → chỉ "Chi tiết"
                    break;

                case "delivered":
                    // Đã giao → Ẩn "Chi tiết", Hiện "Đánh giá" (Nền tím chữ trắng)
                    btnDetail.setVisibility(View.GONE);
                    btnReview.setVisibility(View.VISIBLE);
                    
                    // Hiện dòng chữ đã giao ở góc phải
                    tvOrderTopStatus.setVisibility(View.VISIBLE);
                    tvOrderTopStatus.setText("Đã giao hàng");

                    if (order.isReviewed()) {
                        btnReview.setText("Đã đánh giá");
                        btnReview.setEnabled(false);
                        btnReview.setAlpha(0.5f);
                    } else {
                        btnReview.setText("Đánh giá");
                        btnReview.setEnabled(true);
                        btnReview.setAlpha(1.0f);
                        // Sửa thành chữ trắng nền tím (giống mầu nút Chi tiết)
                        btnReview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF5A4B75));
                        btnReview.setTextColor(android.graphics.Color.WHITE);
                        btnReview.setStrokeWidth(0); 
                    }
                    break;

                case "cancelled":
                    // Đã hủy → Hiện "Chi tiết" + Dòng chữ đỏ ở góc phải
                    tvOrderTopStatus.setVisibility(View.VISIBLE);
                    tvOrderTopStatus.setText("Đã hủy");
                    break;
            }

            // === Click listeners ===
            btnDetail.setOnClickListener(v -> {
                if (listener != null) listener.onDetailClick(order);
            });

            btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(order);
            });

            btnReview.setOnClickListener(v -> {
                if (listener != null) listener.onReviewClick(order);
            });
        }

        private String formatCurrency(double amount) {
            return String.format(Locale.US, "%,.0fđ", amount);
        }
    }
}
