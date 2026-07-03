package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.AnalyticsEvent;
import com.deepawasthi.URLShortener.model.ClickEvent;
import com.deepawasthi.URLShortener.repository.ClickEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private final ClickEventRepository clickEventRepository;

    public AnalyticsConsumer(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    @KafkaListener(topics = "url.clicks", groupId = "url-shortener-analytics")
    public void consume(AnalyticsEvent event) {
        ClickEvent entity = new ClickEvent(
                event.getShortCode(),
                event.getOriginalUrl(),
                event.getClickedAt(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getReferer());
        clickEventRepository.save(entity);
        log.debug("Persisted click event for {}", event.getShortCode());
    }
}
