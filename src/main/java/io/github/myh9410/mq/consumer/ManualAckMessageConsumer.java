package io.github.myh9410.mq.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.message.Message;
import io.github.myh9410.mq.message.Topics;

/**
 * Manual offset commit 패턴.
 * 처리 성공 직후에만 ack.acknowledge() 를 호출 → 처리 도중 죽으면 다음 instance가 같은 record를 다시 받는다.
 *
 * 학습용 시연: content가 "noack"로 시작하면 의도적으로 ack를 호출하지 않는다.
 * 동일 JVM 내에서는 다시 안 받지만, 컨슈머 재시작 시 last committed offset부터 다시 시작하므로 재처리된다.
 */
@Component
public class ManualAckMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ManualAckMessageConsumer.class);

    @KafkaListener(
        topics = Topics.MESSAGES,
        groupId = "springboot-mq-manualack-consumer",
        containerFactory = "manualAckKafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, Message> record, Acknowledgment ack) {
        Message msg = record.value();
        if (msg != null && msg.content() != null && msg.content().startsWith("noack")) {
            log.warn("ManualAck SKIPPING ack: partition={} offset={} key={} (redelivered on restart)",
                record.partition(), record.offset(), record.key());
            return;
        }

        log.info("ManualAck processed: partition={} offset={} key={} payload={}",
            record.partition(), record.offset(), record.key(), record.value());
        ack.acknowledge();
    }
}
