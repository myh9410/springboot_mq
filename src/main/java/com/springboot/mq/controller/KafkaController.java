package com.springboot.mq.controller;

import com.springboot.mq.services.IntegratedService;
import com.springboot.mq.services.KafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@EnableAsync
@RequestMapping(value = "/kafka")
public class KafkaController {
    private final KafkaProducer producer;
    private final IntegratedService integratedService;

    @PostMapping
    public String sendMessage(@RequestParam("message") String message) {
        this.producer.sendMessage(message);

        return "SUCCESS";
    }

    @GetMapping(value = "/async")
    public String getAsyncAction() {
        System.out.println("컨트롤러 :: " + Thread.currentThread().getId());
        integratedService.doAction();
        return "SUCCESS";
    }

}
