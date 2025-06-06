package com.cryptosim.trading212.controllers;

import com.cryptosim.trading212.dtos.TradeRequest;
import com.cryptosim.trading212.models.Transaction;
import com.cryptosim.trading212.services.TradingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * REST Controller for handling buy and sell trading operations.
 */
@RestController
@RequestMapping("/api/trade")
public class TradingController {

    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);

    private final TradingServiceImpl tradingServiceImpl;

    @Autowired
    public TradingController(TradingServiceImpl tradingServiceImpl) {
        this.tradingServiceImpl = tradingServiceImpl;
    }

    /**
     * Endpoint to buy cryptocurrency.
     * Expects a JSON body with userIdentifier, assetSymbol, and quantity.
     * @param tradeRequest The trade request details.
     * @return ResponseEntity with the created Transaction or an error status.
     */
    @PostMapping("/buy")
    public ResponseEntity<?> buyCrypto(@RequestBody TradeRequest tradeRequest) {
        try {
            logger.info("Received buy request: {}", tradeRequest);
            if (tradeRequest.getUserIdentifier() == null || tradeRequest.getAssetSymbol() == null || tradeRequest.getQuantity() == null) {
                return ResponseEntity.badRequest().body("Missing required fields in trade request (userIdentifier, assetSymbol, quantity).");
            }
            Transaction transaction = tradingServiceImpl.buyCrypto(
                    tradeRequest.getUserIdentifier(),
                    tradeRequest.getAssetSymbol(),
                    tradeRequest.getQuantity()
            );
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException e) {
            logger.warn("Buy request failed due to invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            logger.warn("Buy request failed because an entity was not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during buy operation for request {}: ", tradeRequest, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during the buy operation.");
        }
    }

    /**
     * Endpoint to sell cryptocurrency.
     * Expects a JSON body with userIdentifier, assetSymbol, and quantity.
     * @param tradeRequest The trade request details.
     * @return ResponseEntity with the created Transaction or an error status.
     */
    @PostMapping("/sell")
    public ResponseEntity<?> sellCrypto(@RequestBody TradeRequest tradeRequest) {
        try {
            logger.info("Received sell request: {}", tradeRequest);
            if (tradeRequest.getUserIdentifier() == null || tradeRequest.getAssetSymbol() == null || tradeRequest.getQuantity() == null) {
                return ResponseEntity.badRequest().body("Missing required fields in trade request (userIdentifier, assetSymbol, quantity).");
            }
            Transaction transaction = tradingServiceImpl.sellCrypto(
                    tradeRequest.getUserIdentifier(),
                    tradeRequest.getAssetSymbol(),
                    tradeRequest.getQuantity()
            );
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException e) {
            logger.warn("Sell request failed due to invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            logger.warn("Sell request failed because an entity was not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during sell operation for request {}: ", tradeRequest, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during the sell operation.");
        }
    }
}
