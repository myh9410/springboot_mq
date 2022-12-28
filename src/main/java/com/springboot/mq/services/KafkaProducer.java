package com.springboot.mq.services;

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

    public void sendTestEventAsString(String testEvent) {
        try {
            this.kafkaTemplate.send(TOPIC, testEvent);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
    }
}
