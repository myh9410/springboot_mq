package io.github.myh9410.mq.message;

import java.time.Instant;

public record Message(String id, String content, Instant occurredAt) {
}
