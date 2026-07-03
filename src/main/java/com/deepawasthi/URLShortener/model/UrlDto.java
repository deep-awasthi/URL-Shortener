package com.deepawasthi.URLShortener.model;

import jakarta.validation.constraints.NotBlank;

public class UrlDto {

    @NotBlank(message = "url is required")
    private String url;
    private String expirationDate;
    private String customCode;

    public UrlDto() {}

    public UrlDto(String url, String expirationDate, String customCode) {
        this.url = url;
        this.expirationDate = expirationDate;
        this.customCode = customCode;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public String getCustomCode() { return customCode; }
    public void setCustomCode(String customCode) { this.customCode = customCode; }
}
