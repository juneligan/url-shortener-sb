package com.auth.user.service.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String phoneNumber;
    private String password;
}
