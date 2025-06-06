package com.cryptosim.trading212.controllers;

import com.cryptosim.trading212.models.Account;
import com.cryptosim.trading212.models.PortfolioAsset;
import com.cryptosim.trading212.models.Transaction;
import com.cryptosim.trading212.services.KrakenDataServiceImpl;
import com.cryptosim.trading212.services.TradingServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * REST Controller for account-related operations like fetching balance,
 * portfolio, transaction history, and resetting the account.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final TradingServiceImpl tradingServiceImpl;
    private final KrakenDataServiceImpl krakenDataServiceImpl; // For enriching portfolio with current prices

    @Autowired
    public AccountController(TradingServiceImpl tradingServiceImpl, KrakenDataServiceImpl krakenDataServiceImpl) {
        this.tradingServiceImpl = tradingServiceImpl;
        this.krakenDataServiceImpl = krakenDataServiceImpl;
    }

    /**
     * Retrieves account details for a given user identifier.
     * @param userIdentifier The unique identifier for the user.
     * @return ResponseEntity with Account details or an error status.
     */
    @GetMapping("/{userIdentifier}")
    public ResponseEntity<?> getAccountDetails(@PathVariable String userIdentifier) {
        try {
            Account account = tradingServiceImpl.getAccountDetails(userIdentifier);
            return ResponseEntity.ok(account);
        } catch (NoSuchElementException e) {
            logger.warn("Account details request failed for {}: {}", userIdentifier, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching account details for {}: ", userIdentifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    /**
     * Retrieves the portfolio for a given user, enriched with current market values.
     * @param userIdentifier The unique identifier for the user.
     * @return ResponseEntity with a list of portfolio assets (enriched) or an error status.
     */
    @GetMapping("/{userIdentifier}/portfolio")
    public ResponseEntity<?> getPortfolio(@PathVariable String userIdentifier) {
        try {
            List<PortfolioAsset> portfolio = tradingServiceImpl.getPortfolio(userIdentifier);
            // Enrich with current market value
            List<Map<String, Object>> enrichedPortfolio = portfolio.stream().map(asset -> {
                Map<String, Object> assetMap = new HashMap<>(); // Use HashMap for explicit typing
                BigDecimal currentPrice = krakenDataServiceImpl.getPriceForSymbol(asset.getAssetSymbol());
                BigDecimal currentValue = BigDecimal.ZERO;

                if (currentPrice != null && asset.getQuantity() != null) {
                    currentValue = asset.getQuantity().multiply(currentPrice);
                }

                assetMap.put("assetId", asset.getAssetId());
                assetMap.put("accountId", asset.getAccountId());
                assetMap.put("assetSymbol", asset.getAssetSymbol());
                assetMap.put("quantity", asset.getQuantity());
                assetMap.put("averagePurchasePrice", asset.getAveragePurchasePrice());
                assetMap.put("currentPrice", currentPrice != null ? (Object)currentPrice : "N/A"); // Cast to Object if needed, or ensure consistent type
                assetMap.put("currentMarketValue", currentValue);
                assetMap.put("createdAt", asset.getCreatedAt());
                assetMap.put("updatedAt", asset.getUpdatedAt());

                return assetMap;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(enrichedPortfolio);
        } catch (NoSuchElementException e) {
            logger.warn("Portfolio request failed for {}: {}", userIdentifier, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching portfolio for {}: ", userIdentifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    /**
     * Retrieves the transaction history for a given user.
     * @param userIdentifier The unique identifier for the user.
     * @return ResponseEntity with a list of transactions or an error status.
     */
    @GetMapping("/{userIdentifier}/transactions")
    public ResponseEntity<?> getTransactionHistory(@PathVariable String userIdentifier) {
        try {
            List<Transaction> transactions = tradingServiceImpl.getTransactionHistory(userIdentifier);
            return ResponseEntity.ok(transactions);
        } catch (NoSuchElementException e) {
            logger.warn("Transaction history request failed for {}: {}", userIdentifier, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching transaction history for {}: ", userIdentifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    /**
     * Resets the account for the given user identifier to its initial state.
     * @param userIdentifier The unique identifier for the user.
     * @return ResponseEntity with the updated Account details or an error status.
     */
    @PostMapping("/{userIdentifier}/reset")
    public ResponseEntity<?> resetAccount(@PathVariable String userIdentifier) {
        try {
            Account account = tradingServiceImpl.resetAccount(userIdentifier);
            logger.info("Account reset successfully for user: {}", userIdentifier);
            return ResponseEntity.ok(account);
        } catch (NoSuchElementException e) {
            logger.warn("Account reset failed for {}: {}", userIdentifier, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error resetting account for {}: ", userIdentifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during account reset.");
        }
    }
}
