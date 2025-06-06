package com.cryptosim.trading212.services.contracts;

import com.cryptosim.trading212.models.Account;
import com.cryptosim.trading212.models.PortfolioAsset;
import com.cryptosim.trading212.models.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException; // Keep as it's part of the public contract for exceptions

/**
 * Interface for the Trading Service.
 * Defines the contract for handling core trading logic, account management,
 * portfolio updates, and transaction logging.
 */
public interface TradingService {

    /**
     * Processes a buy transaction for a given user and asset.
     *
     * @param userIdentifier The identifier of the user making the purchase.
     * @param assetSymbol    The symbol of the cryptocurrency to buy (e.g., "XBT/USD").
     * @param cryptoQuantity The amount of cryptocurrency to buy.
     * @return The created Transaction object.
     * @throws IllegalArgumentException if inputs are invalid (e.g., insufficient funds, invalid quantity).
     * @throws NoSuchElementException if asset price is not available or account not found.
     */
    Transaction buyCrypto(String userIdentifier, String assetSymbol, BigDecimal cryptoQuantity);

    /**
     * Processes a sell transaction for a given user and asset.
     *
     * @param userIdentifier The identifier of the user making the sale.
     * @param assetSymbol    The symbol of the cryptocurrency to sell (e.g., "XBT/USD").
     * @param cryptoQuantity The amount of cryptocurrency to sell.
     * @return The created Transaction object.
     * @throws IllegalArgumentException if inputs are invalid (e.g., insufficient assets, invalid quantity).
     * @throws NoSuchElementException if asset is not in portfolio, price is not available, or account not found.
     */
    Transaction sellCrypto(String userIdentifier, String assetSymbol, BigDecimal cryptoQuantity);

    /**
     * Resets the account of the specified user to its initial balance and clears their portfolio.
     *
     * @param userIdentifier The identifier of the user whose account is to be reset.
     * @return The updated Account object after reset.
     * @throws NoSuchElementException if the account or initial balance is not found.
     */
    Account resetAccount(String userIdentifier);

    /**
     * Retrieves the portfolio for a given user.
     * The portfolio includes current holdings. The actual enrichment with current market values
     * might be done at the controller or DTO level, but the service provides the core asset data.
     *
     * @param userIdentifier The identifier of the user.
     * @return A list of PortfolioAsset objects.
     * @throws NoSuchElementException if the account is not found.
     */
    List<PortfolioAsset> getPortfolio(String userIdentifier);

    /**
     * Retrieves the transaction history for a given user.
     *
     * @param userIdentifier The identifier of the user.
     * @return A list of Transaction objects.
     * @throws NoSuchElementException if the account is not found.
     */
    List<Transaction> getTransactionHistory(String userIdentifier);

    /**
     * Retrieves the account details for a given user.
     *
     * @param userIdentifier The identifier of the user.
     * @return The Account object.
     * @throws NoSuchElementException if the account is not found.
     */
    Account getAccountDetails(String userIdentifier);
}
