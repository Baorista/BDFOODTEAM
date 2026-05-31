package com.example.khoga.model;

public class CartItem {
    private String itemId;
    private String productId;
    private String productName;
    private String productImage;
    private double price;
    private int quantity;
    private String selectedColor;
    private String selectedSize;
    private long addedAt;
    private boolean selected = true;

    public CartItem() {}

    public CartItem(String itemId, String productId, String productName,
                    String productImage, double price, int quantity,
                    String selectedColor, String selectedSize, long addedAt) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.price = price;
        this.quantity = quantity;
        this.selectedColor = selectedColor;
        this.selectedSize = selectedSize;
        this.addedAt = addedAt;
        this.selected = true;
    }

    public String getItemId()        { return itemId; }
    public String getProductId()     { return productId; }
    public String getProductName()   { return productName; }
    public String getProductImage()  { return productImage; }
    public double getPrice()         { return price; }
    public int    getQuantity()      { return quantity; }
    public String getSelectedColor() { return selectedColor; }
    public String getSelectedSize()  { return selectedSize; }
    public long   getAddedAt()       { return addedAt; }
    public boolean isSelected()      { return selected; }

    public void setItemId(String itemId)               { this.itemId = itemId; }
    public void setProductId(String productId)         { this.productId = productId; }
    public void setProductName(String productName)     { this.productName = productName; }
    public void setProductImage(String productImage)   { this.productImage = productImage; }
    public void setPrice(double price)                 { this.price = price; }
    public void setQuantity(int quantity)              { this.quantity = quantity; }
    public void setSelectedColor(String selectedColor) { this.selectedColor = selectedColor; }
    public void setSelectedSize(String selectedSize)   { this.selectedSize = selectedSize; }
    public void setAddedAt(long addedAt)               { this.addedAt = addedAt; }
    public void setSelected(boolean selected)          { this.selected = selected; }
}
