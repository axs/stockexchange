package com.stockexchange.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final String stockSymbol;
    private final int quantity;
    private final BigDecimal price;
    private final Instant timestamp;
    private final OrderType type;

    public enum OrderType {
        BUY, SELL, MATCH
    }

    public Order(String stockSymbol, int quantity, BigDecimal price, OrderType type) {
        this.id = UUID.randomUUID();
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = Instant.now();
        this.type = type;
    }

    // Getters
    public UUID getId() { return id; }
    public String getStockSymbol() { return stockSymbol; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public Instant getTimestamp() { return timestamp; }
    public OrderType getType() { return type; }
}
