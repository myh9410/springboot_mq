package com.springboot.mq.services;

import com.springboot.mq.dto.event.TestEvent;
import com.springboot.mq.entity.Test;
import com.springboot.mq.services.KafkaProducer;
import com.springboot.mq.services.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * emitter : 이벤트를 발생시키는 객체
 */
@RequiredArgsConstructor
@Service
public class IntegratedService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TestService testService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doAction() {
        System.out.println("서비스 :: " + Thread.currentThread().getId());
        //이벤트 퍼블리셔 통한 이벤트 발생
        applicationEventPublisher.publishEvent(
                TestEvent.builder()
                        .no(1L)
                        .event("test1 이벤트 추가")
                        .build()
        );

        throw new RuntimeException("testRuntimeException");
//        System.out.println("doAction 동작 끝!");
    }

    /**
     * test DB에 데이터를 넣고, 이벤트를 발생시킨다.
     */
    public void createTestEvent(String message) {
        //1. DB에 데이터를 넣는다.
        Test test = testService.createTestData(message);

        //2. 이벤트를 발생시킨다.
        applicationEventPublisher.publishEvent(
                TestEvent.builder()
                    .no(test.getNo())
                    .event(test.getName())
                    .build()
        );
    }

}
