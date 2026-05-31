package com.example.khoga.adapter;

import com.example.khoga.model.CartItem;
import com.example.khoga.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;
    private DatabaseReference cartRef;
    private OnCartChangeListener listener;

    // Lưu itemId của các item đang được chọn (tick)
    private Set<String> selectedIds = new HashSet<>();

    public interface OnCartChangeListener {
        void onSelectionChanged();
    }

    public CartAdapter(List<CartItem> cartList, DatabaseReference cartRef,
                       OnCartChangeListener listener) {
        this.cartList = cartList;
        this.cartRef  = cartRef;
        this.listener = listener;
    }

    // ── Quản lý trạng thái chọn ─────────────────────────────────────────

    /** Kiểm tra item có đang được chọn không */
    public boolean isSelected(String itemId) {
        return selectedIds.contains(itemId);
    }

    /** Lấy danh sách itemId đang được chọn */
    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    /** Lấy danh sách CartItem đang được chọn */
    public List<CartItem> getSelectedItems() {
        java.util.ArrayList<CartItem> selected = new java.util.ArrayList<>();
        for (CartItem item : cartList) {
            if (selectedIds.contains(item.getItemId())) {
                selected.add(item);
            }
        }
        return selected;
    }

    /** Chọn tất cả */
    public void selectAll() {
        selectedIds.clear();
        for (CartItem item : cartList) {
            selectedIds.add(item.getItemId());
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged();
    }

    /** Bỏ chọn tất cả */
    public void deselectAll() {
        selectedIds.clear();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged();
    }

    /** Kiểm tra có đang chọn tất cả không */
    public boolean isAllSelected() {
        return !cartList.isEmpty() && selectedIds.size() == cartList.size();
    }

    /** Gọi sau khi cartList thay đổi — tự chọn item mới, bỏ item đã xóa */
    public void syncSelectionWithList() {
        // Xóa các id không còn trong list
        Set<String> validIds = new HashSet<>();
        for (CartItem item : cartList) {
            validIds.add(item.getItemId());
        }
        selectedIds.retainAll(validIds);

        // Nếu có item mới chưa trong selectedIds → tự chọn luôn (default checked)
        for (CartItem item : cartList) {
            if (!selectedIds.contains(item.getItemId())) {
                selectedIds.add(item.getItemId());
            }
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.tvProductName.setText(item.getProductName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvProductPrice.setText(formatter.format(item.getPrice()) + "đ");

        // Load ảnh
        if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getProductImage())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.imgProduct);
        }

        // Checkbox — set trạng thái + lắng nghe thay đổi
        holder.cbSelect.setOnCheckedChangeListener(null); // tránh trigger khi recycle
        holder.cbSelect.setChecked(selectedIds.contains(item.getItemId()));
        holder.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                selectedIds.add(item.getItemId());
            } else {
                selectedIds.remove(item.getItemId());
            }
            if (listener != null) listener.onSelectionChanged();
        });

        // Disable nút minus khi qty = 1
        holder.btnMinus.setAlpha(item.getQuantity() <= 1 ? 0.3f : 1.0f);

        // ── Nút TĂNG ────────────────────────────────────────────────────
        holder.btnPlus.setOnClickListener(v -> {
            int newQty = item.getQuantity() + 1;
            FirebaseDatabase.getInstance().getReference("products")
                    .child(item.getProductId())
                    .child("stock")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer stock = snapshot.getValue(Integer.class);
                            if (stock == null || newQty > stock) {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Không đủ hàng trong kho (còn "
                                                + (stock != null ? stock : 0) + ")",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                cartRef.child(item.getItemId()).child("quantity")
                                        .setValue(newQty);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        });

        // ── Nút GIẢM ────────────────────────────────────────────────────
        holder.btnMinus.setOnClickListener(v -> {
            int currentQty = item.getQuantity();
            if (currentQty <= 1) {
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Xóa sản phẩm")
                        .setMessage("Bạn có muốn xóa \"" + item.getProductName()
                                + "\" khỏi giỏ hàng?")
                        .setPositiveButton("Xóa", (dialog, which) ->
                                cartRef.child(item.getItemId()).removeValue())
                        .setNegativeButton("Hủy", null)
                        .show();
            } else {
                cartRef.child(item.getItemId()).child("quantity")
                        .setValue(currentQty - 1);
            }
        });

        // ── Nút XÓA ─────────────────────────────────────────────────────
        holder.btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Xóa sản phẩm")
                        .setMessage("Bạn có chắc muốn xóa \"" + item.getProductName()
                                + "\" khỏi giỏ hàng?")
                        .setPositiveButton("Xóa", (dialog, which) ->
                                cartRef.child(item.getItemId()).removeValue())
                        .setNegativeButton("Hủy", null)
                        .show()
        );
    }

    @Override
    public int getItemCount() {
        return cartList == null ? 0 : cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        CheckBox   cbSelect;
        ImageView  imgProduct;
        TextView   tvProductName, tvProductPrice, tvQuantity;
        ImageButton btnMinus, btnPlus, btnDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect      = itemView.findViewById(R.id.cbSelect);
            imgProduct    = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice= itemView.findViewById(R.id.tvProductPrice);
            tvQuantity    = itemView.findViewById(R.id.tvQuantity);
            btnMinus      = itemView.findViewById(R.id.btnMinus);
            btnPlus       = itemView.findViewById(R.id.btnPlus);
            btnDelete     = itemView.findViewById(R.id.btnDelete);
        }
    }
}