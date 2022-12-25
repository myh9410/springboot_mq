package com.springboot.mq.dto.event;

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
