package com.springboot.mq.services;

import com.springboot.mq.dto.event.TestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducer {
    private static final String TOPIC = "create";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTemplate<String, Object> kafkaJsonTemplate;

    public void sendTestEventAsString(String testEvent) {
        try {
            this.kafkaTemplate.send(TOPIC, testEvent);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
    }

    public void sendTestEvent(TestEvent testEvent) {
        try {
            this.kafkaJsonTemplate.send(TOPIC, testEvent);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
    }
}
