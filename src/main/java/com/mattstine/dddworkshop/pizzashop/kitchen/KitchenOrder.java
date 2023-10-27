package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.List;
import java.util.function.BiFunction;

@Value
public final class KitchenOrder implements Aggregate {
    public static final String KITCHEN_ORDERS = "kitchen_orders";
    KitchenOrderRef ref;
    OnlineOrderRef onlineOrderRef;
    List<Pizza> pizzas;
    EventLog $eventLog;
    @NonFinal
    State state;

    @Builder
    private KitchenOrder(@NonNull KitchenOrderRef ref, @NonNull OnlineOrderRef onlineOrderRef, @Singular List<Pizza> pizzas, @NonNull EventLog eventLog) {
        this.ref = ref;
        this.onlineOrderRef = onlineOrderRef;
        this.pizzas = pizzas;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private KitchenOrder() {
        this.ref = null;
        this.onlineOrderRef = null;
        this.pizzas = null;
        this.$eventLog = null;
    }

    public boolean isNew() {
        return this.state==State.NEW;
    }

    void startPrep() {
        if (!isNew())
            throw new IllegalStateException("Cannot start prep on a KitchenOrder that is not NEW");
        this.state= State.PREPPING;
        $eventLog.publish(new Topic(KITCHEN_ORDERS), new KitchenOrderPrepStartedEvent(ref));
    }

    boolean isPrepping() {
        return this.state==State.PREPPING;

    }

    void startBake() {
        if (!isPrepping())
            throw new IllegalStateException("Cannot start bake on a KitchenOrder that is not PREPPING");
        this.state= State.BAKING;
        $eventLog.publish(new Topic(KITCHEN_ORDERS), new KitchenOrderBakeStartedEvent(ref));
    }

    boolean isBaking() {
        return this.state==State.BAKING;
    }

    void startAssembly() {
        if (!isBaking()){
            throw new IllegalStateException("Cannot start assembly on a KitchenOrder that is not BAKING");
        }
        this.state= State.ASSEMBLING;
        $eventLog.publish(new Topic(KITCHEN_ORDERS), new KitchenOrderAssemblyStartedEvent(ref));
    }

    boolean hasStartedAssembly() {
        return this.state==State.ASSEMBLING;
    }

    void finishAssembly() {
        if (!hasStartedAssembly())
            throw new IllegalStateException("Cannot finish assembly on a KitchenOrder that has not started assembly");
        this.state= State.ASSEMBLED;
        $eventLog.publish(new Topic(KITCHEN_ORDERS), new KitchenOrderAssemblyFinishedEvent(ref));
    }

    boolean hasFinishedAssembly() {
        return this.state==State.ASSEMBLED;
    }

    @Override
    public KitchenOrder identity() {
        return null;
    }

    @Override
    public BiFunction<KitchenOrder, KitchenOrderEvent, KitchenOrder> accumulatorFunction() {
        return new Accumulator();
    }

    @Override
    public OrderState state() {
        return new OrderState(ref, onlineOrderRef, pizzas);
    }

    enum State {
        NEW,
        PREPPING,
        BAKING,
        ASSEMBLING,
        ASSEMBLED
    }

    private static class Accumulator implements BiFunction<KitchenOrder, KitchenOrderEvent, KitchenOrder> {

        @Override
        public KitchenOrder apply(KitchenOrder kitchenOrder, KitchenOrderEvent kitchenOrderEvent) {
            return null;
        }

    }

    /*
     * Pizza Value Object for OnlineOrder Details Only
     */
    @Value
    public static final class Pizza {
        Size size;

        @Builder
        private Pizza(@NonNull Size size) {
            this.size = size;
        }

        public enum Size {
            SMALL, MEDIUM, LARGE
        }
    }

    @Value
    static class OrderState implements AggregateState {
        KitchenOrderRef kitchenOrderRef;
        OnlineOrderRef onlineOrderRef;
        List<Pizza> pizzas;
    }
}
