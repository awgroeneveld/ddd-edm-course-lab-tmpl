package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.adapters.InProcessEventSourcedRepository;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;

import java.util.HashMap;
import java.util.Map;

final class InProcessEventSourcedKitchenOrderRepository extends InProcessEventSourcedRepository<KitchenOrderRef, KitchenOrder, KitchenOrder.OrderState, KitchenOrderEvent, KitchenOrderAddedEvent> implements KitchenOrderRepository {

    Map<OnlineOrderRef, KitchenOrderRef> kitchenRefByOnlineOrderRef=new HashMap<>();

    InProcessEventSourcedKitchenOrderRepository(EventLog eventLog, Topic topic) {
        super(eventLog,
                KitchenOrderRef.class,
                KitchenOrder.class,
                KitchenOrder.OrderState.class,
                KitchenOrderAddedEvent.class,
                topic);
        eventLog.subscribe(topic, e -> {
            if (e instanceof KitchenOrderAddedEvent) {
                KitchenOrderAddedEvent koae = (KitchenOrderAddedEvent) e;
                kitchenRefByOnlineOrderRef.put(koae.getState().getOnlineOrderRef(), koae.getRef());
            }
        });
    }

    @Override
    public KitchenOrder findByOnlineOrderRef(OnlineOrderRef onlineOrderRef) {
        return findByRef(kitchenRefByOnlineOrderRef.get(onlineOrderRef));
    }
}
