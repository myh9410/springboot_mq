package com.springboot.mq.infrastructure;

import com.springboot.mq.domains.dto.DefaultBoardCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@RequiredArgsConstructor
@Slf4j
@Service
public class KafkaConsumer {

    /**
     * 동일 topic, group-id에 대한 listner의 경우, 상위 listener가 먼저 타는 것 같은데...
     * @param defaultBoardCreateEvent
     */
//    @KafkaListener(topics = "create", groupId = "event", containerFactory = "kafkaListenerContainerFactory")
//    public void consume(String testEvent) {
//        System.out.println("consumed message :: " + testEvent);
//    }

    @Transactional
    @KafkaListener(topics = "create", groupId = "event", containerFactory = "kafkaListenerContainerBoardFactory")
    public void createDefaultBoard(@Payload DefaultBoardCreateEvent defaultBoardCreateEvent) {
        log.info("consumer :: transaction-id :: {}", TransactionAspectSupport.currentTransactionStatus().hashCode());
        System.out.println("defaultBoardCreate - user_no :: " + defaultBoardCreateEvent.getUserNo());
    }
}
