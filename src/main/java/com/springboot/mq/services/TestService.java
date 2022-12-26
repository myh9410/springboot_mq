package com.springboot.mq.services;

import com.springboot.mq.entity.Test;
import com.springboot.mq.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TestService {
    private final TestRepository testRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void doTest() {
        System.out.println("entity 영속화 전 :: " + Thread.currentThread().getId());
        testRepository.save(Test.builder().name("test1").build());
    }

}
