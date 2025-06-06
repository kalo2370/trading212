package com.cryptosim.trading212.controllers;

import com.cryptosim.trading212.services.KrakenDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST Controller to provide cryptocurrency price information.
 */
@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);
    private final KrakenDataServiceImpl krakenDataServiceImpl;

    @Autowired
    public PriceController(KrakenDataServiceImpl krakenDataServiceImpl) {
        this.krakenDataServiceImpl = krakenDataServiceImpl;
    }

    /**
     * Endpoint to get the latest prices for all subscribed cryptocurrencies.
     *
     * @return A map of asset symbols to their latest prices.
     */
    @GetMapping
    public ResponseEntity<Map<String, BigDecimal>> getAllPrices() {
        try {
            Map<String, BigDecimal> prices = krakenDataServiceImpl.getLatestPrices();
            if (prices.isEmpty()) {
                logger.warn("Price map is empty. KrakenDataService might not have data yet or no symbols are subscribed.");
                // Depending on requirements, could return 204 No Content or an empty map with 200 OK.
                // Returning 200 with an empty map is often more client-friendly.
            }
            return ResponseEntity.ok(prices);
        } catch (Exception e) {
            logger.error("Error retrieving all prices", e);
            // Consider a more specific error response DTO if needed
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint to get the latest price for a specific cryptocurrency symbol.
     * The asset symbol (e.g., "XBT/USD"). Note: URL encoding might be needed for '/' if passed as path variable directly.
     * A better approach for symbols with '/' is to use request parameters or encode the symbol.
     * For simplicity, we'll assume the client handles encoding or the symbol doesn't contain problematic chars.
     * Let's adjust to take the full symbol as a path variable.
     * @return The latest price for the given symbol, or 404 if not found.
     */
    @GetMapping("/{symbol1}/{symbol2}") // e.g., /api/prices/XBT/USD
    public ResponseEntity<BigDecimal> getPriceForSymbol(@PathVariable String symbol1, @PathVariable String symbol2) {
        String fullSymbol = symbol1 + "/" + symbol2;
        try {
            BigDecimal price = krakenDataServiceImpl.getPriceForSymbol(fullSymbol);
            if (price != null) {
                return ResponseEntity.ok(price);
            } else {
                logger.warn("Price not found for symbol: {}", fullSymbol);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving price for symbol: {}", fullSymbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
