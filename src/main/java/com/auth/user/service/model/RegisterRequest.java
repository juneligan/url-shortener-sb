package com.auth.user.service.model;

import com.auth.user.validation.ValidPhoneNumber;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class RegisterRequest {

    @ValidPhoneNumber(
            messagePlus63 = "Phone number must start with +63(PH code) and be 13 characters long",
            message63 = "Phone number must start with 63(PH code) and be 12 characters long"
    )
    private String phoneNumber;
    private String email;
    private String username;
    private String password;
    private Set<String> role;
}
