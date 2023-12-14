package com.springboot.mq.infrastructure;

import com.springboot.mq.domains.domain.Callback;
import com.springboot.mq.domains.dto.DefaultBoardCreateEvent;
import com.springboot.mq.domains.dto.TestEvent;

import com.springboot.mq.domains.repository.callback.CallbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class EventComponent {
    private final KafkaProducer kafkaProducer;
    private final CallbackRepository callbackRepository;

    /**
     * EventListener의 경우, 상위 트랜잭션과 상관없이 동작
     *
     * TransactionalEventListener의 경우, 상위 트랜잭션에 영향을 받는다.
     * TransactionPhase.AFTER_COMPLETION :
     *      async 적용되어 다른 쓰레드 id로 실행 / DB insert 성공(트랜잭션 성공, 실패 여부와 상관없이 동작한다.)
     * TransactionPhase.AFTER_COMMIT :
     *      트랜잭션이 성공하지 않는다면 이벤트를 실행하지 않는다.
     * TransactionPhase.BEFORE_COMMIT : 
     *      트랜잭션의 커밋 전에 이벤트 실행이 되어야하는데
     *      RuntimeException 발생 시 ROLLBACK 동작하여 이벤트를 실행하지 않는다.
     *      정상 동작한다면, async 적용되어 다른 쓰레드 id로 실행됨
     *      AFTER_COMMIT과 다른점은 상위 transaction에 대한 롤백 처리에 영향을 줄 수 있음. (BEFORE_COMMIT에서 exception발생 시 상위 트랜잭션 롤백)
     *  TransactionPhase.AFTER_ROLLBACK :
     *      정상 동작한다면, 이벤트 실행되지 않음.
     *      ROLLBACK 발생 시 이벤트 동작하여 데이터 추가됨. (다른 쓰레드 id)
     */
    /*
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void eventListen(TestEvent testEvent) {
        System.out.println("async 동작 :: " + Thread.currentThread().getId() + " :: " + testEvent.toString());
        testService.createTestData(testEvent.getEvent());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void eventListen2(TestEvent testEvent) {
        System.out.println("eventListen2 동작 :: " + Thread.currentThread().getId() + " :: " + object.toString());
    }
    */

    /**
     * EventListener의 경우, 특정 value를 설정해주지 않으면 기본적으로 모든 이벤트에 실행되는 것으로 보임
     * ex) ConsumerStartingEvent, ConsumerStartedEvent, ApplicationReadyEvent, ApplicationStartedEvent, ContextRefreshedEvent 등 여러 event에 다 걸림
     *
     * ServletRequestHandledEvent extends RequestHandledEvent 이고, RequestHandledEvent는 HTTP 요청을 처리했을 떄 발생한다.
     * 따라서, 만약 value 설정이 RequestHandledEvent이고, 서비스 내에서 applicationEventPublisher로 publish 하는 경우, 이벤트는 두번 실행될 수 있다.
     *
     * 당연히 @Async가 있다면, 전부 다른 쓰레드 id로 비동기 동작
     */
    /*
    @EventListener(TestEvent.class)
    public void eventListen3(Object object) {
        System.out.println("eventListen3 동작 :: " + Thread.currentThread().getId() + " :: " + object);
    }
    */

    //condition으로 eventlistener가 동작하는 조건을 설정할 수 있다.
    @Transactional
    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            condition = "#testEvent.no > 35"
    )
    @Async
    public void publishTestEvent(TestEvent testEvent) {
        log.info("""
                
                [log start]
                이벤트 - publishTestEvent :: {}
                트랜잭션 hashCode :: {}
                [log end]
                """, Thread.currentThread().getId(), TransactionAspectSupport.currentTransactionStatus().hashCode());

        Optional<Callback> optionalCallback = callbackRepository.findById(testEvent.getNo());

        if (optionalCallback.isEmpty()) {
            callbackRepository.save(Callback.builder().no(testEvent.getNo()).count(1L).build());
        } else {
            Callback callback = optionalCallback.get();
            callback.addCount();
            callbackRepository.save(callback);
        }

        kafkaProducer.sendTestEvent(testEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Async
    public void publishTestEvent(String testEventAsString) {
        kafkaProducer.sendTestEventAsString(testEventAsString);
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void publishDefaultBoardCreate(DefaultBoardCreateEvent defaultBoardCreateEvent) {
        kafkaProducer.sendDefaultBoardCreateEvent(defaultBoardCreateEvent);
    }
}
