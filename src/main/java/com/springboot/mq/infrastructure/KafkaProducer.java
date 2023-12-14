package com.springboot.mq.infrastructure;

import com.springboot.mq.domains.dto.DefaultBoardCreateEvent;
import com.springboot.mq.domains.dto.TestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Slf4j
@Service
public class KafkaProducer {
    private static final String TOPIC = "create";
    private final KafkaTemplate<String, String> stringTemplate;
    private final KafkaTemplate<String, Object> jsonTemplate;
    private final KafkaTemplate<String, DefaultBoardCreateEvent> boardTemplate;

    public KafkaProducer(
        @Qualifier(value = "stringTemplate") KafkaTemplate<String, String> kafkaStringTemplate,
        @Qualifier(value = "jsonTemplate") KafkaTemplate<String, Object> kafkaJsonTemplate,
        @Qualifier(value = "boardCreateEventTemplate") KafkaTemplate<String, DefaultBoardCreateEvent> kafkaBoardTemplate
    ) {
        this.stringTemplate = kafkaStringTemplate;
        this.jsonTemplate = kafkaJsonTemplate;
        this.boardTemplate = kafkaBoardTemplate;
    }

    public void sendTestEventAsString(String testEvent) {
        try {
            stringTemplate.send(TOPIC, testEvent);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
    }

    public void sendTestEvent(TestEvent testEvent) {
        try {
            jsonTemplate.send(TOPIC, testEvent);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
    }

    public void sendDefaultBoardCreateEvent(DefaultBoardCreateEvent defaultBoardCreateEvent) {
        log.info("consumer :: transaction-id :: {}", TransactionAspectSupport.currentTransactionStatus().hashCode());
        try {
            boardTemplate.send(TOPIC, defaultBoardCreateEvent);
            System.out.println("default board create event publish");
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.info("kafka message produce fail");
        }
    }
}
