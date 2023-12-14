package com.springboot.mq.infrastructure;

import com.springboot.mq.domains.dto.DefaultBoardCreateEvent;
import com.springboot.mq.domains.repository.board.BoardRepository;
import com.springboot.mq.web.services.BoardService;
import com.springboot.mq.web.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@RequiredArgsConstructor
@Slf4j
@Component
public class KafkaConsumer {

    private final BoardService boardService;
    private final UserService userService;

    /**
     * 동일 topic, group-id에 대한 listner의 경우, 상위 listener가 먼저 타는 것 같은데...
     * @param defaultBoardCreateEvent
     */
//    @KafkaListener(topics = "create", groupId = "event", containerFactory = "kafkaListenerContainerFactory")
//    public void consume(String testEvent) {
//        System.out.println("consumed message :: " + testEvent);
//    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @KafkaListener(topics = "create", groupId = "event", containerFactory = "kafkaListenerContainerBoardFactory")
    public void createDefaultBoard(@Payload DefaultBoardCreateEvent defaultBoardCreateEvent) {
        log.info("consumer :: is-new {} :: transaction-id :: {}",
                TransactionAspectSupport.currentTransactionStatus().isNewTransaction(),
                TransactionAspectSupport.currentTransactionStatus().hashCode()
        );

        boardService.createWelcomeBoard(defaultBoardCreateEvent.getUserNo());

    }
}
