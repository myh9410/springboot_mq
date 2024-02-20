package com.springboot.mq.web.services;

import com.springboot.mq.domains.domain.User;
import com.springboot.mq.domains.dto.DefaultBoardCreateEvent;
import com.springboot.mq.domains.dto.UserInfo;
import com.springboot.mq.domains.dto.request.CreateUserRequest;
import com.springboot.mq.domains.repository.customer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public User createUserData() {
        User user = userRepository.save(
                User.builder()
                    .id("myh9410")
                    .name("문용호")
                    .password("password")
                    .build()
        );

        return user;
    }

    @Transactional(readOnly = true)
    public UserInfo findUserByNo(Long no) {
        UserInfo userInfo = userRepository.getUserInfoByNo(no);

        User user = entityManager.find(User.class, userInfo.getNo());

        log.info("is entityManager contains - service : " + entityManager.contains(user));

        return userInfo;
    }

    @Transactional
    public UserInfo createUser(CreateUserRequest createUserRequest) {
        log.info("userService :: is-new {} :: transaction-id :: {}",
                TransactionAspectSupport.currentTransactionStatus().isNewTransaction(),
                TransactionAspectSupport.currentTransactionStatus().hashCode()
        );

        User user = userRepository.save(
            User.createUser(
                createUserRequest.getId(),
                createUserRequest.getName(),
                createUserRequest.getPassword()
            )
        );

        applicationEventPublisher.publishEvent(DefaultBoardCreateEvent.builder().userNo(user.getNo()).build());

        return new UserInfo(user);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateDate(Long userNo) {
        User user = userRepository.findById(userNo).orElseThrow(RuntimeException::new);

        user.changeUpdateDateToNow();
    }
}
