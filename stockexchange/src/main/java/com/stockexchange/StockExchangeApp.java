package com.stockexchange;

import akka.actor.typed.ActorSystem;
import com.stockexchange.actors.StockExchangeActor;
import com.stockexchange.model.Stock;
import com.stockexchange.model.Order;

import com.stockexchange.messages.LookupBusImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import com.stockexchange.messages.Message.Command;

public class StockExchangeApp {


    public static void waitForKey() {

        try {
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final LookupBusImpl lookupBus = new LookupBusImpl();

        final Random random = new Random();

        // Create the actor system
        ActorSystem<Command> stockExchange = ActorSystem.create(StockExchangeActor.create(lookupBus), "StockExchange");
/*
        stockExchange.tell(new StockExchangeActor.RegisterStock(
                new Stock("AAPL", "Apple Inc.", BigDecimal.valueOf(150.50))
        ));
        stockExchange.tell(new StockExchangeActor.RegisterStock(
                new Stock("GOOGL", "Alphabet Inc.", BigDecimal.valueOf(120.75))
        ));
        stockExchange.tell(new StockExchangeActor.RegisterStock(
                new Stock("IBM", "IBM Inc.", BigDecimal.valueOf(90.75))
        ));

        // Register stocks with market making
        stockExchange.tell(new StockExchangeActor.RegisterMarketMaker(
                "AAPL", BigDecimal.valueOf(150.50)
        ));

        stockExchange.tell(new StockExchangeActor.RegisterMarketMaker(
                "GOOGL", BigDecimal.valueOf(120.75)
        ));
        stockExchange.tell(new StockExchangeActor.RegisterMarketMaker(
                "IBM", BigDecimal.valueOf(90.77)
        ));
*/
        List<String> symbols = Arrays.asList("IBM","GOOGL","AAPL","MSFT", "SPY");
        for(String symbol: symbols) {
            BigDecimal d = BigDecimal.valueOf(100*random.nextDouble()).setScale(2, RoundingMode.HALF_UP);

            stockExchange.tell(new StockExchangeActor.RegisterStock(
                    new Stock(symbol, "Apple Inc.", d)
            ));

            // Register stocks with market making
            stockExchange.tell(new StockExchangeActor.RegisterMarketMaker(symbol, d));

            stockExchange.scheduler().scheduleWithFixedDelay(
                    Duration.ofSeconds(6),
                    Duration.ofSeconds(2),
                    (Runnable) () ->{stockExchange.tell(new StockExchangeActor.ExecuteTrade(
                            new Order(symbol,
                                    100,
                                    BigDecimal.valueOf(100*random.nextDouble()).setScale(2, RoundingMode.HALF_UP),
                                    Order.OrderType.BUY)
                    ));},
                    stockExchange.executionContext()
            );
        }

        waitForKey();

        // Shutdown
        stockExchange.terminate();
    }
}
