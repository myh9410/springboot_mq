package io.github.myh9410.mq.consumer;

import io.github.myh9410.mq.message.MqEvent;

/**
 * 도메인 이벤트별 처리 전략 인터페이스.
 * 새 MqEvent 타입을 추가하려면 이 인터페이스의 구현 빈도 함께 등록되어야 한다.
 */
public interface MqEventConverter<T extends MqEvent> {

    Class<T> eventType();

    void handle(T event);
}
