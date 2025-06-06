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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the TradingService class.
 * Uses JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    @Mock
    private AccountDao accountDao;

    @Mock
    private PortfolioAssetDao portfolioAssetDao;

    @Mock
    private TransactionDao transactionDao;

    @Mock
    private KrakenDataService krakenDataService;

    @InjectMocks
    private TradingServiceImpl tradingService;

    private Account testAccount;
    private final String USER_IDENTIFIER = "testUser";
    private final String ASSET_SYMBOL_BTC = "BTC/USD";
    private final String ASSET_SYMBOL_ETH = "ETH/USD";
    private static final int FIAT_SCALE = 2;
    private static final int CRYPTO_QUANTITY_SCALE = 8;
    private static final int PRICE_SCALE = 8;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAccountId(1);
        testAccount.setUserIdentifier(USER_IDENTIFIER);
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setInitialBalance(new BigDecimal("10000.00"));
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());
    }

    //buyCrypto tests
    @Test
    void buyCrypto_success_newAsset() {
        BigDecimal quantityToBuy = new BigDecimal("0.1");
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal expectedCost = quantityToBuy.multiply(price).setScale(FIAT_SCALE, RoundingMode.HALF_UP);

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(krakenDataService.getPriceForSymbol(ASSET_SYMBOL_BTC)).thenReturn(price);
        when(portfolioAssetDao.findByAccountIdAndAssetSymbol(testAccount.getAccountId(), ASSET_SYMBOL_BTC)).thenReturn(Optional.empty());
        when(transactionDao.logTransaction(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(portfolioAssetDao.addAsset(any(PortfolioAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = tradingService.buyCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToBuy);

        assertNotNull(result);
        assertEquals(TransactionType.BUY, result.getTransactionType());
        assertEquals(ASSET_SYMBOL_BTC, result.getAssetSymbol());
        assertEquals(0, quantityToBuy.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN).compareTo(result.getQuantity()));
        assertEquals(0, price.setScale(PRICE_SCALE, RoundingMode.HALF_UP).compareTo(result.getPricePerUnit()));
        assertEquals(0, expectedCost.compareTo(result.getTotalTransactionValue()));

        verify(accountDao).updateBalance(eq(testAccount.getAccountId()), eq(testAccount.getBalance().subtract(expectedCost)));
        verify(portfolioAssetDao).addAsset(any(PortfolioAsset.class));
        verify(transactionDao).logTransaction(any(Transaction.class));
    }

    @Test
    void buyCrypto_success_existingAsset() {
        BigDecimal quantityToBuy = new BigDecimal("0.05");
        BigDecimal currentPrice = new BigDecimal("52000.00");
        BigDecimal expectedCost = quantityToBuy.multiply(currentPrice).setScale(FIAT_SCALE, RoundingMode.HALF_UP);

        PortfolioAsset existingAsset = new PortfolioAsset();
        existingAsset.setAssetId(10);
        existingAsset.setAccountId(testAccount.getAccountId());
        existingAsset.setAssetSymbol(ASSET_SYMBOL_BTC);
        existingAsset.setQuantity(new BigDecimal("0.1"));
        existingAsset.setAveragePurchasePrice(new BigDecimal("50000.00"));

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(krakenDataService.getPriceForSymbol(ASSET_SYMBOL_BTC)).thenReturn(currentPrice);
        when(portfolioAssetDao.findByAccountIdAndAssetSymbol(testAccount.getAccountId(), ASSET_SYMBOL_BTC)).thenReturn(Optional.of(existingAsset));
        when(transactionDao.logTransaction(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = tradingService.buyCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToBuy);

        assertNotNull(result);
        BigDecimal newTotalQuantity = existingAsset.getQuantity().add(quantityToBuy);
        BigDecimal oldTotalValue = existingAsset.getQuantity().multiply(existingAsset.getAveragePurchasePrice());
        BigDecimal newPurchaseValue = quantityToBuy.multiply(currentPrice);
        BigDecimal expectedNewAveragePrice = oldTotalValue.add(newPurchaseValue)
                .divide(newTotalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);


        verify(accountDao).updateBalance(eq(testAccount.getAccountId()), eq(testAccount.getBalance().subtract(expectedCost)));
        verify(portfolioAssetDao).updateAsset(eq(existingAsset.getAssetId()),
                eq(newTotalQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN)),
                eq(expectedNewAveragePrice));
        verify(transactionDao).logTransaction(any(Transaction.class));
    }


    @Test
    void buyCrypto_insufficientFunds() {
        BigDecimal quantityToBuy = new BigDecimal("1.0"); // Cost will be 50000
        BigDecimal price = new BigDecimal("50000.00");
        testAccount.setBalance(new BigDecimal("4000.00")); // Not enough for 50000

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(krakenDataService.getPriceForSymbol(ASSET_SYMBOL_BTC)).thenReturn(price);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tradingService.buyCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToBuy);
        });
        assertTrue(exception.getMessage().contains("Insufficient funds"));

        verify(accountDao, never()).updateBalance(anyInt(), any(BigDecimal.class));
        verify(portfolioAssetDao, never()).addAsset(any(PortfolioAsset.class));
        verify(transactionDao, never()).logTransaction(any(Transaction.class));
    }

    @Test
    void buyCrypto_priceNotAvailable() {
        BigDecimal quantityToBuy = new BigDecimal("0.1");
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(krakenDataService.getPriceForSymbol(ASSET_SYMBOL_BTC)).thenReturn(null); // Price unavailable

        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            tradingService.buyCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToBuy);
        });
        assertTrue(exception.getMessage().contains("Price for " + ASSET_SYMBOL_BTC + " is not currently available."));
    }

    @Test
    void buyCrypto_invalidQuantity_zero() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tradingService.buyCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, BigDecimal.ZERO);
        });
        assertTrue(exception.getMessage().contains("Quantity to buy must be positive."));
    }

    @Test
    void buyCrypto_invalidQuantity_negative() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tradingService.buyCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, new BigDecimal("-0.1"));
        });
        assertTrue(exception.getMessage().contains("Quantity to buy must be positive."));
    }

    //sellCrypto Tests
    @Test
    void sellCrypto_success_removesAssetIfZeroQuantity() {
        BigDecimal quantityToSell = new BigDecimal("0.1");
        BigDecimal price = new BigDecimal("55000.00");
        BigDecimal expectedProceeds = quantityToSell.multiply(price).setScale(FIAT_SCALE, RoundingMode.HALF_UP);

        PortfolioAsset assetToSell = new PortfolioAsset();
        assetToSell.setAssetId(10);
        assetToSell.setAccountId(testAccount.getAccountId());
        assetToSell.setAssetSymbol(ASSET_SYMBOL_BTC);
        assetToSell.setQuantity(new BigDecimal("0.1")); // Selling all of it
        assetToSell.setAveragePurchasePrice(new BigDecimal("50000.00"));

        BigDecimal expectedProfitLoss = price.subtract(assetToSell.getAveragePurchasePrice())
                .multiply(quantityToSell)
                .setScale(FIAT_SCALE, RoundingMode.HALF_UP);

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(portfolioAssetDao.findByAccountIdAndAssetSymbol(testAccount.getAccountId(), ASSET_SYMBOL_BTC)).thenReturn(Optional.of(assetToSell));
        when(krakenDataService.getPriceForSymbol(ASSET_SYMBOL_BTC)).thenReturn(price);
        when(transactionDao.logTransaction(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = tradingService.sellCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToSell);

        assertNotNull(result);
        assertEquals(TransactionType.SELL, result.getTransactionType());
        assertEquals(0, expectedProceeds.compareTo(result.getTotalTransactionValue()));
        assertEquals(0, expectedProfitLoss.compareTo(result.getRealizedProfitLoss()));


        verify(accountDao).updateBalance(eq(testAccount.getAccountId()), eq(testAccount.getBalance().add(expectedProceeds)));
        verify(portfolioAssetDao).deleteAsset(eq(assetToSell.getAssetId())); // Asset should be deleted
        verify(portfolioAssetDao, never()).updateAsset(anyInt(), any(BigDecimal.class), any(BigDecimal.class));
        verify(transactionDao).logTransaction(any(Transaction.class));
    }

    @Test
    void sellCrypto_success_updatesAssetQuantity() {
        BigDecimal quantityToSell = new BigDecimal("0.05");
        BigDecimal price = new BigDecimal("55000.00");
        BigDecimal expectedProceeds = quantityToSell.multiply(price).setScale(FIAT_SCALE, RoundingMode.HALF_UP);

        PortfolioAsset assetToSell = new PortfolioAsset();
        assetToSell.setAssetId(10);
        assetToSell.setAccountId(testAccount.getAccountId());
        assetToSell.setAssetSymbol(ASSET_SYMBOL_BTC);
        assetToSell.setQuantity(new BigDecimal("0.1"));
        assetToSell.setAveragePurchasePrice(new BigDecimal("50000.00"));

        BigDecimal remainingQuantity = assetToSell.getQuantity().subtract(quantityToSell);

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(portfolioAssetDao.findByAccountIdAndAssetSymbol(testAccount.getAccountId(), ASSET_SYMBOL_BTC)).thenReturn(Optional.of(assetToSell));
        when(krakenDataService.getPriceForSymbol(ASSET_SYMBOL_BTC)).thenReturn(price);
        when(transactionDao.logTransaction(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = tradingService.sellCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToSell);

        assertNotNull(result);
        verify(accountDao).updateBalance(eq(testAccount.getAccountId()), eq(testAccount.getBalance().add(expectedProceeds)));
        verify(portfolioAssetDao).updateAsset(eq(assetToSell.getAssetId()), eq(remainingQuantity.setScale(CRYPTO_QUANTITY_SCALE, RoundingMode.DOWN)), eq(assetToSell.getAveragePurchasePrice()));
        verify(portfolioAssetDao, never()).deleteAsset(anyInt());
        verify(transactionDao).logTransaction(any(Transaction.class));
    }


    @Test
    void sellCrypto_insufficientAssetQuantity() {
        BigDecimal quantityToSell = new BigDecimal("0.2"); // Has only 0.1
        PortfolioAsset assetToSell = new PortfolioAsset();
        assetToSell.setQuantity(new BigDecimal("0.1"));
        assetToSell.setAssetSymbol(ASSET_SYMBOL_BTC);

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(portfolioAssetDao.findByAccountIdAndAssetSymbol(testAccount.getAccountId(), ASSET_SYMBOL_BTC)).thenReturn(Optional.of(assetToSell));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tradingService.sellCrypto(USER_IDENTIFIER, ASSET_SYMBOL_BTC, quantityToSell);
        });
        assertTrue(exception.getMessage().contains("Insufficient quantity"));
    }

    @Test
    void sellCrypto_assetNotInPortfolio() {
        BigDecimal quantityToSell = new BigDecimal("0.1");
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(portfolioAssetDao.findByAccountIdAndAssetSymbol(testAccount.getAccountId(), ASSET_SYMBOL_ETH)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            tradingService.sellCrypto(USER_IDENTIFIER, ASSET_SYMBOL_ETH, quantityToSell);
        });
        assertTrue(exception.getMessage().contains("not found in your portfolio"));
    }


    //resetAccount Tests
    @Test
    void resetAccount_success() {
        BigDecimal initialBalance = new BigDecimal("10000.00");
        testAccount.setBalance(new BigDecimal("500.00")); // Current balance is different

        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(accountDao.getInitialBalance(testAccount.getAccountId())).thenReturn(Optional.of(initialBalance));
        // For findById after reset
        Account resetAccountState = new Account();
        resetAccountState.setAccountId(testAccount.getAccountId());
        resetAccountState.setUserIdentifier(USER_IDENTIFIER);
        resetAccountState.setBalance(initialBalance);
        resetAccountState.setInitialBalance(initialBalance);
        when(accountDao.findById(testAccount.getAccountId())).thenReturn(Optional.of(resetAccountState));


        Account result = tradingService.resetAccount(USER_IDENTIFIER);

        assertNotNull(result);
        assertEquals(0, initialBalance.compareTo(result.getBalance()));
        verify(accountDao).updateBalance(eq(testAccount.getAccountId()), eq(initialBalance));
        verify(portfolioAssetDao).deleteAllAssetsByAccountId(eq(testAccount.getAccountId()));
    }

    @Test
    void resetAccount_initialBalanceNotFound() {
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        when(accountDao.getInitialBalance(testAccount.getAccountId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            tradingService.resetAccount(USER_IDENTIFIER);
        });
    }

    //getPortfolio, getTransactionHistory, getAccountDetails Tests
    @Test
    void getAccountDetails_success() {
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        Account result = tradingService.getAccountDetails(USER_IDENTIFIER);
        assertNotNull(result);
        assertEquals(USER_IDENTIFIER, result.getUserIdentifier());
    }

    @Test
    void getAccountDetails_notFound() {
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> tradingService.getAccountDetails(USER_IDENTIFIER));
    }

    @Test
    void getPortfolio_success() {
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        List<PortfolioAsset> mockPortfolio = Collections.singletonList(new PortfolioAsset());
        when(portfolioAssetDao.findByAccountId(testAccount.getAccountId())).thenReturn(mockPortfolio);

        List<PortfolioAsset> result = tradingService.getPortfolio(USER_IDENTIFIER);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getTransactionHistory_success() {
        when(accountDao.findByUserIdentifier(USER_IDENTIFIER)).thenReturn(Optional.of(testAccount));
        List<Transaction> mockTransactions = Collections.singletonList(new Transaction());
        when(transactionDao.findByAccountId(testAccount.getAccountId())).thenReturn(mockTransactions);

        List<Transaction> result = tradingService.getTransactionHistory(USER_IDENTIFIER);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

}

