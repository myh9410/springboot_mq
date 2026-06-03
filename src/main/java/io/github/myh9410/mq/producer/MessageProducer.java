package io.github.myh9410.mq.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.message.Message;
import io.github.myh9410.mq.message.Topics;

@Component
public class MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(Message message) {
        log.info("Sending: topic={} key={} payload={}", Topics.MESSAGES, message.id(), message);
        // message.id를 partition key로 사용 — 같은 id는 항상 같은 partition으로 라우팅됨
        kafkaTemplate.send(Topics.MESSAGES, message.id(), message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    var meta = result.getRecordMetadata();
                    log.info("Sent ok: topic={} partition={} offset={}",
                        meta.topic(), meta.partition(), meta.offset());
                } else {
                    log.error("Send failed: topic={}", Topics.MESSAGES, ex);
                }
            });
    }
}
