package com.url.shortener.service.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationMessage {
    private String otp;
    private String phoneNumber;
    private String text;
    private String to;
}
