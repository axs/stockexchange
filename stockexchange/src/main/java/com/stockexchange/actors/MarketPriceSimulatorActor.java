package com.stockexchange.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Random;

public class MarketPriceSimulatorActor extends AbstractBehavior<MarketPriceSimulatorActor.Command> {
    // Commands
    public interface Command {}

    public static class SimulatePriceChange implements Command {}

    public static class GetCurrentPrice implements Command {
        public final ActorRef<BigDecimal> replyTo;
        public GetCurrentPrice(ActorRef<BigDecimal> replyTo) {
            this.replyTo = replyTo;
        }
    }

    // Internal state
    private BigDecimal currentPrice;
    private final String stockSymbol;
    private final Random random = new Random();

    public static Behavior<Command> create(String stockSymbol, BigDecimal initialPrice) {
        return Behaviors.setup(context -> new MarketPriceSimulatorActor(context, stockSymbol, initialPrice));
    }

    private MarketPriceSimulatorActor(ActorContext<Command> context, String stockSymbol, BigDecimal initialPrice) {
        super(context);
        this.stockSymbol = stockSymbol;
        this.currentPrice = initialPrice;

        context.getSystem().scheduler().scheduleWithFixedDelay(
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                (Runnable) () ->{context.getSelf().tell(new SimulatePriceChange());},
                context.getExecutionContext()
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SimulatePriceChange.class, this::onSimulatePriceChange)
                .onMessage(GetCurrentPrice.class, this::onGetCurrentPrice)
                .build();
    }

    private Behavior<Command> onSimulatePriceChange(SimulatePriceChange command) {
        // Simulate price volatility
        double changePercent = (random.nextDouble() - 0.5) * 0.1; // +/- 5% volatility
        BigDecimal priceChange = currentPrice.multiply(BigDecimal.valueOf(changePercent))
                .setScale(2, RoundingMode.HALF_UP);

        currentPrice = currentPrice.add(priceChange);

        getContext().getLog().info("Price update for {}: ${}", stockSymbol, currentPrice);
        return this;
    }

    private Behavior<Command> onGetCurrentPrice(GetCurrentPrice command) {
        command.replyTo.tell(currentPrice);
        return this;
    }


    private MarketPriceSimulatorActor onPreRestart(PreRestart preRestart) {
        System.out.println("Job is about to restart.");
        return this;
    }

    private MarketPriceSimulatorActor onTerminated(Terminated terminated) {
        System.out.println("Job" + terminated.getRef().path().name() + "stopped.");
        return this;
    }

    private MarketPriceSimulatorActor onPostStop(PostStop postStop) {
        System.out.println("Job is stopped.");
        return this;
    }
}
