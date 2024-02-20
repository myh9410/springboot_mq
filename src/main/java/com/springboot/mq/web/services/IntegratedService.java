package com.springboot.mq.web.services;

import com.springboot.mq.domains.domain.User;
import com.springboot.mq.domains.dto.TestEvent;
import com.springboot.mq.domains.domain.Test;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * emitter : 이벤트를 발생시키는 객체
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class IntegratedService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TestService testService;
    private final UserService userService;
    private final BoardService boardService;

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUserAndDefaultBoard(String userId) {
        userService.createUserData();
        boardService.createWelcomeBoard(1L);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doAction() {
        System.out.println("서비스 :: " + Thread.currentThread().getId());

        Test test = testService.createTestData("transactional 과 eventlistener 간 롤백 범위 확인");

        //이벤트 퍼블리셔 통한 이벤트 발생
        applicationEventPublisher.publishEvent(
                TestEvent.builder()
                    .no(test.getNo())
                    .event(test.getName())
                    .build()
        );

        throw new RuntimeException("testRuntimeException");
//        System.out.println("doAction 동작 끝!");
    }

    /**
     * test DB에 데이터를 넣고, 이벤트를 발생시킨다.
     */
    @Transactional(transactionManager = "jtaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void createTestEvent(String message) {
        log.info("""
                
                [integratedService log start]
                이벤트 - createTestEvent :: {}
                트랜잭션 hashCode :: {}
                [log end]
                """, Thread.currentThread().getId(), TransactionAspectSupport.currentTransactionStatus().hashCode());
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

    /**
     * test DB에 데이터를 넣고, 이벤트를 발생시킨다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTestEventAsString(String message) {
        //1. DB에 데이터를 넣는다.
        Test test = testService.createTestData(message);

        //2. 이벤트를 발생시킨다.
        applicationEventPublisher.publishEvent(
                TestEvent.builder()
                    .no(test.getNo())
                    .event(test.getName())
                    .build().toString()
        );
    }

    /**
     * 멀티 DB에 대한 트랜잭션매니저 테스트
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "jtaTransactionManager"
    )
    public void multiDataSourceTransacionTest(String message) {
        //1. DB에 데이터를 넣는다.
        Test test = testService.createTestData(message);

        //2. DB에 데이터를 넣는다.
        User user = userService.createUserData();
    }

}
