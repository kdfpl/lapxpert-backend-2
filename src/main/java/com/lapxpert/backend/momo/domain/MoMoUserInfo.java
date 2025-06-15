package com.lapxpert.backend.momo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MoMo v3 API UserInfo class for enhanced payment requests.
 * Contains user information for payment processing.
 */
public class MoMoUserInfo {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("email")
    private String email;
    
    // Default constructor
    public MoMoUserInfo() {
    }
    
    // Constructor with essential fields
    public MoMoUserInfo(String name, String phoneNumber, String email) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "MoMoUserInfo{" +
                "name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
