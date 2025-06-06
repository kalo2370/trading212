package com.cryptosim.trading212.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an account in the crypto trading simulator.
 * Corresponds to the 'accounts' table in the database.
 */
public class Account {

    private int accountId;
    private String userIdentifier;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Account() {
    }

    public Account(int accountId, String userIdentifier, BigDecimal balance, BigDecimal initialBalance, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.userIdentifier = userIdentifier;
        this.balance = balance;
        this.initialBalance = initialBalance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
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

    // toString method for debugging
    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", userIdentifier='" + userIdentifier + '\'' +
                ", balance=" + balance +
                ", initialBalance=" + initialBalance +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}