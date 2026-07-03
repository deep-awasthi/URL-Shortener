package com.deepawasthi.URLShortener.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic clickEventsTopic() {
        return new NewTopic("url.clicks", 3, (short) 1);
    }
}
