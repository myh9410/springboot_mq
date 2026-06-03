package io.github.myh9410.mq.consumer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import io.github.myh9410.mq.message.EventType;
import io.github.myh9410.mq.message.MqEvent;
import io.github.myh9410.mq.message.Topics;

/**
 * sealed MqEvent + EventType + MqEventConverter 패턴을 하나로 묶는 디스패처.
 * 여러 토픽을 한 listener로 구독해 토픽 → 클래스 매핑으로 typed 객체를 만들고, 해당 converter로 위임.
 * 새 이벤트가 추가될 때 MqEvent permits / EventType enum / MqEventConverter 구현 3곳을 함께 손대도록 강제됨.
 */
@Component
public class EventDispatchingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventDispatchingConsumer.class);

    private final JsonMapper jsonMapper;
    private final Map<Class<? extends MqEvent>, MqEventConverter<?>> converterByClass;

    public EventDispatchingConsumer(JsonMapper jsonMapper, List<MqEventConverter<?>> converters) {
        this.jsonMapper = jsonMapper;
        this.converterByClass = converters.stream()
            .collect(Collectors.toUnmodifiableMap(
                MqEventConverter::eventType,
                c -> c,
                (a, b) -> {
                    throw new IllegalStateException(
                        "duplicate converter for event type: " + a.eventType());
                }
            ));
        log.info("EventDispatchingConsumer wired with {} converters: {}",
            converterByClass.size(), converterByClass.keySet());
    }

    @KafkaListener(
        topics = { Topics.MESSAGES, Topics.ORDERS },
        groupId = "springboot-mq-dispatch-consumer",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onEvent(ConsumerRecord<String, String> record) {
        EventType type = EventType.fromTopic(record.topic());
        Class<? extends MqEvent> clazz = type.eventClass();

        MqEvent event = jsonMapper.readValue(record.value(), clazz);
        dispatch(event);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dispatch(MqEvent event) {
        MqEventConverter converter = converterByClass.get(event.getClass());
        if (converter == null) {
            throw new IllegalStateException("no converter for " + event.getClass());
        }
        converter.handle(event);
    }
}
