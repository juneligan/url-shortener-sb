package com.auth.user.service.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import com.auth.user.validation.ValidPhoneNumber;

@Builder
@Data
public class LoginRequest {
    @ValidPhoneNumber(
            messagePlus63 = "Phone number must start with +63(PH code) and be 13 characters long",
            message63 = "Phone number must start with 63(PH code) and be 12 characters long"
    )
    private String phoneNumber;
    private String email;
    private String password;
}