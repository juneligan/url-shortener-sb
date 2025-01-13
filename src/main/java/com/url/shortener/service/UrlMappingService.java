package com.url.shortener.service;

import com.auth.user.entity.User;
import com.url.shortener.entity.UrlMapping;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.service.model.UrlMappingResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UrlMappingService {

    private final UrlMappingRepository urlMappingRepository;

    public UrlMappingResponse createShortUrl(String originalUrl, User user) {
        String shortUrl = generateShortUrl(originalUrl);
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());

        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);
        return convertToResponse(savedUrlMapping);
    }

    public List<UrlMappingResponse> getAllUrls(User user) {
        List<UrlMapping> urlMappings = urlMappingRepository.findAllByUser(user);
        return urlMappings.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private UrlMappingResponse convertToResponse(UrlMapping urlMapping) {
        return UrlMappingResponse.builder()
                        .id(urlMapping.getId())
                        .originalUrl(urlMapping.getOriginalUrl())
                        .shortUrl(urlMapping.getShortUrl())
                        .clickCount(urlMapping.getClickCount())
                        .createdDate(urlMapping.getCreatedDate())
                        .username(urlMapping.getUser().getUsername())
                        .phoneNumber(urlMapping.getUser().getPhoneNumber())
                        .build();
    }

    private String generateShortUrl(String originalUrl) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder shortUrl = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            shortUrl.append(characters.charAt(random.nextInt(characters.length())));
        }

        return shortUrl.toString();
    }
}
