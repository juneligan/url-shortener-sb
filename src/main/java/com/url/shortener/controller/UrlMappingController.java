package com.url.shortener.controller;

import com.auth.user.entity.User;
import com.auth.user.service.UserService;
import com.auth.user.service.model.UserDetailsImpl;
import com.url.shortener.service.UrlMappingService;
import com.url.shortener.service.model.UrlMappingRequest;
import com.url.shortener.service.model.UrlMappingResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
        String phoneNumber = getPhoneNumber(((UsernamePasswordAuthenticationToken) principal).getPrincipal());
        User user = userService.findByPhoneNumber(phoneNumber);
        UrlMappingResponse urlMappingResponse = urlMappingService.createShortUrl(originalUrl, user);

        return ResponseEntity.ok(urlMappingResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAllUrls(@NonNull Principal principal) {
        String phoneNumber = getPhoneNumber(((UsernamePasswordAuthenticationToken) principal).getPrincipal());
        User user = userService.findByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(urlMappingService.getAllUrls(user));
    }

    private static String getPhoneNumber(@NonNull Object principal) {
        return ((UserDetailsImpl) principal).getPhoneNumber();
    }
}
