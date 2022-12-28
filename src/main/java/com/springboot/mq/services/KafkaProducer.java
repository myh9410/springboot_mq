package com.springboot.mq.services;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KafkaProducer {
    private static final String TOPIC = "create";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(String message) {
        try {
            this.kafkaTemplate.send(TOPIC, message);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
