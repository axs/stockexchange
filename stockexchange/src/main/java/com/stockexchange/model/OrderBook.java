// Order Book Implementation
package com.stockexchange.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import com.stockexchange.model.Order;


public class OrderBook {
    private final String stockSymbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;

    public OrderBook(String stockSymbol) {
        this.stockSymbol = stockSymbol;
        // Buy orders sorted in descending order (highest price first)
        this.buyOrders = new PriorityQueue<>((a, b) -> b.getPrice().compareTo(a.getPrice()));
        // Sell orders sorted in ascending order (lowest price first)
        this.sellOrders = new PriorityQueue<>(Comparator.comparing(Order::getPrice));
    }

    public void addOrder(Order order) {
        System.out.println("Adding order ");
        if (order.getType() == Order.OrderType.BUY) {
            buyOrders.offer(order);
        } else {
            sellOrders.offer(order);
        }
    }

    public Optional<com.stockexchange.model.Order> matchOrders() {
        System.out.println("Match orders ");
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
            return Optional.empty();
        }

        Order bestBuy = buyOrders.peek();
        Order bestSell = sellOrders.peek();

        if (bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0) {
            // Match found
            int matchedQuantity = Math.min(bestBuy.getQuantity(), bestSell.getQuantity());
            BigDecimal tradePrice = bestSell.getPrice(); // Use sell order price

            // Remove or update orders
            updateOrderAfterTrade(buyOrders, bestBuy, matchedQuantity);
            updateOrderAfterTrade(sellOrders, bestSell, matchedQuantity);

            return Optional.of(new com.stockexchange.model.Order(
                    this.stockSymbol,
                    matchedQuantity,
                    tradePrice,
                    com.stockexchange.model.Order.OrderType.MATCH
            ));
        }

        return Optional.empty();
    }

    private void updateOrderAfterTrade(PriorityQueue<Order> orders, Order order, int tradedQuantity) {
        orders.remove(order);
        if (order.getQuantity() > tradedQuantity) {
            Order remainingOrder = new Order(
                    order.getStockSymbol(),
                    order.getQuantity() - tradedQuantity,
                    order.getPrice(),
                    order.getType()
            );
            orders.offer(remainingOrder);
        }
    }
}
