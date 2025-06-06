package com.cryptosim.trading212.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a buy or sell transaction.
 * Corresponds to the 'transactions' table in the database.
 */
public class Transaction {

    private int transactionId;
    private int accountId; // Foreign key to Account
    private String assetSymbol;
    private TransactionType transactionType;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalTransactionValue;
    private LocalDateTime transactionTimestamp;
    private BigDecimal realizedProfitLoss; // Nullable

    // Constructors
    public Transaction() {
    }

    public Transaction(int transactionId, int accountId, String assetSymbol, TransactionType transactionType,
                       BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal totalTransactionValue,
                       LocalDateTime transactionTimestamp, BigDecimal realizedProfitLoss) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.assetSymbol = assetSymbol;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalTransactionValue = totalTransactionValue;
        this.transactionTimestamp = transactionTimestamp;
        this.realizedProfitLoss = realizedProfitLoss;
    }

    // Getters and Setters
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public BigDecimal getTotalTransactionValue() {
        return totalTransactionValue;
    }

    public void setTotalTransactionValue(BigDecimal totalTransactionValue) {
        this.totalTransactionValue = totalTransactionValue;
    }

    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public BigDecimal getRealizedProfitLoss() {
        return realizedProfitLoss;
    }

    public void setRealizedProfitLoss(BigDecimal realizedProfitLoss) {
        this.realizedProfitLoss = realizedProfitLoss;
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", accountId=" + accountId +
                ", assetSymbol='" + assetSymbol + '\'' +
                ", transactionType=" + transactionType +
                ", quantity=" + quantity +
                ", pricePerUnit=" + pricePerUnit +
                ", totalTransactionValue=" + totalTransactionValue +
                ", transactionTimestamp=" + transactionTimestamp +
                ", realizedProfitLoss=" + realizedProfitLoss +
                '}';
    }
}
