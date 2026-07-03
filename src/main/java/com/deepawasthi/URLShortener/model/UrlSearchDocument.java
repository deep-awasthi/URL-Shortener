package com.deepawasthi.URLShortener.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "urls")
public class UrlSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String shortCode;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String originalUrl;

    @Field(type = FieldType.Date)
    private LocalDateTime creationDate;

    @Field(type = FieldType.Date)
    private LocalDateTime expirationDate;

    @Field(type = FieldType.Long)
    private long clickCount;

    public UrlSearchDocument() {}

    public UrlSearchDocument(Url url) {
        this.id = url.getId();
        this.shortCode = url.getShortCode();
        this.originalUrl = url.getOriginalUrl();
        this.creationDate = url.getCreationDate();
        this.expirationDate = url.getExpirationDate();
        this.clickCount = url.getClickCount();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }
}
