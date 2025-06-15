package com.lapxpert.backend.momo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MoMo v3 API Item class for enhanced payment requests.
 * Represents individual items in a payment order.
 */
public class MoMoItem {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    @JsonProperty("manufacturer")
    private String manufacturer;
    
    @JsonProperty("unit")
    private String unit;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("price")
    private Long price;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("totalPrice")
    private Long totalPrice;
    
    @JsonProperty("taxAmount")
    private Long taxAmount;
    
    @JsonProperty("auxiliary")
    private String auxiliary;
    
    // Default constructor
    public MoMoItem() {
        this.currency = "VND";
    }
    
    // Constructor with essential fields
    public MoMoItem(String id, String name, Integer quantity, Long price) {
        this();
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = quantity * price;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.price != null && quantity != null) {
            this.totalPrice = quantity * this.price;
        }
    }
    
    public Long getPrice() {
        return price;
    }
    
    public void setPrice(Long price) {
        this.price = price;
        if (this.quantity != null && price != null) {
            this.totalPrice = this.quantity * price;
        }
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Long getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public Long getTaxAmount() {
        return taxAmount;
    }
    
    public void setTaxAmount(Long taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public String getAuxiliary() {
        return auxiliary;
    }
    
    public void setAuxiliary(String auxiliary) {
        this.auxiliary = auxiliary;
    }
    
    @Override
    public String toString() {
        return "MoMoItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", totalPrice=" + totalPrice +
                ", currency='" + currency + '\'' +
                '}';
    }
}
