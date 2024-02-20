package com.springboot.mq.web.controller;

import com.springboot.mq.domains.dto.UserInfo;
import com.springboot.mq.domains.dto.request.CreateUserRequest;
import com.springboot.mq.web.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{no}")
    public ResponseEntity<UserInfo> getByNo(@PathVariable(name = "no") Long no) {
        UserInfo userInfo = userService.findUserByNo(no);

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping()
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest createUserRequest) {
        UserInfo userInfo = userService.createUser(createUserRequest);
        return ResponseEntity.created(URI.create("/users/"+userInfo.getNo())).build();
    }

    @GetMapping("/vt")
    public String getByVT() {
        return "";
    }
}
