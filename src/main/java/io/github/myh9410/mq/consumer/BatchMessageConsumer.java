package io.github.myh9410.mq.consumer;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.message.Message;
import io.github.myh9410.mq.message.Topics;

/**
 * messages 토픽을 batch로 소비하는 두 번째 consumer.
 * group-id가 MessageConsumer와 다르므로 같은 record를 양쪽이 독립적으로 받는다 (pub/sub).
 */
@Component
public class BatchMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(BatchMessageConsumer.class);

    @KafkaListener(
        topics = Topics.MESSAGES,
        groupId = "springboot-mq-batch-consumer",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onBatch(List<ConsumerRecord<String, Message>> records) {
        log.info("Batch received: size={}", records.size());
        records.forEach(r ->
            log.info("  - partition={} offset={} key={}", r.partition(), r.offset(), r.key())
        );
    }
}
