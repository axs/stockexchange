package com.stockexchange.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;
import com.stockexchange.messages.EventTopic;
import com.stockexchange.messages.LookupBusImpl;
import com.stockexchange.messages.Message.Command;
import com.stockexchange.messages.MsgEnvelope;
import com.stockexchange.model.OrderBook;
import akka.actor.typed.ActorRef;
import com.stockexchange.model.Stock;
import com.stockexchange.model.Order;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StockExchangeActor extends AbstractBehavior<Command> {

    //public interface Command {}
    // Add new commands
    public static class RegisterMarketMaker implements Command {
        public final String stockSymbol;
        public final BigDecimal initialPrice;
        public RegisterMarketMaker(String stockSymbol, BigDecimal initialPrice) {
            this.stockSymbol = stockSymbol;
            this.initialPrice = initialPrice;
        }
    }

    public static class GetStockPrice implements Command {
        public final String stockSymbol;
        public final ActorRef<BigDecimal> replyTo;
        public GetStockPrice(String stockSymbol, ActorRef<BigDecimal> replyTo) {
            this.stockSymbol = stockSymbol;
            this.replyTo = replyTo;
        }
    }

    public static class RegisterStock implements Command {
        public final Stock stock;
        public RegisterStock(Stock stock) {
            this.stock = stock;
        }
    }

    public static class ExecuteTrade implements Command {
        public final Order trade;
        public ExecuteTrade(Order trade) {
            this.trade = trade;
        }
    }

    private StockExchangeActor(ActorContext<Command> context, LookupBusImpl lookupBus) {
        super(context);
        this.lookupBus = lookupBus;
        this.lookupBus.subscribe(this.getContext().getSelf(), EventTopic.PSA.getValue());
    }

    // Update internal state
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<String, ActorRef<MarketPriceSimulatorActor.Command>> priceSimulators = new ConcurrentHashMap<>();
    private final Map<String, ActorRef<MarketMakerActor.Command>> marketMakers = new ConcurrentHashMap<>();

    private final Map<String, Stock> registeredStocks = new HashMap<>();
    private final LookupBusImpl lookupBus;

    // Modify create and other methods to support new functionality
    public static Behavior<Command> create(LookupBusImpl lookupBus) {
        return Behaviors.setup(ctx -> new StockExchangeActor(ctx, lookupBus));
    }

    // Add methods to handle new commands
    private Behavior<Command> onRegisterMarketMaker(RegisterMarketMaker command) {
        // Create OrderBook
        OrderBook orderBook = new OrderBook(command.stockSymbol);
        orderBooks.put(command.stockSymbol, orderBook);

        // Create Price Simulator
        ActorRef<MarketPriceSimulatorActor.Command> priceSimulator =
                getContext().spawn(
                        MarketPriceSimulatorActor.create(command.stockSymbol, command.initialPrice),
                        "PriceSimulator-" + command.stockSymbol
                );
        priceSimulators.put(command.stockSymbol, priceSimulator);

        // Create Market Maker
        ActorRef<MarketMakerActor.Command> marketMaker =
                getContext().spawn(
                        MarketMakerActor.create(command.stockSymbol, command.initialPrice, this.lookupBus),
                        "MarketMaker-" + command.stockSymbol
                );
        marketMakers.put(command.stockSymbol, marketMaker);

        return this;
    }

    // Modify createReceive to include new message handling
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RegisterStock.class, this::onRegisterStock)
                .onMessage(RegisterMarketMaker.class, this::onRegisterMarketMaker)
                .onMessage(ExecuteTrade.class, this::onExecuteTrade)
                .onMessage(GetStockPrice.class, this::onGetStockPrice)
                .onMessage(MsgEnvelope.class, this::onMsgEnvelope)
                .build();
    }

    private Behavior<Command> onMsgEnvelope(MsgEnvelope command) {
        getContext().getLog().info("MsgEnvelope executed {}", command.payload.toString());
        return this;
    }


    private Behavior<Command> onRegisterStock(RegisterStock command) {
        registeredStocks.put(command.stock.getSymbol(), command.stock);
        return this;
    }

    private Behavior<Command> onExecuteTrade(ExecuteTrade command) {
        // Basic trade execution logic
        Stock stock = registeredStocks.get(command.trade.getStockSymbol());
        if (stock != null) {
            // In a real system, this would involve more complex trade matching and validation
            getContext().getLog().info("Trade executed: {} {} at {}",
                    command.trade.getType(),
                    command.trade.getStockSymbol(),
                    command.trade.getPrice());

            ActorRef<MarketMakerActor.Command> mm = marketMakers.get(command.trade.getStockSymbol());
            mm.tell(new MarketMakerActor.UpdateMarketConditions(command.trade.getPrice()));
            mm.tell(new MarketMakerActor.PlaceMarketMakingOrders());

            OrderBook ob = orderBooks.get(command.trade.getStockSymbol());
            ob.addOrder(new Order(
                    command.trade.getStockSymbol(),
                    command.trade.getQuantity(),
                    command.trade.getPrice(),
                    command.trade.getType() == Order.OrderType.BUY ? Order.OrderType.BUY : Order.OrderType.SELL
            ));
            ob.matchOrders();
        } else {
            getContext().getLog().info("Trade failed: Stock not registered - {}",
                    command.trade.getStockSymbol());
        }
        return this;
    }

    private Behavior<Command> onGetStockPrice(GetStockPrice command) {
        Stock stock = registeredStocks.get(command.stockSymbol);
        if (stock != null) {
            command.replyTo.tell(stock.getPrice());
        } else {
            getContext().getLog().info("Stock price query failed: Stock not found - {}",
                    command.stockSymbol);
        }
        return this;
    }

    private StockExchangeActor onPreRestart(PreRestart preRestart) {
        System.out.println("Job is about to restart.");
        return this;
    }

    private StockExchangeActor onTerminated(Terminated terminated) {
        System.out.println("Job" + terminated.getRef().path().name() + "stopped.");
        return this;
    }

    private StockExchangeActor onPostStop(PostStop postStop) {
        System.out.println("Job is stopped.");
        return this;
    }
}
