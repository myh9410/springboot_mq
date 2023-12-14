package com.springboot.mq.domains.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateUserRequest {

    private String id;

    private String name;

    private String password;

}
