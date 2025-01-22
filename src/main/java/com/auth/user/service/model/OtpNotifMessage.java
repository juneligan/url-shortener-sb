package com.auth.user.service.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtpNotifMessage {
    private String phoneNumber;
    private String otp;
}
