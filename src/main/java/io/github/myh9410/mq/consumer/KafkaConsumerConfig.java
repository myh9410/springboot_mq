package io.github.myh9410.mq.consumer;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import tools.jackson.databind.json.JsonMapper;

import io.github.myh9410.mq.message.Message;
import io.github.myh9410.mq.message.Topics;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Message> consumerFactory(
        KafkaProperties kafkaProperties,
        JsonMapper jsonMapper
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // JacksonJsonDeserializer를 ErrorHandlingDeserializer로 감싸 역직렬화 실패가 컨슈머를 죽이지 않게 한다.
        // 실패 시 value는 null이 되고 DeserializationException이 header에 실린다 → DefaultErrorHandler가 받아 DLQ로 라우팅.
        JacksonJsonDeserializer<Message> jsonDeserializer =
            new JacksonJsonDeserializer<>(Message.class, jsonMapper, false);
        ErrorHandlingDeserializer<Message> valueDeserializer =
            new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaListenerContainerFactory(
        ConsumerFactory<String, Message> consumerFactory,
        DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * DLQ 관찰용 String-payload listener container. 원본 payload를 그대로 받아 헤더와 같이 로깅한다.
     */
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        return new DefaultKafkaConsumerFactory<>(
            config, new StringDeserializer(), new StringDeserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(
        ConsumerFactory<String, String> stringConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory);
        return factory;
    }

    /**
     * DLQ 전송 전용 KafkaTemplate.
     * 핸들러가 throw한 시점의 value는 이미 역직렬화된 Message 객체 — 다시 JSON으로 직렬화해 DLQ에 싣는다.
     * (단순 StringSerializer는 Message → String 캐스팅 실패로 깨진다.)
     */
    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
        KafkaProperties kafkaProperties,
        JsonMapper jsonMapper
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        JacksonJsonSerializer<Object> valueSerializer = new JacksonJsonSerializer<>(jsonMapper);
        valueSerializer.setAddTypeInfo(false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
            config, new StringSerializer(), valueSerializer
        ));
    }

    /**
     * 학습 환경용 backoff: 0.5s → 1s → 2s, 총 ~5s 후 DLQ.
     * (운영에서는 보통 1s → 30s, total 5min 같은 값을 쓴다.)
     * invariant: maxElapsedTime < max.poll.interval.ms (기본 5min) 를 반드시 지킬 것.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dltKafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(500L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(2_000L);
        backOff.setMaxElapsedTime(5_000L);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            dltKafkaTemplate,
            (record, ex) -> new TopicPartition(Topics.DLQ, -1)
        );

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
