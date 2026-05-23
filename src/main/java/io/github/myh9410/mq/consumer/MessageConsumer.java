package io.github.myh9410.mq.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {

    private static final String TOPIC = "messages";

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = TOPIC)
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Received: topic={} partition={} offset={} key={} payload={}",
            record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }
}