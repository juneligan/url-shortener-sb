package com.auth.user.controller;

import com.auth.user.entity.User;
import com.auth.user.service.SmsSenderService;
import com.auth.user.service.model.SmsRequest;
import com.auth.user.service.model.UserDetailsImpl;
import com.url.shortener.service.model.UrlMappingRequest;
import com.url.shortener.service.model.UrlMappingResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import static com.auth.user.utils.UserUtils.getPhoneNumberFromPrincipal;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/sms")
public class SmsSenderController {
    private final SmsSenderService smsSenderService;
    @PostMapping("/send")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> sendSms(@Valid @RequestBody SmsRequest smsRequest, @NonNull Principal principal) {
        String phoneNumber = getPhoneNumberFromPrincipal(((UsernamePasswordAuthenticationToken) principal)
                .getPrincipal());

        return ResponseEntity.ok(smsSenderService.sendMessage(smsRequest, phoneNumber));
    }
}
