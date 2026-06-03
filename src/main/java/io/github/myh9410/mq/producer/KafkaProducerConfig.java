package io.github.myh9410.mq.producer;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(
        KafkaProperties kafkaProperties,
        JsonMapper jsonMapper
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

        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
