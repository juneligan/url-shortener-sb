package com.auth.user.security;

import com.auth.user.service.model.SbResponse;
import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class JwtAuthenticationResponse implements SbResponse {
    private String accessToken;
}
