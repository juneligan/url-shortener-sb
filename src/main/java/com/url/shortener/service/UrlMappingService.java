package com.url.shortener.service;

import com.auth.user.entity.User;
import com.url.shortener.entity.ClickEvent;
import com.url.shortener.entity.UrlMapping;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.service.model.ClickEventResponse;
import com.url.shortener.service.model.UrlMappingResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@AllArgsConstructor
@Service
public class UrlMappingService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    public UrlMappingResponse createShortUrl(String originalUrl, User user) {
        String shortUrl = generateShortUrl(originalUrl);
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedAt(LocalDateTime.now());

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
                        .createdDate(urlMapping.getCreatedAt())
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

    public List<ClickEventResponse> getClickEventByDate(
            String shortUrl, LocalDateTime startDateTime, LocalDateTime endDateTime
    ) {
        return urlMappingRepository.findByShortUrl(shortUrl)
                .map(mapping ->
                        // if not null, find all click events by url mapping and date range
                        clickEventRepository.findByUrlMappingAndClickDateBetween(mapping, startDateTime, endDateTime)
                .stream()
                .collect(Collectors.groupingBy(
                        clickEvent -> clickEvent.getClickDate().toLocalDate(), Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> ClickEventResponse.builder()
                        .clickDate(entry.getKey())
                        .count(entry.getValue())
                        .build()
                )
                .toList())
                .orElse(emptyList());

    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(
            User user, LocalDate startDateTime, LocalDate endDateTime
    ) {
        List<UrlMapping> urlMappings = urlMappingRepository.findAllByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingInAndClickDateBetween(
                urlMappings, startDateTime.atStartOfDay(), endDateTime.plusDays(1).atStartOfDay()
        );
        return clickEvents.stream()
                .collect(Collectors.groupingBy(
                        clickEvent -> clickEvent.getClickDate().toLocalDate(), Collectors.counting()
                ));
    }

    public Optional<UrlMapping> getOriginalUrl(String shortUrl) {
        Optional<UrlMapping> urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        urlMapping.ifPresent(u -> {
            u.setClickCount(u.getClickCount() + 1);
            urlMappingRepository.save(u);

            ClickEvent clickEvent = new ClickEvent();
            clickEvent.setClickDate(LocalDateTime.now());
            clickEvent.setUrlMapping(u);
            clickEventRepository.save(clickEvent);
        });
        return urlMapping;
    }
}
