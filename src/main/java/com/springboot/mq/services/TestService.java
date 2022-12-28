package com.springboot.mq.services;

import com.springboot.mq.entity.Test;
import com.springboot.mq.repository.TestRepository;
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
        System.out.println("데이터 생성");
        Test test = testRepository.save(Test.builder().name(message).build());
        System.out.println(test.toString());

        return test;
    }

}
