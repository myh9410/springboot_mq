package io.github.myh9410.mq.message;

/**
 * Kafka로 발행되는 모든 이벤트 DTO의 공통 마커.
 * sealed로 닫아둠으로써 새 event를 추가할 때 EventType / Converter 매핑도 반드시 업데이트되도록 강제.
 */
public sealed interface MqEvent permits Message, OrderEvent {
    String id();
}
