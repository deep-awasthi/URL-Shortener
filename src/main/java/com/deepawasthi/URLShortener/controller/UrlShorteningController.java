package com.deepawasthi.URLShortener.controller;

import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.model.UrlDto;
import com.deepawasthi.URLShortener.model.UrlErrorResponseDto;
import com.deepawasthi.URLShortener.model.UrlResponseDto;
import com.deepawasthi.URLShortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class UrlShorteningController {
    @Autowired
    private UrlService urlService;

    public ResponseEntity<?> generateShortLink(@RequestBody UrlDto urlDto){
        Url urlToRet = urlService.generateShortLink(urlDto);
        if(urlToRet!=null){

            UrlResponseDto urlResponseDto = new UrlResponseDto();
            urlResponseDto.setOriginalUrl(urlToRet.getOriginalUrl());
            urlResponseDto.setExpirationDate(LocalDateTime.parse(urlDto.getExpirationDate()));
            urlResponseDto.setShortLink(urlDto.getUrl());

            return new ResponseEntity<UrlResponseDto>(urlResponseDto, HttpStatus.OK);
        }

        UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
        urlErrorResponseDto.setStatus("404");
        urlErrorResponseDto.setError("Error");
        return  new ResponseEntity<UrlErrorResponseDto>(urlErrorResponseDto, HttpStatus.OK);
    }
}
