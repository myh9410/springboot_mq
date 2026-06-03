package io.github.myh9410.mq.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.message.EventType;
import io.github.myh9410.mq.message.MqEvent;

/**
 * 모든 MqEvent를 받아 자동으로 토픽을 결정해 발행한다.
 * EventType enum이 토픽 ↔ 클래스 매핑의 단일 진실 공급원.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MqEvent event) {
        EventType type = EventType.fromClass(event.getClass());
        log.info("Publishing: topic={} key={} payload={}", type.topic(), event.id(), event);
        // Send 결과 로깅은 KafkaTemplate에 부착된 ProducerListener가 담당.
        kafkaTemplate.send(type.topic(), event.id(), event);
    }
}
