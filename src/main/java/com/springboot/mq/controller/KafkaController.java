package com.springboot.mq.controller;

import com.springboot.mq.dto.event.TestEvent;
import com.springboot.mq.services.KafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@EnableAsync
@RequestMapping(value = "/kafka")
public class KafkaController {
    private final KafkaProducer producer;
    private final ApplicationEventPublisher applicationEventPublisher;

    @PostMapping
    public String sendMessage(@RequestParam("message") String message) {
        this.producer.sendMessage(message);

        return "SUCCESS";
    }

    @GetMapping(value = "/async")
    public String getAsyncAction() {
        System.out.println("publish 전 :: " + Thread.currentThread().getId());
        applicationEventPublisher.publishEvent(TestEvent.builder().no(1L).event("테스트 이벤트").build());
        return "SUCCESS";
    }

}
