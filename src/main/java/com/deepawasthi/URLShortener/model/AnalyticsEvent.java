package com.deepawasthi.URLShortener.model;

import java.time.LocalDateTime;

public class AnalyticsEvent {
    private String shortCode;
    private String originalUrl;
    private LocalDateTime clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referer;

    public AnalyticsEvent() {}

    public AnalyticsEvent(String shortCode, String originalUrl, LocalDateTime clickedAt,
                          String ipAddress, String userAgent, String referer) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.clickedAt = clickedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public LocalDateTime getClickedAt() { return clickedAt; }
    public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
}
