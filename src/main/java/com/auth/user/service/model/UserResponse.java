package com.auth.user.service.model;

import com.auth.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class UserResponse implements SbResponse {
    private String phoneNumber; // unique
    private String email;
    private String username;
    private String role;
    private Boolean phoneNumberVerified;
    private Boolean emailVerified;
    private LocalDateTime createdAt;

    // transient
    @JsonIgnore
    private User user;

    public static UserResponse build(User user) {
        return UserResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .phoneNumberVerified(user.getPhoneNumberVerified())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .user(user)
                .build();
    }
}
