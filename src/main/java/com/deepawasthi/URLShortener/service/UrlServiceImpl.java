package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.model.UrlDto;
import com.deepawasthi.URLShortener.repository.UrlRepository;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class UrlServiceImpl implements UrlService{
    @Autowired
    private UrlRepository urlRepository;

    @Override
    public Url generateShortLink(UrlDto urlDto){
        if (StringUtils.isNotEmpty(urlDto.getUrl())){
            String encodedUrl = encodeUrl(urlDto.getUrl());
            Url urlToPersit = new Url();
            urlToPersit.setCreationDate(LocalDateTime.now());
            urlToPersit.setOriginalUrl(urlDto.getUrl());
            urlToPersit.setShortLink(encodedUrl);
            urlToPersit.setExpirationDate(getExpirationalDate(urlDto.getExpirationDate(), urlToPersit.getCreationDate()));
            Url urlToRet = persistShortLink(urlToPersit);

            if(urlToRet!=null){
                return urlToRet;
            }
            return null;
        }
            return null;
    }

    private LocalDateTime getExpirationalDate(String expirationDate, LocalDateTime creationDate) {
        if(StringUtils.isBlank(expirationDate)){
            return creationDate.plusSeconds(120);
        }
        LocalDateTime expirationDateToRet = LocalDate.parse(expirationDate).atStartOfDay();
        return expirationDateToRet;
    }

    private String encodeUrl(String url) {
        String encodedUrl = "";
        LocalDateTime time = LocalDateTime.now();
        encodedUrl = Hashing.murmur3_32().hashString((url.concat(time.toString())), StandardCharsets.UTF_8).toString();

        return encodedUrl;
    }

    @Override
    public Url persistShortLink(Url url) {
        Url urlToRet = urlRepository.save(url);
        return urlToRet;
    }

    @Override
    public Url getEncodedUrl(String url) {
        Url urlToRet = urlRepository.findByShortLink(url);
        return urlToRet;
    }

    @Override
    public void deleteShortLink(Url url) {
        urlRepository.delete(url);
    }
}
