package io.github.myh9410.mq.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.github.myh9410.mq.message.Topics;

/**
 * DLQ 토픽에 어떤 record가 떨어지는지 관찰하기 위한 학습용 listener.
 * 실무에서는 보통 별도 모니터링 시스템이나 운영자가 직접 처리한다.
 */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    @KafkaListener(
        topics = Topics.DLQ,
        groupId = "springboot-mq-dlq-watcher",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onDlqRecord(ConsumerRecord<String, String> record) {
        StringBuilder headers = new StringBuilder();
        for (Header h : record.headers()) {
            String key = h.key();
            // DeadLetterPublishingRecoverer가 자동으로 채워주는 메타데이터 헤더만 텍스트로 출력
            if (key.startsWith("kafka_dlt-")) {
                headers.append(key).append('=')
                    .append(new String(h.value(), StandardCharsets.UTF_8)).append(' ');
            }
        }
        log.warn("DLQ record: key={} payload={} | headers: {}",
            record.key(), record.value(), headers.toString().trim());
    }
}
