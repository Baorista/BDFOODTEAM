    package com.example.khoga.model;

import java.util.Map;
import java.util.List;

public class Order {

    // ── Sub-class: mỗi sản phẩm trong đơn ────────────────────────────────
    public static class OrderItem {
        private String productId;
        private String productName;
        private String productImage;
        private double price;
        private int    quantity;
        private double subtotal;
        private String color;
        private String size;

        public OrderItem() {}

        public OrderItem(String productId, String productName, String productImage,
                         double price, int quantity, String color, String size) {
            this.productId    = productId;
            this.productName  = productName;
            this.productImage = productImage;
            this.price        = price;
            this.quantity     = quantity;
            this.subtotal     = price * quantity;
            this.color        = color;
            this.size         = size;
        }

        public String getProductId()    { return productId; }
        public String getProductName()  { return productName; }
        public String getProductImage() { return productImage; }
        public double getPrice()        { return price; }
        public int    getQuantity()     { return quantity; }
        public double getSubtotal()     { return subtotal; }
        public String getColor()        { return color; }
        public String getSize()         { return size; }

        public void setProductId(String productId)       { this.productId = productId; }
        public void setProductName(String productName)   { this.productName = productName; }
        public void setProductImage(String productImage) { this.productImage = productImage; }
        public void setPrice(double price)               { this.price = price; }
        public void setQuantity(int quantity)            { this.quantity = quantity; }
        public void setSubtotal(double subtotal)         { this.subtotal = subtotal; }
        public void setColor(String color)               { this.color = color; }
        public void setSize(String size)                 { this.size = size; }
    }

    // ── Sub-class: địa chỉ giao hàng (snapshot) ──────────────────────────
    public static class ShippingAddress {
        private String recipientName;
        private String phone;
        private String province;
        private String district;
        private String ward;
        private String detail;
        private String fullAddress;

        public ShippingAddress() {}

        public ShippingAddress(String recipientName, String phone, String province,
                               String district, String ward, String detail) {
            this.recipientName = recipientName;
            this.phone         = phone;
            this.province      = province;
            this.district      = district;
            this.ward          = ward;
            this.detail        = detail;
            this.fullAddress   = detail + ", " + ward + ", " + district + ", " + province;
        }

        public String getRecipientName() { return recipientName; }
        public String getPhone()         { return phone; }
        public String getProvince()      { return province; }
        public String getDistrict()      { return district; }
        public String getWard()          { return ward; }
        public String getDetail()        { return detail; }
        public String getFullAddress()   { return fullAddress; }

        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
        public void setPhone(String phone)                 { this.phone = phone; }
        public void setProvince(String province)           { this.province = province; }
        public void setDistrict(String district)           { this.district = district; }
        public void setWard(String ward)                   { this.ward = ward; }
        public void setDetail(String detail)               { this.detail = detail; }
        public void setFullAddress(String fullAddress)     { this.fullAddress = fullAddress; }
    }

    // ── Fields phụ (flag đánh giá) ────────────────────────────────────────
    private boolean reviewed;

    // ── Fields chính ──────────────────────────────────────────────────────
    private String orderId;
    private String userId;

    // items dùng Map<String, OrderItem> thay vì List để tương thích Firebase
    // Firebase lưu dạng { "0": {...}, "1": {...} }
    private List<OrderItem> items;

    private double subtotal;
    private double shippingFee;
    private double totalAmount;

    private ShippingAddress shippingAddress;

    // Enum values chuẩn theo schema:
    // shippingMethod : "standard" | "express"
    // paymentMethod  : "cod" | "vnpay" | "momo"
    // paymentStatus  : "pending" | "paid" | "failed"
    // orderStatus    : "pending" | "confirmed" | "shipping" | "delivered" | "cancelled"
    private String shippingMethod;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;

    private long createdAt;
    private long updatedAt;

    // Constructor rỗng bắt buộc để Firebase deserialize
    public Order() {}

    public Order(String orderId, String userId, List<OrderItem> items,
                 double subtotal, double shippingFee,
                 ShippingAddress shippingAddress,
                 String shippingMethod, String paymentMethod,
                 String paymentStatus, String orderStatus,
                 long createdAt, long updatedAt) {
        this.orderId          = orderId;
        this.userId           = userId;
        this.items            = items;
        this.subtotal         = subtotal;
        this.shippingFee      = shippingFee;
        this.totalAmount      = subtotal + shippingFee;
        this.shippingAddress  = shippingAddress;
        this.shippingMethod   = shippingMethod;
        this.paymentMethod    = paymentMethod;
        this.paymentStatus    = paymentStatus;
        this.orderStatus      = orderStatus;
        this.createdAt        = createdAt;
        this.updatedAt        = updatedAt;
    }

    // Getters
    public String getOrderId()                    { return orderId; }
    public String getUserId()                     { return userId; }
    public List<OrderItem> getItems()      { return items; }
    public double getSubtotal()                   { return subtotal; }
    public double getShippingFee()                { return shippingFee; }
    public double getTotalAmount()                { return totalAmount; }
    public ShippingAddress getShippingAddress()   { return shippingAddress; }
    public String getShippingMethod()             { return shippingMethod; }
    public String getPaymentMethod()              { return paymentMethod; }
    public String getPaymentStatus()              { return paymentStatus; }
    public String getOrderStatus()                { return orderStatus; }
    public long   getCreatedAt()                  { return createdAt; }
    public long   getUpdatedAt()                  { return updatedAt; }

    // Setters
    public void setOrderId(String orderId)                          { this.orderId = orderId; }
    public void setUserId(String userId)                            { this.userId = userId; }
    public void setItems(List<OrderItem> items)              { this.items = items; }
    public void setSubtotal(double subtotal)                        { this.subtotal = subtotal; }
    public void setShippingFee(double shippingFee)                  { this.shippingFee = shippingFee; }
    public void setTotalAmount(double totalAmount)                  { this.totalAmount = totalAmount; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }
    public void setShippingMethod(String shippingMethod)            { this.shippingMethod = shippingMethod; }
    public void setPaymentMethod(String paymentMethod)              { this.paymentMethod = paymentMethod; }
    public void setPaymentStatus(String paymentStatus)              { this.paymentStatus = paymentStatus; }
    public void setOrderStatus(String orderStatus)                  { this.orderStatus = orderStatus; }
    public void setCreatedAt(long createdAt)                        { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt)                        { this.updatedAt = updatedAt; }
    public boolean isReviewed()                                     { return reviewed; }
    public void setReviewed(boolean reviewed)                       { this.reviewed = reviewed; }

    // ── Helper methods ──────────────────────────────────────────────────
    public boolean canCancel() {
        return "pending".equals(orderStatus) || "confirmed".equals(orderStatus);
    }

    public boolean isDelivered() {
        return "delivered".equals(orderStatus);
    }
}