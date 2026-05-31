package com.example.khoga.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.khoga.R;
import com.example.khoga.adapter.OrderAdapter;
import com.example.khoga.model.Order;
import com.example.khoga.repository.ReviewRepository;
import com.example.khoga.ui.activity.CancelOrderDialog;
import com.example.khoga.ui.activity.OrderDetailActivity;
import com.example.khoga.viewmodel.OrderViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderListFragment — Màn hình "Lịch sử mua hàng"
 *
 * 4 tabs theo Figma:
 *   Tab 0: "Chờ xác nhận"    → pending + confirmed
 *   Tab 1: "Đang giao hàng"  → shipping
 *   Tab 2: "Đã giao"         → delivered
 *   Tab 3: "Đã hủy"          → cancelled
 */
public class OrderListFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private View layoutEmpty;
    private ImageView imgEmpty;
    private TextView tvEmpty;

    private OrderViewModel orderViewModel;
    private OrderAdapter orderAdapter;

    private List<Order> allOrders = new ArrayList<>();
    private int currentTab = 0;
    private final ReviewRepository reviewRepository = new ReviewRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupTabs();
        setupRecyclerView();
        setupViewModel();
    }

    // ================================================================
    // INIT VIEWS
    // ================================================================

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayoutOrders);
        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        imgEmpty = view.findViewById(R.id.imgEmpty);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        // Nút quay lại
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }
    }

    // ================================================================
    // TABS: 4 tab theo Figma
    // ================================================================

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Chờ xác nhận"));
        tabLayout.addTab(tabLayout.newTab().setText("Đang giao hàng"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã giao"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã hủy"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                filterOrders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ================================================================
    // RECYCLERVIEW + ADAPTER
    // ================================================================

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onDetailClick(Order order) {
                // Mở OrderDetailActivity
                Intent intent = new Intent(getContext(), OrderDetailActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }

            @Override
            public void onCancelClick(Order order) {
                // Mở dialog hủy đơn
                CancelOrderDialog dialog = CancelOrderDialog.newInstance(order.getOrderId());
                dialog.setOnCancelConfirmListener((orderId, reason) -> {
                    orderViewModel.cancelOrder(orderId, reason);
                });
                dialog.show(getChildFragmentManager(), "cancel_order");
            }

            @Override
            public void onReviewClick(Order order) {
                // Mở ReviewActivity cho tất cả sản phẩm trong đơn
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    ArrayList<String> productIds = new ArrayList<>();
                    ArrayList<String> productNames = new ArrayList<>();
                    ArrayList<String> productImages = new ArrayList<>();

                    for (Order.OrderItem item : order.getItems()) {
                        productIds.add(item.getProductId());
                        productNames.add(item.getProductName());
                        productImages.add(item.getProductImage());
                    }

                    Intent intent = new Intent(getContext(),
                            com.example.khoga.ui.activity.ReviewOrderActivity.class);
                    intent.putStringArrayListExtra("productIds", productIds);
                    intent.putStringArrayListExtra("productNames", productNames);
                    intent.putStringArrayListExtra("productImages", productImages);
                    intent.putExtra("orderId", order.getOrderId());
                    startActivity(intent);
                }
            }
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(orderAdapter);
    }

    // ================================================================
    // VIEWMODEL + OBSERVE
    // ================================================================

    private void setupViewModel() {
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Observe danh sách đơn hàng
        orderViewModel.getUserOrders().observe(getViewLifecycleOwner(), orders -> {
            allOrders = orders != null ? orders : new ArrayList<>();
            // Đồng bộ flag reviewed: nếu review bị xóa khỏi DB thì tự reset flag
            for (Order order : allOrders) {
                if ("delivered".equals(order.getOrderStatus()) && "paid".equals(order.getPaymentStatus())
                        && order.getItems() != null && !order.getItems().isEmpty()) {

                    for (Order.OrderItem item : order.getItems()) {
                        reviewRepository.syncReviewedFlag(
                                order.getOrderId(),
                                item.getProductId());
                    }
                }
            }
            filterOrders();
        });

        // Observe hủy đơn thành công
        orderViewModel.getCancelSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                android.widget.Toast.makeText(getContext(),
                        "Đã hủy đơn hàng", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Observe lỗi
        orderViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                android.widget.Toast.makeText(getContext(),
                        error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Load data
        orderViewModel.loadUserOrders(user.getUid());
    }

    // ================================================================
    // LỌC ĐƠN HÀNG THEO TAB
    // ================================================================

    private void filterOrders() {
        List<Order> filtered = new ArrayList<>();

        for (Order order : allOrders) {
            String status = order.getOrderStatus();
            if (status == null) continue;

            switch (currentTab) {
                case 0: // Chờ xác nhận → pending + confirmed
                    if ("pending".equals(status) || "confirmed".equals(status)) {
                        filtered.add(order);
                    }
                    break;
                case 1: // Đang giao hàng → shipping
                    if ("shipping".equals(status)) {
                        filtered.add(order);
                    }
                    break;
                case 2: // Đã giao → delivered
                    if ("delivered".equals(status)) {
                        filtered.add(order);
                    }
                    break;
                case 3: // Đã hủy → cancelled
                    if ("cancelled".equals(status)) {
                        filtered.add(order);
                    }
                    break;
            }
        }

        orderAdapter.updateData(filtered);

        // Empty state
        if (filtered.isEmpty()) {
            rvOrders.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Bạn chưa có đơn hàng nào cả");
        } else {
            rvOrders.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }
}
