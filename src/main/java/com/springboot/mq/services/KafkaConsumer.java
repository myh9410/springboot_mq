package com.springboot.mq.services;

import com.springboot.mq.dto.event.TestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class KafkaConsumer {

    @KafkaListener(topics = "create", groupId = "event")
    public void consume(TestEvent testEvent) {
        System.out.println("consumed message :: " + testEvent.toString());
    }
}
