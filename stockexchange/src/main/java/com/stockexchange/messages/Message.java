package com.stockexchange.messages;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public final class Message {
    public interface Command extends Serializable{}

    public enum TradeType {
        BUY("BUY"), SELL("SELL"), MATCH("MATCH");

        private static final Map<String, TradeType> mValueMap;
        private final String side;

        TradeType(String s) {
            this.side = s;
        }

        public String getValue() {
            return side;
        }

        public TradeType getFlip() {
            return this == TradeType.BUY ? TradeType.SELL : TradeType.BUY;
        }

        static TradeType getInstanceForValue(String inValue) {
            TradeType ot = mValueMap.get(inValue);
            return ot;
        }


        static {
            Map<String, TradeType> table = new HashMap<String, TradeType>();
            for (TradeType ot : values()) {
                table.put(ot.getValue(), ot);
            }
            mValueMap = Collections.unmodifiableMap(table);
        }

    }

    public record Order(int id, TradeType action, BigDecimal price, int quantity, String symbol) implements  Command {
    }

    public record Fill(int id, int order_id, BigDecimal price, int quantity) implements  Command {
    }

    public record Cancel(int id, int order_id) implements Command{
    }


    public record Trade(String id, Instant timestamp, String stockSymbol, int quantity, BigDecimal price, TradeType type) implements Command{

        public Trade(String stockSymbol, int quantity, BigDecimal price, TradeType type) {
            this(UUID.randomUUID().toString(),
                    Instant.now(),
                    stockSymbol,   quantity,   price,   type
            );
        }
    }

}



