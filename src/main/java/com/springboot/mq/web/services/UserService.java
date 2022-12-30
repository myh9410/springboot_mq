package com.springboot.mq.web.services;

import com.springboot.mq.domains.domain.User;
import com.springboot.mq.domains.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public User createUserData() {
        return userRepository.save(
                User.builder()
                    .id("myh9410")
                    .name("문용호")
                    .build()
        );
    }

}
