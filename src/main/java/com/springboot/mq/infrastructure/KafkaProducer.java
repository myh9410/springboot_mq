package com.springboot.mq.infrastructure;

import com.springboot.mq.domains.dto.TestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducer {
    private static final String TOPIC = "create";
    private final KafkaTemplate<String, Object> stringTemplate;
    private final KafkaTemplate<String, Object> jsonTemplate;

    public KafkaProducer(
        @Qualifier(value = "stringTemplate") KafkaTemplate<String, Object> kafkaStringTemplate,
        @Qualifier(value = "jsonTemplate") KafkaTemplate<String, Object> kafkaJsonTemplate
    ) {
        this.stringTemplate = kafkaStringTemplate;
        this.jsonTemplate = kafkaJsonTemplate;
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
}
