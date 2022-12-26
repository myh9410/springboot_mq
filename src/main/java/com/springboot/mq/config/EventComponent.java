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

    /**
     * EventListener의 경우, 상위 트랜잭션과 상관없이 동작
     *
     * TransactionalEventListener의 경우, 상위 트랜잭션에 영향을 받는다.
     * TransactionPhase.AFTER_COMPLETION :
     *      async 적용되어 다른 쓰레드 id로 실행 / DB insert 성공(트랜잭션 성공, 실패 여부와 상관없이 동작한다.)
     * TransactionPhase.AFTER_COMMIT :
     *      이벤트 동작 X (트랜잭션이 성공하지 않았기 때문에 이벤트가 동작하지 않는다.)
     * TransactionPhase.BEFORE_COMMIT : 
     *      트랜잭션의 커밋 전에 이벤트 실행이 되어야하는데
     *      RuntimeException 발생 시 ROLLBACK 동작하여 이벤트 동작하지 않는것으로 보임
     *      정상 동작한다면, async 적용되어 다른 쓰레드 id로 실행됨
     *  TransactionPhase.AFTER_ROLLBACK :
     *      정상 동작한다면, 이벤트 실행되지 않음.
     *      ROLLBACK 발생 시 이벤트 동작하여 데이터 추가됨. (다른 쓰레드 id)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
//    @EventListener
    @Async
    public void eventListen(TestEvent testEvent) {
        System.out.println("EventListener Action");
        System.out.println("async 동작 :: " + Thread.currentThread().getId() + " :: " + testEvent.toString());
        testService.doTest();
    }
}
