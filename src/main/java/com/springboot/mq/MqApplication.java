package com.springboot.mq;

import com.springboot.mq.common.config.KafkaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties(value = {KafkaProperties.class})
public class MqApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqApplication.class, args);
    }

}

