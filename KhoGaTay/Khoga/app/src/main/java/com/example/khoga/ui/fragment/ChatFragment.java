package com.example.khoga.ui.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import android.widget.ImageView;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.khoga.R;
import com.example.khoga.adapter.ChatAdapter;
import com.example.khoga.model.BrowsingHistory;
import com.example.khoga.model.ChatMessage;
import com.example.khoga.model.Order;
import com.example.khoga.model.Product;
import com.example.khoga.viewmodel.ChatViewModel;
import com.example.khoga.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatFragment — Màn hình chatbot AI
 *
 * Flow:
 * 1. Load lịch sử chat + context (browsing + orders + products)
 * 2. Hiển thị tin nhắn chào mừng
 * 3. User gõ tin nhắn → optimistic UI → gọi Gemini API
 * 4. Nhận response → hiển thị + load product cards nếu có suggestions
 */
public class ChatFragment extends Fragment {

    private RecyclerView rvChat;
    private EditText edtMessage;
    private ImageButton btnSend, btnNewChat;
    private LinearLayout layoutTyping;
    private View dot1, dot2, dot3;
    private ImageView imgChatBotAvatar;

    private ChatViewModel chatViewModel;
    private ChatAdapter chatAdapter;

    private String userId;
    private boolean historyLoaded = false;

    // Context data cho AI
    private List<BrowsingHistory> browsingContext = new ArrayList<>();
    private List<Order> orderContext = new ArrayList<>();
    private List<Product> productCatalog = new ArrayList<>();

    // Typing indicator animation
    private AnimatorSet typingAnimator;

    // ================================================================
    // LIFECYCLE
    // ================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        userId = user.getUid();

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupInputActions(view);
        loadContextData();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (userId != null) {
            chatViewModel.trimOldMessages(userId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        historyLoaded = false;
        if (typingAnimator != null) {
            typingAnimator.cancel();
        }
    }

    // ================================================================
    // INIT VIEWS
    // ================================================================

    private void initViews(View view) {
        rvChat = view.findViewById(R.id.rvChat);
        edtMessage = view.findViewById(R.id.edtMessage);
        btnSend = view.findViewById(R.id.btnSend);
        layoutTyping = view.findViewById(R.id.layoutTyping);
        dot1 = view.findViewById(R.id.dot1);
        dot2 = view.findViewById(R.id.dot2);
        dot3 = view.findViewById(R.id.dot3);
        imgChatBotAvatar = view.findViewById(R.id.imgChatBotAvatar);
        btnNewChat = view.findViewById(R.id.btnNewChat);

        Glide.with(this)
                .load("https://res.cloudinary.com/djx02shiq/image/upload/v1774736126/images_gpkcwh.jpg")
                .circleCrop()
                .into(imgChatBotAvatar);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(
                // Click sản phẩm → mở ProductDetailFragment
                product -> {
                    ProductViewModel productViewModel = new ViewModelProvider(requireActivity())
                            .get(ProductViewModel.class);
                    productViewModel.selectProduct(product);
                    Bundle args = new Bundle();
                    args.putString("productId", product.getProductId());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_chat_to_detail, args);
                },
                // Click giỏ hàng → thêm vào cart
                this::addToCart
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Tin nhắn mới ở dưới
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
    }

    // ================================================================
    // VIEWMODEL + OBSERVERS
    // ================================================================

    private void setupViewModel() {
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Observe lịch sử chat
        chatViewModel.getChatHistory().observe(getViewLifecycleOwner(), messages -> {
            if (!historyLoaded) {
                historyLoaded = true;
                if (messages == null || messages.isEmpty()) {
                    // Chưa có lịch sử → hiện lời chào
                    chatAdapter.addAssistantMessage(
                            "Xin chào, tôi có thể giúp gì cho bạn?");
                } else {
                    List<ChatMessage> msgsWithSuggestions = chatAdapter.setHistory(messages);
                    // Resolve product suggestions từ lịch sử
                    for (ChatMessage msg : msgsWithSuggestions) {
                        loadProductsForHistoryMessage(msg);
                    }
                    scrollToBottom();
                }
            }
        });

        // Observe response từ AI
        chatViewModel.getAiResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                hideTypingIndicator();
                chatAdapter.addAssistantMessage(response);
                scrollToBottom();
            }
        });

        // Observe suggested product IDs từ AI response
        chatViewModel.getSuggestedProductIds().observe(getViewLifecycleOwner(), productIds -> {
            if (productIds != null && !productIds.isEmpty()) {
                loadProductsByIds(productIds);
            }
        });

        // Observe order intent (User wants to buy)
        chatViewModel.getOrderIntentLiveData().observe(getViewLifecycleOwner(), orderMap -> {
            if (orderMap != null && !orderMap.isEmpty()) {
                addBulkToCartAndCheckout(orderMap);
                chatViewModel.getOrderIntentLiveData().setValue(null); // Tránh gọi lại khi xoay màn hình
            }
        });

        // Observe loading state
        chatViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                showTypingIndicator();
            } else {
                hideTypingIndicator();
            }
            btnSend.setEnabled(loading == null || !loading);
        });

        // Load history
        chatViewModel.loadHistory(userId);
    }

    // ================================================================
    // INPUT ACTIONS: Gõ tin nhắn + Suggestion chips
    // ================================================================

    private void setupInputActions(View view) {
        // Nút tạo cuộc trò chuyện mới
        btnNewChat.setOnClickListener(v -> startNewChat());

        // Nút gửi
        btnSend.setOnClickListener(v -> sendMessage());

        // Nhấn Enter trên keyboard
        edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Suggestion chips
        setupChip(view, R.id.chipSuggest, "Gợi ý sản phẩm cho tôi");
        setupChip(view, R.id.chipHot, "Sản phẩm đang hot");
        setupChip(view, R.id.chipGift, "Tư vấn quà tặng");
        setupChip(view, R.id.chipOrder, "Theo dõi đơn hàng");
    }

    private void setupChip(View root, int chipId, String text) {
        Chip chip = root.findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(v -> {
                edtMessage.setText(text);
                sendMessage();
            });
        }
    }

    // ================================================================
    // TẠO CUỘC TRÒ CHUYỆN MỚI
    // ================================================================

    private void startNewChat() {
        chatViewModel.clearHistory(userId);
        historyLoaded = true;
        chatAdapter.setHistory(null);
        chatAdapter.addAssistantMessage("Xin chào, tôi có thể giúp gì cho bạn?");
        scrollToBottom();
    }

    // ================================================================
    // GỬI TIN NHẮN
    // ================================================================

    private void sendMessage() {
        String text = edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // 1. Optimistic UI: hiện tin nhắn user ngay
        chatAdapter.addUserMessage(text);
        scrollToBottom();

        // 2. Clear input
        edtMessage.setText("");

        // 3. Lấy lịch sử gần đây từ adapter
        List<ChatMessage> recentHistory = chatViewModel.getChatHistory().getValue();

        // 4. Gọi AI
        chatViewModel.sendMessage(
                userId, text,
                recentHistory,
                browsingContext,
                orderContext,
                productCatalog
        );
    }

    // ================================================================
    // LOAD CONTEXT DATA (chạy khi mở fragment)
    // ================================================================

    private void loadContextData() {
        // Load browsing history
        FirebaseDatabase.getInstance().getReference("browsingHistory")
                .child(userId)
                .orderByChild("viewCount")
                .limitToLast(10)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        browsingContext.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            BrowsingHistory bh = child.getValue(BrowsingHistory.class);
                            if (bh != null) browsingContext.add(bh);
                        }
                        Collections.reverse(browsingContext);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Load recent orders
        FirebaseDatabase.getInstance().getReference("orders")
                .orderByChild("userId").equalTo(userId)
                .limitToLast(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderContext.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order order = child.getValue(Order.class);
                            if (order != null) orderContext.add(order);
                        }
                        Collections.reverse(orderContext);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Load product catalog (cho AI biết sản phẩm trong shop)
        FirebaseDatabase.getInstance().getReference("products")
                .orderByChild("isActive").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        productCatalog.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) productCatalog.add(p);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ================================================================
    // LOAD PRODUCT SUGGESTIONS (khi AI trả về productIds)
    // ================================================================

    /**
     * Load products cho một tin nhắn lịch sử có suggestions
     * Chèn product cards ngay sau tin nhắn đó trong adapter
     */
    private void loadProductsForHistoryMessage(ChatMessage msg) {
        List<String> productIds = msg.getSuggestedProductIds();
        if (productIds == null || productIds.isEmpty()) return;

        List<Product> products = new ArrayList<>();
        final int[] remaining = {productIds.size()};

        for (String productId : productIds) {
            FirebaseDatabase.getInstance().getReference("products")
                    .child(productId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Product p = snapshot.getValue(Product.class);
                            if (p != null) products.add(p);
                            remaining[0]--;
                            if (remaining[0] == 0 && !products.isEmpty()) {
                                chatAdapter.insertProductsAfterMessage(msg, products);
                                scrollToBottom();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            remaining[0]--;
                        }
                    });
        }
    }

    private void loadProductsByIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        List<Product> products = new ArrayList<>();
        final int[] remaining = {productIds.size()};

        for (String productId : productIds) {
            FirebaseDatabase.getInstance().getReference("products")
                    .child(productId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Product p = snapshot.getValue(Product.class);
                            if (p != null) products.add(p);
                            remaining[0]--;
                            if (remaining[0] == 0 && !products.isEmpty()) {
                                chatAdapter.addProductSuggestions(products);
                                scrollToBottom();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            remaining[0]--;
                        }
                    });
        }
    }

    // ================================================================
    // TYPING INDICATOR (3 chấm nhấp nháy)
    // ================================================================

    private void showTypingIndicator() {
        layoutTyping.setVisibility(View.VISIBLE);
        startDotAnimation();
        scrollToBottom();
    }

    private void hideTypingIndicator() {
        layoutTyping.setVisibility(View.GONE);
        if (typingAnimator != null) {
            typingAnimator.cancel();
        }
    }

    /**
     * Animation 3 chấm nhấp nháy lần lượt
     * Dot1 → Dot2 → Dot3, lặp vô hạn
     */
    private void startDotAnimation() {
        if (typingAnimator != null) typingAnimator.cancel();

        ObjectAnimator anim1 = createDotAnim(dot1, 0);
        ObjectAnimator anim2 = createDotAnim(dot2, 200);
        ObjectAnimator anim3 = createDotAnim(dot3, 400);

        typingAnimator = new AnimatorSet();
        typingAnimator.playTogether(anim1, anim2, anim3);
        typingAnimator.start();
    }

    private ObjectAnimator createDotAnim(View dot, long startDelay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f);
        animator.setDuration(1000);
        animator.setStartDelay(startDelay);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        return animator;
    }

    // ================================================================
    // ADD TO CART
    // ================================================================

    private void addToCart(Product product) {
        FirebaseDatabase.getInstance().getReference("carts").child(userId)
                .orderByChild("productId").equalTo(product.getProductId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Integer qty = child.child("quantity").getValue(Integer.class);
                                child.getRef().child("quantity").setValue((qty != null ? qty : 0) + 1);
                            }
                        } else {
                            String key = FirebaseDatabase.getInstance()
                                    .getReference("carts").child(userId).push().getKey();
                            Map<String, Object> item = new HashMap<>();
                            item.put("productId", product.getProductId());
                            item.put("productName", product.getName());
                            item.put("productImage", product.getFirstImage());
                            item.put("price", product.getDisplayPrice());
                            item.put("quantity", 1);
                            item.put("addedAt", System.currentTimeMillis());
                            FirebaseDatabase.getInstance().getReference("carts")
                                    .child(userId).child(key).setValue(item);
                        }
                        Toast.makeText(getContext(),
                                "Đã thêm " + product.getName() + " vào giỏ",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(),
                                "Lỗi thêm giỏ hàng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================================================================
    // ORDER INTENT LOGIC
    // ================================================================

    private void addBulkToCartAndCheckout(Map<String, Integer> orderMap) {
        if (orderMap.isEmpty()) return;
        Toast.makeText(getContext(), "Đang chốt đơn, vui lòng đợi...", Toast.LENGTH_SHORT).show();

        List<String> productIds = new ArrayList<>(orderMap.keySet());
        final int[] remaining = {productIds.size()};

        for (String productId : productIds) {
            final int requestedQty = orderMap.get(productId) != null ? orderMap.get(productId) : 1;

            FirebaseDatabase.getInstance().getReference("products").child(productId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Product p = snapshot.getValue(Product.class);
                            if (p != null) {
                                if (p.getStock() <= 0) {
                                    remaining[0]--;
                                    if (remaining[0] == 0) navigateToCart();
                                    return;
                                }
                                // Xử lý validate số lượng với tồn kho
                                int actualQty = Math.min(requestedQty, p.getStock());
                                addToCartWithQuantity(p, actualQty, () -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) navigateToCart();
                                });
                            } else {
                                remaining[0]--;
                                if (remaining[0] == 0) navigateToCart();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            remaining[0]--;
                            if (remaining[0] == 0) navigateToCart();
                        }
                    });
        }
    }

    private void addToCartWithQuantity(Product product, int additionalQuantity, Runnable onComplete) {
        FirebaseDatabase.getInstance().getReference("carts").child(userId)
                .orderByChild("productId").equalTo(product.getProductId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        com.google.android.gms.tasks.Task<Void> task;
                        if (snapshot.exists()) {
                            DataSnapshot firstChild = snapshot.getChildren().iterator().next();
                            Integer qty = firstChild.child("quantity").getValue(Integer.class);
                            task = firstChild.getRef().child("quantity").setValue((qty != null ? qty : 0) + additionalQuantity);
                        } else {
                            String key = FirebaseDatabase.getInstance()
                                    .getReference("carts").child(userId).push().getKey();
                            Map<String, Object> item = new HashMap<>();
                            item.put("productId", product.getProductId());
                            item.put("productName", product.getName());
                            item.put("productImage", product.getFirstImage());
                            item.put("price", product.getDisplayPrice());
                            item.put("quantity", additionalQuantity);
                            item.put("addedAt", System.currentTimeMillis());
                            task = FirebaseDatabase.getInstance().getReference("carts")
                                    .child(userId).child(key).setValue(item);
                        }
                        
                        task.addOnCompleteListener(t -> onComplete.run());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onComplete.run();
                    }
                });
    }

    private void navigateToCart() {
        if (!isAdded() || getView() == null) return;
        if (getContext() != null) {
            Toast.makeText(getContext(), "Đã tự động thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
        }
        Navigation.findNavController(requireView()).navigate(R.id.action_global_cart);
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private void scrollToBottom() {
        if (chatAdapter.getLastPosition() >= 0) {
            rvChat.postDelayed(() ->
                    rvChat.smoothScrollToPosition(chatAdapter.getLastPosition()), 100);
        }
    }
}
