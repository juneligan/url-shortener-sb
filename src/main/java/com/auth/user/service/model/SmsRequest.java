package com.auth.user.service.model;

import lombok.Data;

@Data
public class SmsRequest {
    private final String phoneNumber;
    private final String message;
}
