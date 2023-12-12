package com.springboot.mq.web.services;

import com.springboot.mq.domains.domain.User;
import com.springboot.mq.domains.dto.UserInfo;
import com.springboot.mq.domains.dto.request.CreateUserRequest;
import com.springboot.mq.domains.repository.customer.user.UserRepository;
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

    public UserInfo findUserByNo(Long no) {
        return  userRepository.getUserInfoByNo(no);
    }

    public UserInfo createUser(CreateUserRequest createUserRequest) {
        return null;
    }
}
