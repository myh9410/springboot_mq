package io.github.myh9410.mq.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    public static final String TOPIC = "messages";

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String payload) {
        log.info("Sending: topic={} payload={}", TOPIC, payload);
        kafkaTemplate.send(TOPIC, payload)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    var meta = result.getRecordMetadata();
                    log.info("Sent ok: topic={} partition={} offset={}",
                        meta.topic(), meta.partition(), meta.offset());
                } else {
                    log.error("Send failed: topic={}", TOPIC, ex);
                }
            });
    }
}