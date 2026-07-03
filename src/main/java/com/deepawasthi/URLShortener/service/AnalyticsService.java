package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.AnalyticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String TOPIC = "url.clicks";

    private final KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;

    public AnalyticsService(KafkaTemplate<String, AnalyticsEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void recordClick(AnalyticsEvent event) {
        kafkaTemplate.send(TOPIC, event.getShortCode(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send analytics event for {}: {}",
                                event.getShortCode(), ex.getMessage());
                    } else {
                        log.debug("Analytics event sent for {}", event.getShortCode());
                    }
                });
    }
}
