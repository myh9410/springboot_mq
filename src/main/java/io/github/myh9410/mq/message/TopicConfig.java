package io.github.myh9410.mq.message;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 토픽 partition 구성을 코드로 명시.
 * Spring Boot의 KafkaAdmin이 startup 시 부재한 토픽을 자동 생성한다.
 * (이미 존재하는 토픽의 partition 수를 늘리지는 않으므로, 기존 토픽이 있다면 삭제 후 재시작.)
 */
@Configuration
public class TopicConfig {

    @Bean
    public NewTopic messagesTopic() {
        return TopicBuilder.name(Topics.MESSAGES)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(Topics.DLQ)
            .partitions(1)
            .replicas(1)
            .build();
    }
}
