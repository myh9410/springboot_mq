package com.springboot.mq.web.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class KafkaConsumer {

    @KafkaListener(topics = "create", groupId = "event")
    public void consume(String testEvent) {
        System.out.println("consumed message :: " + testEvent);
    }
}
