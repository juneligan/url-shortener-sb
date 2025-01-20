package com.auth.user.service.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class RegisterRequest {
    private String phoneNumber;
    private String email;
    private String username;
    private String password;
    private Set<String> role;
}
