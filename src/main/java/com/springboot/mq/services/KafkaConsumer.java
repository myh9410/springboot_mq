package com.springboot.mq.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @KafkaListener(topics = "test-events", groupId = "foo")
    public void consume(String message) {
        System.out.printf("Consumed Message : %s%n",message);
    }
}
