package com.url.shortener.controller;

import com.url.shortener.entity.UrlMapping;
import com.url.shortener.service.UrlMappingService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@AllArgsConstructor
@RestController
public class RedirectController {

    private UrlMappingService urlMappingService;

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> redirect(@PathVariable String shortUrl) {
        Optional<UrlMapping> urlMapping = urlMappingService.getOriginalUrl(shortUrl);

        return urlMapping.map(u -> {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.LOCATION, u.getOriginalUrl());
            return ResponseEntity.status(302).headers(httpHeaders).build();
        })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
