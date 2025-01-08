package com.stockexchange.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Stock {
    private final String symbol;
    private final String companyName;
    private final BigDecimal price;

    public Stock(String symbol, String companyName, BigDecimal price) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public BigDecimal getPrice() { return price; }
}
