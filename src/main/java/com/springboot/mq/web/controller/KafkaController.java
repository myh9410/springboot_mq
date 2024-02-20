package com.springboot.mq.web.controller;

import com.springboot.mq.web.services.IntegratedService;
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

    @PostMapping("/users")
    public void createUserAndDefaultBoard(@RequestParam("user_id") String userId) {
        integratedService.createUserAndDefaultBoard(userId);
    }

    @PostMapping(value = "/string")
    public String createMessageDataAndEmitEventOfString(@RequestParam("message") String message) {
        integratedService.createTestEventAsString(message);

        return "SUCCESS";
    }

    @PostMapping(value = "/transaction")
    public String multipleDataSourceTransactionTest(@RequestParam("message") String message) {
        integratedService.multiDataSourceTransacionTest(message);

        return "SUCCESS";
    }

    @GetMapping(value = "/async")
    public String getAsyncAction() {
        System.out.println("컨트롤러 :: " + Thread.currentThread().getId());
        integratedService.doAction();
        return "SUCCESS";
    }

}
