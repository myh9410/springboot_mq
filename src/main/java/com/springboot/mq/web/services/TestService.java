package com.springboot.mq.web.services;

import com.springboot.mq.domains.domain.Test;
import com.springboot.mq.domains.repository.test.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * test DB의 데이터에 관한 처리만 한다.
 */
@RequiredArgsConstructor
@Service
public class TestService {
    private final TestRepository testRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public Test createTestData(String message) {
        Test test = testRepository.save(Test.builder().name(message).build());

        return test;
    }

}
