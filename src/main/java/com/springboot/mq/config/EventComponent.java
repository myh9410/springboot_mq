package com.springboot.mq.config;

import com.springboot.mq.dto.event.TestEvent;
import com.springboot.mq.services.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class EventComponent {

    private final TestService testService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
//    @EventListener
    @Async
    public void eventListen(TestEvent testEvent) {
        System.out.println("EventListener Action");
        System.out.println("async 동작 :: " + Thread.currentThread().getId() + " :: " + testEvent.toString());
        testService.doTest();
    }
}
