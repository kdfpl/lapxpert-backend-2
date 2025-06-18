package com.lapxpert.backend.payment.momo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MoMo v3 API DeliveryInfo class for enhanced payment requests.
 * Contains delivery information for payment processing.
 */
public class MoMoDeliveryInfo {
    
    @JsonProperty("deliveryAddress")
    private String deliveryAddress;
    
    @JsonProperty("deliveryFee")
    private String deliveryFee;
    
    @JsonProperty("quantity")
    private String quantity;
    
    @JsonProperty("amount")
    private String amount;
    
    // Default constructor
    public MoMoDeliveryInfo() {
    }
    
    // Constructor with essential fields
    public MoMoDeliveryInfo(String deliveryAddress, String deliveryFee, String quantity, String amount) {
        this.deliveryAddress = deliveryAddress;
        this.deliveryFee = deliveryFee;
        this.quantity = quantity;
        this.amount = amount;
    }
    
    // Getters and Setters
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getDeliveryFee() {
        return deliveryFee;
    }
    
    public void setDeliveryFee(String deliveryFee) {
        this.deliveryFee = deliveryFee;
    }
    
    public String getQuantity() {
        return quantity;
    }
    
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
    
    public String getAmount() {
        return amount;
    }
    
    public void setAmount(String amount) {
        this.amount = amount;
    }
    
    @Override
    public String toString() {
        return "MoMoDeliveryInfo{" +
                "deliveryAddress='" + deliveryAddress + '\'' +
                ", deliveryFee='" + deliveryFee + '\'' +
                ", quantity='" + quantity + '\'' +
                ", amount='" + amount + '\'' +
                '}';
    }
}
