package io.github.myh9410.mq.consumer;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import tools.jackson.databind.json.JsonMapper;

import io.github.myh9410.mq.message.Message;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Message> consumerFactory(
        KafkaProperties kafkaProperties,
        JsonMapper jsonMapper
    ) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // target type을 명시하고 type info 헤더는 무시 — producer가 어떤 클래스를 보냈는지에 의존하지 않는다.
        JacksonJsonDeserializer<Message> valueDeserializer =
            new JacksonJsonDeserializer<>(Message.class, jsonMapper, false);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaListenerContainerFactory(
        ConsumerFactory<String, Message> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
