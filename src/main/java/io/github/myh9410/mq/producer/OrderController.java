package io.github.myh9410.mq.producer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.myh9410.mq.message.OrderEvent;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final EventPublisher publisher;

    public OrderController(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<Void> publish(@RequestBody OrderEvent order) {
        publisher.publish(order);
        return ResponseEntity.accepted().build();
    }
}
