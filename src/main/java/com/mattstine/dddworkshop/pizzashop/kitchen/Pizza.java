package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.BiFunction;

@Value
public final class Pizza implements Aggregate {
    public static final String PIZZAS = "pizzas";
    PizzaRef ref;
    KitchenOrderRef kitchenOrderRef;
    Size size;
    EventLog $eventLog;
    @NonFinal
    State state;

    @Builder
    private Pizza(@NonNull PizzaRef ref,
                  @NonNull KitchenOrderRef kitchenOrderRef,
                  @NonNull Size size,
                  @NonNull EventLog eventLog) {
        this.ref = ref;
        this.kitchenOrderRef = kitchenOrderRef;
        this.size = size;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private Pizza() {
        this.ref = null;
        this.kitchenOrderRef = null;
        this.size = null;
        this.$eventLog = null;
    }

    public boolean isNew() {
        return this.state==State.NEW;
    }

    void startPrep() {
        if (!isNew()) {
            throw new IllegalStateException("Cannot start prep on a Pizza that is not NEW");
        }
        state= State.PREPPING;
        $eventLog.publish(new Topic(PIZZAS), new PizzaPrepStartedEvent(ref));
    }

    boolean isPrepping() {
        return this.state == State.PREPPING;
    }

    void finishPrep() {
        if(!isPrepping())
            throw new IllegalStateException("Cannot finish prep on a Pizza that is not PREPPING");
        state=State.PREPPED;
        $eventLog.publish(new Topic(PIZZAS), new PizzaPrepFinishedEvent(ref));
    }

    boolean hasFinishedPrep() {
        return this.state==State.PREPPED;
    }

    void startBake() {
        if (!hasFinishedPrep())
            throw new IllegalStateException("Cannot start bake on a Pizza that is not PREPPED");
        state=State.BAKING;
        $eventLog.publish(new Topic(PIZZAS), new PizzaBakeStartedEvent(ref));
    }

    boolean isBaking() {
        return this.state==State.BAKING;
    }

    void finishBake() {
        if (!isBaking())
            throw new IllegalStateException("Cannot finish bake on a Pizza that is not BAKING");
        this.state=State.BAKED;
        $eventLog.publish(new Topic(PIZZAS), new PizzaBakeFinishedEvent(ref));
    }

    boolean hasFinishedBaking() {
        return this.state==State.BAKED;
    }

    @Override
    public Pizza identity() {
        return Pizza.builder()
                .ref(PizzaRef.IDENTITY)
                .size(Size.IDENTITY)
                .kitchenOrderRef(KitchenOrderRef.IDENTITY)
                .eventLog($eventLog.IDENTITY)
                .build()
                ;
    }

    @Override
    public BiFunction<Pizza, PizzaEvent, Pizza> accumulatorFunction() {
        return new Accumulator();
    }

    @Override
    public PizzaRef getRef() {
        return ref;
    }

    @Override
    public PizzaState state() {
        return new PizzaState(ref, kitchenOrderRef, size);
    }

    enum Size {
        IDENTITY, SMALL, MEDIUM, LARGE
    }

    enum State {
        NEW,
        PREPPING,
        PREPPED,
        BAKING,
        BAKED
    }

    private static class Accumulator implements BiFunction<Pizza, PizzaEvent, Pizza> {

        @Override
        public Pizza apply(Pizza pizza, PizzaEvent pizzaEvent) {
            if (pizzaEvent instanceof PizzaAddedEvent){
                return Pizza.builder()
                        .ref(((PizzaAddedEvent) pizzaEvent).getState().getPizzaRef())
                        .size(((PizzaAddedEvent) pizzaEvent).getState().getSize())
                        .kitchenOrderRef(((PizzaAddedEvent) pizzaEvent).getState().getKitchenOrderRef())
                        .eventLog(pizza.$eventLog)
                        .build();
            }
            if (pizzaEvent instanceof PizzaPrepStartedEvent){
                pizza.state=State.PREPPING;
                return pizza;
            }
            if (pizzaEvent instanceof PizzaPrepFinishedEvent){
                pizza.state=State.PREPPED;
                return pizza;
            }
            if (pizzaEvent instanceof PizzaBakeStartedEvent){
                pizza.state=State.BAKING;
                return pizza;
            }
            if (pizzaEvent instanceof PizzaBakeFinishedEvent) {
                pizza.state = State.BAKED;
                return pizza;
            }
            return null;
        }
    }

    @Value
    static class PizzaState implements AggregateState {
        PizzaRef pizzaRef;
        KitchenOrderRef kitchenOrderRef;
        Size size;
    }
}
