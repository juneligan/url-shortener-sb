package com.auth.user.service.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class LoginRequest {
    @NonNull
    private String phoneNumber;
    private String password;
}
