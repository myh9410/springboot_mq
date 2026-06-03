package io.github.myh9410.mq.producer;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import io.micrometer.core.instrument.MeterRegistry;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Bean
    public ProducerFactory<String, Object> producerFactory(
        KafkaProperties kafkaProperties,
        JsonMapper jsonMapper,
        MeterRegistry meterRegistry
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());

        // 신뢰성 강화: idempotent producer로 중복/순서 보장.
        // - acks=all + replicas의 동의가 있어야 ack — 단일 브로커에선 의미 없지만 운영 멀티 브로커 가정.
        // - enable.idempotence=true: producer가 자체 sequence number를 부여해 중복 send 자동 dedup.
        // - max.in.flight=3: idempotence를 켜려면 5 이하여야 함 (Kafka 3+).
        // - retries=5: 일시적 네트워크 에러에 자동 재시도.
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 3);
        config.put(ProducerConfig.RETRIES_CONFIG, 5);

        // Spring이 구성한 JsonMapper(JavaTimeModule 등록됨)를 그대로 사용하기 위해
        // serializer는 config map이 아니라 constructor로 주입한다.
        JacksonJsonSerializer<Object> valueSerializer = new JacksonJsonSerializer<>(jsonMapper);
        valueSerializer.setAddTypeInfo(false);

        DefaultKafkaProducerFactory<String, Object> factory =
            new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valueSerializer);
        factory.addListener(new MicrometerProducerListener<>(meterRegistry));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        // 모든 send 결과를 한 곳에서 처리 — 호출부의 whenComplete 콜백 책임을 framework 레이어로 옮긴다.
        template.setProducerListener(new ProducerListener<String, Object>() {
            @Override
            public void onSuccess(ProducerRecord<String, Object> record, RecordMetadata metadata) {
                log.info("Sent ok: topic={} partition={} offset={} key={}",
                    metadata.topic(), metadata.partition(), metadata.offset(), record.key());
            }

            @Override
            public void onError(ProducerRecord<String, Object> record, RecordMetadata metadata, Exception exception) {
                log.error("Send failed: topic={} key={}", record.topic(), record.key(), exception);
            }
        });
        return template;
    }
}
