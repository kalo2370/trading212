package com.cryptosim.trading212.services;

import com.cryptosim.trading212.daos.contracts.AccountDao;
import com.cryptosim.trading212.daos.contracts.PortfolioAssetDao;
import com.cryptosim.trading212.daos.contracts.TransactionDao;
import com.cryptosim.trading212.models.Account;
import com.cryptosim.trading212.models.PortfolioAsset;
import com.cryptosim.trading212.models.Transaction;
import com.cryptosim.trading212.models.TransactionType;
import com.cryptosim.trading212.services.contracts.KrakenDataService;
import com.cryptosim.trading212.services.contracts.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service class for handling core trading logic, account management,
 * portfolio updates, and transaction logging.
 */
@Service
public class TradingServiceImpl implements TradingService {

    private static final Logger logger = LoggerFactory.getLogger(TradingServiceImpl.class);

    private final AccountDao accountDao;
    private final PortfolioAssetDao portfolioAssetDao;
    private final TransactionDao transactionDao;
    private final KrakenDataService krakenDataService;

    private static final int FIAT_SCALE = 2;
    private static final int CRYPTO_QUANTITY_SCALE = 8;
    private static final int PRICE_SCALE = 8;


    @Autowired
    public TradingServiceImpl(AccountDao accountDao,
                              PortfolioAssetDao portfolioAssetDao,
                              TransactionDao transactionDao,
                              KrakenDataService krakenDataService) {
        this.accountDao = accountDao;
        this.portfolioAssetDao = portfolioAssetDao;
        this.transactionDao = transactionDao;
        this.krakenDataService = krakenDataService;
    }

    /**
     * Retrieves an account by its user identifier.
     *
     * @param userIdentifier The user's unique identifier.
     * @return The Account object.
     * @throws NoSuchElementException if the account is not found.
     */
    private Account getAccountByUserIdentifier(String userIdentifier) {
        return accountDao.findByUserIdentifier(userIdentifier)
                .orElseThrow(() -> {
                    logger.warn("Account not found for userIdentifier: {}", userIdentifier);
                    return new NoSuchElementException("Account not found for user: " + userIdentifier);
                });
    }

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
    @Transactional
    @Override
    public Transaction buyCrypto(String userIdentifier, String assetSymbol, BigDecimal cryptoQuantity) {
        logger.info("Attempting to buy {} of {} for user {}", cryptoQuantity, assetSymbol, userIdentifier);

        if (cryptoQuantity == null || cryptoQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Invalid quantity for buy: {}", cryptoQuantity);
            throw new IllegalArgumentException("Quantity to buy must be positive.");
        }

        Account account = getAccountByUserIdentifier(userIdentifier);
        BigDecimal currentPrice = krakenDataService.getPriceForSymbol(assetSymbol);

        if (currentPrice == null) {
            logger.error("Price not available for asset: {}", assetSymbol);
            throw new NoSuchElementException("Price for " + assetSymbol + " is not currently available.");
        }

        BigDecimal cost = cryptoQuantity.multiply(currentPrice).setScale(FIAT_SCALE, RoundingMode.HALF_UP);

        if (account.getBalance().compareTo(cost) < 0) {
            logger.error("Insufficient funds for user {}. Balance: {}, Cost: {}", userIdentifier, account.getBalance(), cost);
            throw new IllegalArgumentException("Insufficient funds to complete the purchase. Required: " + cost + ", Available: " + account.getBalance());
        }

        // Update account balance
        BigDecimal newBalance = account.getBalance().subtract(cost);
        accountDao.updateBalance(account.getAccountId(), newBalance);
        logger.info("Updated balance for account {}: {}", account.getAccountId(), newBalance);

        // Update portfolio
        Optional<PortfolioAsset> existingAssetOpt = portfolioAssetDao.findByAccountIdAndAssetSymbol(account.getAccountId(), assetSymbol);
        if (existingAssetOpt.isPresent()) {
            PortfolioAsset existingAsset = existingAssetOpt.get();
            BigDecimal totalQuantity = existingAsset.getQuantity().add(cryptoQuantity);
            BigDecimal oldTotalValue = existingAsset.getQuantity().multiply(existingAsset.getAveragePurchasePrice());
            BigDecimal newPurchaseValue = cryptoQuantity.multiply(currentPrice);
            BigDecimal newAveragePrice = oldTotalValue.add(newPurchaseValue)
                    .divide(totalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);

            portfolioAssetDao.updateAsset(existingAsset.getAssetId(), totalQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN), newAveragePrice);
            logger.info("Updated asset {} for account {}", assetSymbol, account.getAccountId());
        } else {
            PortfolioAsset newAsset = new PortfolioAsset();
            newAsset.setAccountId(account.getAccountId());
            newAsset.setAssetSymbol(assetSymbol);
            newAsset.setQuantity(cryptoQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN));
            newAsset.setAveragePurchasePrice(currentPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
            portfolioAssetDao.addAsset(newAsset);
            logger.info("Added new asset {} for account {}", assetSymbol, account.getAccountId());
        }

        // Log transaction
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAssetSymbol(assetSymbol);
        transaction.setTransactionType(TransactionType.BUY);
        transaction.setQuantity(cryptoQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN));
        transaction.setPricePerUnit(currentPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        transaction.setTotalTransactionValue(cost);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        Transaction loggedTransaction = transactionDao.logTransaction(transaction);
        logger.info("Logged BUY transaction ID: {}", loggedTransaction.getTransactionId());

        return loggedTransaction;
    }

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
    @Transactional
    @Override
    public Transaction sellCrypto(String userIdentifier, String assetSymbol, BigDecimal cryptoQuantity) {
        logger.info("Attempting to sell {} of {} for user {}", cryptoQuantity, assetSymbol, userIdentifier);

        if (cryptoQuantity == null || cryptoQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Invalid quantity for sell: {}", cryptoQuantity);
            throw new IllegalArgumentException("Quantity to sell must be positive.");
        }

        Account account = getAccountByUserIdentifier(userIdentifier);
        PortfolioAsset assetToSell = portfolioAssetDao.findByAccountIdAndAssetSymbol(account.getAccountId(), assetSymbol)
                .orElseThrow(() -> {
                    logger.error("Asset {} not found in portfolio for user {}", assetSymbol, userIdentifier);
                    return new NoSuchElementException("Asset " + assetSymbol + " not found in your portfolio.");
                });

        if (assetToSell.getQuantity().compareTo(cryptoQuantity) < 0) {
            logger.error("Insufficient asset quantity for user {}. Available: {}, Trying to sell: {}", userIdentifier, assetToSell.getQuantity(), cryptoQuantity);
            throw new IllegalArgumentException("Insufficient quantity of " + assetSymbol + " to sell. Available: " + assetToSell.getQuantity() + ", Requested: " + cryptoQuantity);
        }

        BigDecimal currentPrice = krakenDataService.getPriceForSymbol(assetSymbol);
        if (currentPrice == null) {
            logger.error("Price not available for asset: {}", assetSymbol);
            throw new NoSuchElementException("Price for " + assetSymbol + " is not currently available to complete the sale.");
        }

        BigDecimal proceeds = cryptoQuantity.multiply(currentPrice).setScale(FIAT_SCALE, RoundingMode.HALF_UP);
        BigDecimal costBasisOfSoldPortion = cryptoQuantity.multiply(assetToSell.getAveragePurchasePrice()).setScale(FIAT_SCALE, RoundingMode.HALF_UP);
        BigDecimal realizedProfitLoss = proceeds.subtract(costBasisOfSoldPortion);

        // Update account balance
        BigDecimal newBalance = account.getBalance().add(proceeds);
        accountDao.updateBalance(account.getAccountId(), newBalance);
        logger.info("Updated balance for account {}: {}", account.getAccountId(), newBalance);

        // Update portfolio
        BigDecimal remainingQuantity = assetToSell.getQuantity().subtract(cryptoQuantity);
        if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            portfolioAssetDao.deleteAsset(assetToSell.getAssetId());
            logger.info("Removed asset {} (ID: {}) from portfolio for account {} as quantity is now zero or less.", assetSymbol, assetToSell.getAssetId(), account.getAccountId());
        } else {
            portfolioAssetDao.updateAsset(assetToSell.getAssetId(), remainingQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN), assetToSell.getAveragePurchasePrice());
            logger.info("Updated quantity for asset {} (ID: {}) in portfolio for account {}. New quantity: {}", assetSymbol, assetToSell.getAssetId(), account.getAccountId(), remainingQuantity);
        }

        // Log transaction
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAssetSymbol(assetSymbol);
        transaction.setTransactionType(TransactionType.SELL);
        transaction.setQuantity(cryptoQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN));
        transaction.setPricePerUnit(currentPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        transaction.setTotalTransactionValue(proceeds);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        transaction.setRealizedProfitLoss(realizedProfitLoss.setScale(FIAT_SCALE, RoundingMode.HALF_UP));
        Transaction loggedTransaction = transactionDao.logTransaction(transaction);
        logger.info("Logged SELL transaction ID: {}. Realized P/L: {}", loggedTransaction.getTransactionId(), realizedProfitLoss);

        return loggedTransaction;
    }

    /**
     * Resets the account of the specified user to its initial balance and clears their portfolio.
     *
     * @param userIdentifier The identifier of the user whose account is to be reset.
     * @return The updated Account object after reset.
     * @throws NoSuchElementException if the account or initial balance is not found.
     */
    @Transactional
    @Override
    public Account resetAccount(String userIdentifier) {
        logger.info("Attempting to reset account for user: {}", userIdentifier);
        Account account = getAccountByUserIdentifier(userIdentifier);

        BigDecimal initialBalance = accountDao.getInitialBalance(account.getAccountId())
                .orElseThrow(() -> {
                    logger.error("Initial balance not found for accountId: {}", account.getAccountId());
                    return new NoSuchElementException("Initial balance configuration missing for account ID: " + account.getAccountId());
                });

        accountDao.updateBalance(account.getAccountId(), initialBalance);
        logger.info("Account balance reset to initial value: {} for accountId: {}", initialBalance, account.getAccountId());
        int assetsDeleted = portfolioAssetDao.deleteAllAssetsByAccountId(account.getAccountId());
        logger.info("Deleted {} assets from portfolio for accountId: {}", assetsDeleted, account.getAccountId());

        return accountDao.findById(account.getAccountId())
                .orElseThrow(() -> new NoSuchElementException("Failed to reload account after reset for ID: " + account.getAccountId()));
    }


    /**
     * Retrieves the portfolio for a given user.
     * The portfolio includes current holdings and their market values.
     *
     * @param userIdentifier The identifier of the user.
     * @return A list of PortfolioAsset objects, potentially enriched with current market value.
     * @throws NoSuchElementException if the account is not found.
     */
    @Override
    public List<PortfolioAsset> getPortfolio(String userIdentifier) {
        Account account = getAccountByUserIdentifier(userIdentifier);
        List<PortfolioAsset> assets = portfolioAssetDao.findByAccountId(account.getAccountId());
        logger.debug("Retrieved {} assets for user {}", assets.size(), userIdentifier);
        return assets;
    }

    /**
     * Retrieves the transaction history for a given user.
     *
     * @param userIdentifier The identifier of the user.
     * @return A list of Transaction objects.
     * @throws NoSuchElementException if the account is not found.
     */
    @Override
    public List<Transaction> getTransactionHistory(String userIdentifier) {
        Account account = getAccountByUserIdentifier(userIdentifier);
        List<Transaction> transactions = transactionDao.findByAccountId(account.getAccountId());
        logger.debug("Retrieved {} transactions for user {}", transactions.size(), userIdentifier);
        return transactions;
    }

    /**
     * Retrieves the account details for a given user.
     * @param userIdentifier The identifier of the user.
     * @return The Account object.
     */
    @Override
    public Account getAccountDetails(String userIdentifier) {
        return getAccountByUserIdentifier(userIdentifier);
    }
}
