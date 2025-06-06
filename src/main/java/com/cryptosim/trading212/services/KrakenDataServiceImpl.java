package com.cryptosim.trading212.services;

import com.cryptosim.trading212.services.contracts.KrakenDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to connect to Kraken WebSocket API and manage real-time price data.
 */
@Service
public class KrakenDataServiceImpl implements KrakenDataService {

    private static final Logger logger = LoggerFactory.getLogger(KrakenDataServiceImpl.class);
    private static final String KRAKEN_WS_API_URL = "wss://ws.kraken.com/v2";
    private static final List<String> KRAKEN_SYMBOLS_TO_SUBSCRIBE = Arrays.asList(
            "BTC/USD",  // Using BTC/USD directly based on "XBT/USD not supported" error
            "ETH/USD",  // Ethereum
            "USDT/USD", // Tether
            "ADA/USD",  // Cardano
            "SOL/USD",  // Solana
            "XRP/USD",  // Ripple
            "DOT/USD",  // Polkadot
            "DOGE/USD", // Dogecoin
            "LTC/USD",  // Litecoin
            "LINK/USD", // Chainlin
            "AVAX/USD", // Avalanche
            "SHIB/USD", // Shiba Inu
            "TRX/USD",  // Tron
            "USDC/USD", // USD Coin
            "DAI/USD",  // Dai
            "ATOM/USD", // Cosmos
            "UNI/USD",  // Uniswap
            "BCH/USD",  // Bitcoin Cash
            "ALGO/USD", // Algorand
            "XTZ/USD",  // Tezos
            "FIL/USD",  // Filecoin
            "ETC/USD",  // Ethereum Classic
            "XLM/USD"   // Stellar Lumens
    );

    private WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, BigDecimal> latestPrices = new ConcurrentHashMap<>();

    public KrakenDataServiceImpl() {

    }

    @PostConstruct
    private void init() {
        connect();
    }

    @Override
    public void connect() {
        try {
            webSocketClient = new WebSocketClient(new URI(KRAKEN_WS_API_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("Connected to Kraken WebSocket API. Status: {}", handshakedata.getHttpStatusMessage());
                    subscribeToTickers();
                }

                @Override
                public void onMessage(String message) {
                    logger.debug("Received message from Kraken: {}", message);
                    handleIncomingMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.warn("Kraken WebSocket connection closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
                    // Implement reconnection logic if needed
                }

                @Override
                public void onError(Exception ex) {
                    logger.error("Error in Kraken WebSocket connection", ex);
                }
            };
            logger.info("Attempting to connect to Kraken WebSocket API...");
            webSocketClient.connect(); // Asynchronous connect
        } catch (URISyntaxException e) {
            logger.error("Invalid WebSocket URI: {}", KRAKEN_WS_API_URL, e);
        } catch (Exception e) {
            logger.error("Failed to initialize WebSocket connection", e);
        }
    }

    private void subscribeToTickers() {
        try {
            Map<String, Object> params = new ConcurrentHashMap<>();
            params.put("channel", "ticker");
            params.put("symbol", KRAKEN_SYMBOLS_TO_SUBSCRIBE); // Using the revised list directly

            Map<String, Object> subscribeMessage = new ConcurrentHashMap<>();
            subscribeMessage.put("method", "subscribe");
            subscribeMessage.put("params", params);

            String messagePayload = objectMapper.writeValueAsString(subscribeMessage);
            logger.info("Subscribing to tickers: {}", messagePayload);
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(messagePayload);
            } else {
                logger.warn("WebSocket client not open, cannot send subscription message.");
            }
        } catch (JsonProcessingException e) {
            logger.error("Error creating subscription message JSON", e);
        } catch (Exception e) {
            logger.error("Error sending subscription message", e);
        }
    }

    private void handleIncomingMessage(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            if (rootNode.has("method") && "subscribe".equals(rootNode.get("method").asText())) {
                boolean success = rootNode.get("success").asBoolean(false); // Default to false if not present
                String symbol = rootNode.has("symbol") ? rootNode.get("symbol").asText("N/A") : (rootNode.has("pair") ? rootNode.get("pair").asText("N/A") : "N/A_Subscription_Response");

                if (success) {
                    logger.info("Successfully subscribed to channel for symbol: {}", symbol);
                } else {
                    String errorMsg = rootNode.has("error") ? rootNode.get("error").asText("Unknown subscription error") : "Unknown subscription error";
                    logger.error("Failed to subscribe to channel for symbol: {}. Error: {}", symbol, errorMsg);
                }
                return; // Processed subscription response
            }

            // Check for heartbeat messages
            if (rootNode.has("channel") && "heartbeat".equals(rootNode.get("channel").asText())) {
                logger.debug("Received heartbeat from Kraken.");
                return;
            }
            if (rootNode.has("channel") && "ticker".equals(rootNode.get("channel").asText()) && rootNode.has("data") && rootNode.get("data").isArray()) {
                JsonNode dataArray = rootNode.get("data");
                for (JsonNode tickerData : dataArray) {
                    if (tickerData.has("symbol") && tickerData.has("last")) {
                        String symbol = tickerData.get("symbol").asText();
                        String lastPriceStr = tickerData.get("last").asText();
                        try {
                            BigDecimal price = new BigDecimal(lastPriceStr);
                            latestPrices.put(symbol, price);
                            logger.trace("Updated price for {}: {}", symbol, price);
                        } catch (NumberFormatException e) {
                            logger.error("Could not parse price '{}' for symbol {}", lastPriceStr, symbol, e);
                        }
                    }
                }
            } else if (rootNode.has("channel") && "status".equals(rootNode.get("channel").asText())) {
                logger.info("Received status message from Kraken: {}", message);
                // You might want to handle system status updates (online, maintenance, etc.)
            }
            else {
                logger.warn("Received unhandled message type or format from Kraken: {}", message);
            }

        } catch (JsonProcessingException e) {
            logger.error("Error parsing incoming JSON message from Kraken: {}", message, e);
        } catch (Exception e) {
            logger.error("Unexpected error handling incoming message: {}", message, e);
        }
    }

    @Override
    public Map<String, BigDecimal> getLatestPrices() {
        return new ConcurrentHashMap<>(latestPrices);
    }

    @Override
    public BigDecimal getPriceForSymbol(String assetSymbol) {
        return latestPrices.get(assetSymbol);
    }

    @PreDestroy
    public void cleanup() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            logger.info("Closing Kraken WebSocket connection and unsubscribing...");
            try {
                if (!KRAKEN_SYMBOLS_TO_SUBSCRIBE.isEmpty()) {
                    Map<String, Object> params = new ConcurrentHashMap<>();
                    params.put("channel", "ticker");
                    params.put("symbol", KRAKEN_SYMBOLS_TO_SUBSCRIBE);

                    Map<String, Object> unsubscribeMessage = new ConcurrentHashMap<>();
                    unsubscribeMessage.put("method", "unsubscribe");
                    unsubscribeMessage.put("params", params);

                    String messagePayload = objectMapper.writeValueAsString(unsubscribeMessage);
                    logger.info("Unsubscribing from tickers: {}", messagePayload);
                    webSocketClient.send(messagePayload);
                    Thread.sleep(500);
                }
            } catch (JsonProcessingException e) {
                logger.error("Error creating unsubscribe message JSON during shutdown", e);
            } catch (InterruptedException e) {
                logger.warn("Thread interrupted during unsubscribe sleep period.", e);
                Thread.currentThread().interrupt(); // Restore interruption status
            } catch (Exception e) {
                logger.error("Error sending unsubscribe message during shutdown", e);
            } finally {
                webSocketClient.closeConnection(1000, "Application shutting down");
                logger.info("Kraken WebSocket connection closed command sent.");
            }
        } else if (webSocketClient != null) {
            logger.info("Kraken WebSocket client exists but is not open. Closing if possible.");
            webSocketClient.closeConnection(1000, "Application shutting down - client not open");
        }
    }
}
