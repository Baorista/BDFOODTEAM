package com.example.khoga.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Constants {

    private Constants() {}

    // Gemini API
    // public static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
    // public static final String GEMINI_MODEL = "gemini-2.0-flash";
    // public static final String GEMINI_API_KEY = "AIzaSyBztIVuhUWPQykNwa2Jb6P3M50XgCTqgb0";

    // Chat
    public static final int CHAT_HISTORY_LIMIT = 20;
    public static final int CHAT_MAX_STORED = 50;

    // Browsing History
    public static final int BROWSING_HISTORY_TOP_N = 10;
    public static final long BROWSING_HISTORY_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000; // 30 days

    // Firebase Nodes
    public static final String NODE_USERS = "users";
    public static final String NODE_ADDRESSES = "addresses";
    public static final String NODE_WISHLISTS = "wishlists";
    public static final String NODE_CATEGORIES = "categories";
    public static final String NODE_PRODUCTS = "products";
    public static final String NODE_BANNERS = "banners";
    public static final String NODE_CARTS = "carts";
    public static final String NODE_ORDERS = "orders";
    public static final String NODE_REVIEWS = "reviews";
    public static final String NODE_CHAT_HISTORY = "chatHistory";
    public static final String NODE_BROWSING_HISTORY = "browsingHistory";

    // Order Status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_SHIPPING = "shipping";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_CANCELLED = "cancelled";

    // Payment Methods
    // public static final String PAYMENT_COD = "cod";
    // public static final String PAYMENT_ZALOPAY = "zalopay";
    // public static final String PAYMENT_MOMO = "momo";

    // Payment Status
    // public static final String PAYMENT_PENDING = "pending";
    // public static final String PAYMENT_PAID = "paid";
    // public static final String PAYMENT_FAILED = "failed";

    // // User Roles
    // public static final String ROLE_CUSTOMER = "customer";
    // public static final String ROLE_ADMIN = "admin";

    // Chat Roles
    public static final String CHAT_ROLE_USER = "user";
    public static final String CHAT_ROLE_ASSISTANT = "assistant";

    // Intent Extras
    // public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    // public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    // public static final String EXTRA_ORDER_ID = "extra_order_id";
    // public static final String EXTRA_PRODUCT_NAME = "extra_product_name";
    // public static final String EXTRA_PRODUCT_IMAGE = "extra_product_image";

    // Order status display labels
    public static final Map<String, String> ORDER_STATUS_LABELS;
    static {
        Map<String, String> m = new HashMap<>();
        m.put(STATUS_PENDING, "Chờ xác nhận");
        m.put(STATUS_CONFIRMED, "Đã xác nhận");
        m.put(STATUS_SHIPPING, "Đang giao hàng");
        m.put(STATUS_DELIVERED, "Đã giao hàng");
        m.put(STATUS_CANCELLED, "Đã hủy");
        ORDER_STATUS_LABELS = Collections.unmodifiableMap(m);
    }

    // Order status colors (resource-independent int colors)
    public static final Map<String, Integer> ORDER_STATUS_COLORS;
    static {
        Map<String, Integer> m = new HashMap<>();
        m.put(STATUS_PENDING, 0xFF888888);
        m.put(STATUS_CONFIRMED, 0xFF2196F3);
        m.put(STATUS_SHIPPING, 0xFFFF9800);
        m.put(STATUS_DELIVERED, 0xFF4CAF50);
        m.put(STATUS_CANCELLED, 0xFFF44336);
        ORDER_STATUS_COLORS = Collections.unmodifiableMap(m);
    }

    public static String getStatusLabel(String status) {
        String label = ORDER_STATUS_LABELS.get(status);
        return label != null ? label : status;
    }

    public static int getStatusColor(String status) {
        Integer color = ORDER_STATUS_COLORS.get(status);
        return color != null ? color : 0xFF888888;
    }
}
