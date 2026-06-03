package io.github.myh9410.mq.consumer.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.consumer.MqEventConverter;
import io.github.myh9410.mq.message.OrderEvent;

@Component
public class OrderEventConverter implements MqEventConverter<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConverter.class);

    @Override
    public Class<OrderEvent> eventType() {
        return OrderEvent.class;
    }

    @Override
    public void handle(OrderEvent event) {
        log.info("OrderEventConverter handled: id={} item={} quantity={}",
            event.id(), event.item(), event.quantity());
    }
}
