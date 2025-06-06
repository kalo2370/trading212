package com.cryptosim.trading212.services.contracts;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface for the Kraken Data Service.
 * Defines the contract for fetching and providing real-time cryptocurrency price data.
 */
public interface KrakenDataService {

    /**
     * Retrieves the latest cached prices for all subscribed cryptocurrencies.
     * The keys of the map are the asset symbols (e.g., "BTC/USD"),
     * and the values are their latest prices as BigDecimal.
     *
     * @return A new map containing the latest prices.
     * Returns an empty map if no prices are available or an error occurred.
     * The returned map is a copy to prevent external modification of the internal cache.
     */
    Map<String, BigDecimal> getLatestPrices();

    /**
     * Retrieves the latest cached price for a specific cryptocurrency symbol.
     *
     * @param assetSymbol The symbol of the asset (e.g., "BTC/USD") for which to retrieve the price.
     * @return The latest price as BigDecimal if the symbol is found and has a cached price;
     * otherwise, returns null.
     */
    BigDecimal getPriceForSymbol(String assetSymbol);

    /**
     * Initiates the connection to the WebSocket API if not already connected or if a reconnect is needed.
     * While typically managed by @PostConstruct in the implementation, exposing this allows for
     * programmatic control if necessary (e.g., manual reconnection attempts by another service).
     */
    void connect();

}
