package com.springboot.mq.web.services;

import com.springboot.mq.domains.domain.Test;
import com.springboot.mq.domains.repository.test.TestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * test DB의 데이터에 관한 처리만 한다.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TestService {
    private final TestRepository testRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public Test createTestData(String message) {
        log.info("""
                
                [log start]
                이벤트 - createTestData :: {}
                트랜잭션 hashCode :: {}
                [log end]
                """, Thread.currentThread().getId(), TransactionAspectSupport.currentTransactionStatus().hashCode());
        Test test = testRepository.save(Test.builder().name(message).build());

        return test;
    }

}
