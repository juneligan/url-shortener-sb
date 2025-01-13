package com.auth.user.service.model;

import lombok.Data;

import java.util.Set;

@Data
public class RegisterRequest {
    private String phoneNumber;
    private String email;
    private String username;
    private String password;
    private Set<String> role;
}
