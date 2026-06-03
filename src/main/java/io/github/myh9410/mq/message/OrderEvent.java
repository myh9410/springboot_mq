package io.github.myh9410.mq.message;

import java.time.Instant;

public record OrderEvent(String id, String item, int quantity, Instant occurredAt) implements MqEvent {
}
