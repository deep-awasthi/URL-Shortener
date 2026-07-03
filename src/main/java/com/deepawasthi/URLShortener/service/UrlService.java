package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.model.UrlDto;

import java.util.List;

public interface UrlService {
    Url generateShortLink(UrlDto urlDto);
    Url getEncodedUrl(String shortCode);
    List<Url> getAllUrls();
    void deleteShortLink(String shortCode);
    List<Url> searchUrls(String query);
    void recordClick(String shortCode, String originalUrl,
                     String ipAddress, String userAgent, String referer);
}
