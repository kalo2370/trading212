package com.cryptosim.trading212.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a cryptocurrency asset held within a user's portfolio.
 * This class maps to the 'portfolio_assets' table in the database.
 */
public class PortfolioAsset {

    private Integer assetId;
    private Integer accountId;
    private String assetSymbol; // e.g., "XBT/USD"
    private BigDecimal quantity;
    private BigDecimal averagePurchasePrice; // Weighted average price
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public PortfolioAsset() {
    }

    public PortfolioAsset(Integer assetId, Integer accountId, String assetSymbol, BigDecimal quantity, BigDecimal averagePurchasePrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.assetId = assetId;
        this.accountId = accountId;
        this.assetSymbol = assetSymbol;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
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

    public BigDecimal getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public void setAveragePurchasePrice(BigDecimal averagePurchasePrice) {
        this.averagePurchasePrice = averagePurchasePrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // toString method for logging and debugging
    @Override
    public String toString() {
        return "PortfolioAsset{" +
                "assetId=" + assetId +
                ", accountId=" + accountId +
                ", assetSymbol='" + assetSymbol + '\'' +
                ", quantity=" + quantity +
                ", averagePurchasePrice=" + averagePurchasePrice +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}