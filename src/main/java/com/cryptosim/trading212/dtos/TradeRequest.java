package com.cryptosim.trading212.dtos;

import java.math.BigDecimal;

/**
 * Data Transfer Object for buy/sell requests.
 */
public class TradeRequest {
    private String userIdentifier;
    private String assetSymbol;
    private BigDecimal quantity;

    // Getters and Setters
    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    // toString for logging
    @Override
    public String toString() {
        return "TradeRequest{" +
                "userIdentifier='" + userIdentifier + '\'' +
                ", assetSymbol='" + assetSymbol + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
