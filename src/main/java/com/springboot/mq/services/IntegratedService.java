package com.springboot.mq.services;

import com.springboot.mq.dto.event.TestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class IntegratedService {

    private final ApplicationEventPublisher applicationEventPublisher;

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

}
