package com.springboot.mq.domains.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder
public class TestEvent {
    long no;
    String event;
}
