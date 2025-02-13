package com.url.shortener.controller;

import com.auth.user.entity.User;
import com.auth.user.service.UserService;
import com.auth.user.service.model.UserDetailsImpl;
import com.url.shortener.service.UrlMappingService;
import com.url.shortener.service.model.ClickEventResponse;
import com.url.shortener.service.model.UrlMappingRequest;
import com.url.shortener.service.model.UrlMappingResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.auth.user.utils.UserUtils.getPhoneNumberFromPrincipal;

@AllArgsConstructor
@RestController
@RequestMapping("/api/urls")
public class UrlMappingController {
    private final UrlMappingService urlMappingService;
    private final UserService userService;

    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingResponse> shortenUrl(
            @RequestBody UrlMappingRequest urlMappingRequest, @NonNull Principal principal
    ) {
        String originalUrl = urlMappingRequest.getOriginalUrl();
        String phoneNumber = getPhoneNumberFromPrincipal(((UsernamePasswordAuthenticationToken) principal)
                .getPrincipal());
        User user = userService.findByPhoneNumber(phoneNumber);
        UrlMappingResponse urlMappingResponse = urlMappingService.createShortUrl(originalUrl, user);

        return ResponseEntity.ok(urlMappingResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAllUrls(@NonNull Principal principal) {
        String phoneNumber = getPhoneNumberFromPrincipal(((UsernamePasswordAuthenticationToken) principal)
                .getPrincipal());
        User user = userService.findByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(urlMappingService.getAllUrls(user));
    }

    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventResponse>> getUrlAnalytics(
            @PathVariable String shortUrl,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime startDateTime = startDate != null ? LocalDateTime.parse(startDate, formatter) : null;
        LocalDateTime endDateTime = endDate != null ? LocalDateTime.parse(endDate, formatter) : null;
        List<ClickEventResponse> clickEvents = urlMappingService.getClickEventByDate(
                shortUrl, startDateTime, endDateTime
        );
        return ResponseEntity.ok(clickEvents);
    }

    @GetMapping("/analytics/total-clicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate, Long>> getUrlAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Principal principal
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String phoneNumber = getPhoneNumberFromPrincipal(((UsernamePasswordAuthenticationToken) principal)
                .getPrincipal());
        User user = userService.findByPhoneNumber(phoneNumber);

        LocalDate startDateTime = startDate != null ? LocalDate.parse(startDate, formatter) : null;
        LocalDate endDateTime = endDate != null ? LocalDate.parse(endDate, formatter) : null;

        Map<LocalDate, Long> totalClicks = urlMappingService.getTotalClicksByUserAndDate(
                user, startDateTime, endDateTime
        );
        return ResponseEntity.ok(totalClicks);
    }
}
