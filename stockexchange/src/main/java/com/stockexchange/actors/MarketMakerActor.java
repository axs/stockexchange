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
import com.stockexchange.messages.MsgEnvelope;
import com.stockexchange.model.OrderBook;
import com.stockexchange.model.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MarketMakerActor extends AbstractBehavior<MarketMakerActor.Command> {
    // Commands
    public interface Command {}

    public static class PlaceMarketMakingOrders implements Command {}

    public static class UpdateMarketConditions implements Command {
        public final BigDecimal currentPrice;
        public UpdateMarketConditions(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }
    }

    // Internal state
    private final String stockSymbol;
    private BigDecimal currentPrice;
    private final Random random = new Random();
    private final LookupBusImpl lookupBus;

    public static Behavior<Command> create(String stockSymbol, BigDecimal initialPrice, LookupBusImpl lookupBus) {
        return Behaviors.setup(context -> new MarketMakerActor(context, stockSymbol, initialPrice, lookupBus));
    }

    private MarketMakerActor(ActorContext<Command> context, String stockSymbol, BigDecimal initialPrice, LookupBusImpl lookupBus) {
        super(context);
        this.stockSymbol = stockSymbol;
        this.currentPrice = initialPrice;
        this.lookupBus = lookupBus;
        this.lookupBus.publish(new MsgEnvelope(EventTopic.PSA.getValue(), new String("blah")));
        this.lookupBus.publish(new MsgEnvelope(EventTopic.FILLS.getValue(),  new String("FILLS")));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateMarketConditions.class, this::onUpdateMarketConditions)
                .onMessage(PlaceMarketMakingOrders.class, this::onPlaceMarketMakingOrders)
                .build();
    }

    private Behavior<Command> onUpdateMarketConditions(UpdateMarketConditions command) {
        this.currentPrice = command.currentPrice;

        getContext().getLog().info(
                "onUpdateMarketConditions {}",
                this.currentPrice
        );

        return this;
    }

    private Behavior<Command> onPlaceMarketMakingOrders(PlaceMarketMakingOrders command) {
        // Create bid and ask orders around the current price
        BigDecimal spreadPercent = BigDecimal.valueOf(0.01); // 1% spread

        // Buy order slightly below current price
        BigDecimal bidPrice = currentPrice.multiply(BigDecimal.ONE.subtract(spreadPercent))
                .setScale(2, RoundingMode.HALF_UP);

        // Sell order slightly above current price
        BigDecimal askPrice = currentPrice.multiply(BigDecimal.ONE.add(spreadPercent))
                .setScale(2, RoundingMode.HALF_UP);

        // Randomize order quantities
        int bidQuantity = random.nextInt(50) + 10; // 10-60 shares
        int askQuantity = random.nextInt(50) + 10; // 10-60 shares

        Order buyOrder = new Order(
                this.stockSymbol,
                bidQuantity,
                bidPrice,
                Order.OrderType.BUY
        );

        Order sellOrder = new Order(
                this.stockSymbol,
                askQuantity,
                askPrice,
                Order.OrderType.SELL
        );

        getContext().getLog().info(
                "Market Maker Orders: Buy {} @ ${}, Sell {} @ ${}:  ${}",
                bidQuantity, bidPrice, askQuantity, askPrice, currentPrice
        );

        return this;
    }


    private MarketMakerActor onPreRestart(PreRestart preRestart) {
        System.out.println("Job is about to restart.");
        return this;
    }

    private MarketMakerActor onTerminated(Terminated terminated) {
        System.out.println("Job" + terminated.getRef().path().name() + "stopped.");
        return this;
    }

    private MarketMakerActor onPostStop(PostStop postStop) {
        System.out.println("Job is stopped.");
        return this;
    }
}
