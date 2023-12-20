package com.springboot.mq.web.controller;

import com.springboot.mq.domains.domain.User;
import com.springboot.mq.domains.dto.UserInfo;
import com.springboot.mq.domains.dto.request.CreateUserRequest;
import com.springboot.mq.web.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PersistenceContext
    EntityManager entityManager;

    @GetMapping("/{no}")
    public ResponseEntity<UserInfo> getByNo(@PathVariable(name = "no") Long no) {
        UserInfo userInfo = userService.findUserByNo(no);

        log.info("is entityManager contains - controller : " + entityManager.isJoinedToTransaction());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping()
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest createUserRequest) {
        UserInfo userInfo = userService.createUser(createUserRequest);
        return ResponseEntity.created(URI.create("/users/"+userInfo.getNo())).build();
    }
}
