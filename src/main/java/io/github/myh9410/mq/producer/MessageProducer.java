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
        // Send 결과 (Sent ok / Send failed) 로깅은 KafkaTemplate에 부착된 ProducerListener가 담당.
        kafkaTemplate.send(Topics.MESSAGES, message.id(), message);
    }
}
