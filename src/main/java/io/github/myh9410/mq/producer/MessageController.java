package io.github.myh9410.mq.producer;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.myh9410.mq.message.Message;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final EventPublisher publisher;

    public MessageController(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<Void> publish(@RequestBody Message message) {
        publisher.publish(message);
        return ResponseEntity.accepted().build();
    }

    /**
     * batch 시연용: count개 메시지를 빠르게 발사한다. 각 메시지의 key는 UUID이므로 partition에 골고루 분배된다.
     */
    @PostMapping("/burst")
    public ResponseEntity<Void> burst(@RequestParam(defaultValue = "10") int count) {
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            String id = "burst-" + UUID.randomUUID();
            publisher.publish(new Message(id, "burst#" + i, now));
        }
        return ResponseEntity.accepted().build();
    }
}
