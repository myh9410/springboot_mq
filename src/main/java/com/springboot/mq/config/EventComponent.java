package com.springboot.mq.config;

import com.springboot.mq.dto.event.TestEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EventComponent {
    @EventListener
    @Async
    public void eventListen(TestEvent testEvent) {
        System.out.println("async 동작 :: " + Thread.currentThread().getId() + " :: " + testEvent.toString());
    }
}
