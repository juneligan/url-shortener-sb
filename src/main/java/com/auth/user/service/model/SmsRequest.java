package com.auth.user.service.model;

import com.auth.user.validation.ValidMessageLength;
import com.auth.user.validation.ValidPhoneNumber;
import lombok.Data;

@Data
public class SmsRequest {
    @ValidPhoneNumber(
            messagePlus63 = "Phone number must start with +63(PH code) and be 13 characters long",
            message63 = "Phone number must start with 63(PH code) and be 12 characters long"
    )
    private final String phoneNumber;

    @ValidMessageLength
    private final String message;
}
