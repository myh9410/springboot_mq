package com.springboot.mq.controller;

import com.springboot.mq.services.IntegratedService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@EnableAsync
@RequestMapping(value = "/kafka")
public class KafkaController {
    private final IntegratedService integratedService;

    @PostMapping
    public String createMessageDataAndEmitEvent(@RequestParam("message") String message) {
        integratedService.createTestEvent(message);

        return "SUCCESS";
    }

    @GetMapping(value = "/async")
    public String getAsyncAction() {
        System.out.println("컨트롤러 :: " + Thread.currentThread().getId());
        integratedService.doAction();
        return "SUCCESS";
    }

}
