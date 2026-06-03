package io.github.myh9410.mq.consumer.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.consumer.MqEventConverter;
import io.github.myh9410.mq.message.Message;

@Component
public class MessageEventConverter implements MqEventConverter<Message> {

    private static final Logger log = LoggerFactory.getLogger(MessageEventConverter.class);

    @Override
    public Class<Message> eventType() {
        return Message.class;
    }

    @Override
    public void handle(Message event) {
        log.info("MessageEventConverter handled: id={} content={}", event.id(), event.content());
    }
}
