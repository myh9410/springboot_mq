package io.github.myh9410.mq.message;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 토픽 ↔ 이벤트 클래스 매핑.
 * sealed MqEvent 와 함께 — 새 이벤트가 추가되면 컴파일 에러 / switch warning 으로 강제됨.
 */
public enum EventType {

    MESSAGE(Topics.MESSAGES, Message.class),
    ORDER(Topics.ORDERS, OrderEvent.class);

    private final String topic;
    private final Class<? extends MqEvent> eventClass;

    EventType(String topic, Class<? extends MqEvent> eventClass) {
        this.topic = topic;
        this.eventClass = eventClass;
    }

    public String topic() {
        return topic;
    }

    public Class<? extends MqEvent> eventClass() {
        return eventClass;
    }

    private static final Map<String, EventType> BY_TOPIC =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(EventType::topic, Function.identity()));

    private static final Map<Class<? extends MqEvent>, EventType> BY_CLASS =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(EventType::eventClass, Function.identity()));

    public static EventType fromTopic(String topic) {
        EventType type = BY_TOPIC.get(topic);
        if (type == null) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }
        return type;
    }

    public static EventType fromClass(Class<? extends MqEvent> clazz) {
        EventType type = BY_CLASS.get(clazz);
        if (type == null) {
            throw new IllegalArgumentException("Unknown event class: " + clazz.getName());
        }
        return type;
    }
}
