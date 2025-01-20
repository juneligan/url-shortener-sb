package com.auth.user.service.model;

import lombok.Data;

@Data
public class OtpRequest {
    private String phoneNumber;
    private String otp;
}
