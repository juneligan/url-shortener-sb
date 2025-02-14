package com.auth.user.controller;

import com.auth.user.exception.ErrorResponse;
import com.auth.user.service.OtpService;
import com.auth.user.service.UserService;
import com.auth.user.service.model.GenericResponse;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.OtpRequest;
import com.auth.user.service.model.RegisterRequest;
import com.auth.user.service.model.SbResponse;
import com.auth.user.service.model.UserResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final OtpService otpService;

    @PostMapping("/public/login") // use for logging in user with username and password
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(userService.authenticateUser(loginRequest));
    }

    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        GenericResponse<UserResponse> response = userService.registerUser(registerRequest);
        if (response.hasError()) {
            return ResponseEntity.badRequest().body(response.getError());
        }
        return ResponseEntity.ok("User registered successfully!");
    }

    // this will register user if not exists or login and send OTP
    @PostMapping("/public/otp/login") // use for guest type users
    public ResponseEntity<?> loginRegistrationUserForOtp(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Received request to send OTP");
        return ResponseEntity.ok(otpService.sendOtp(loginRequest));
    }

    // this will authenticate user with OTP
    @PostMapping("/public/otp/authenticate")
    public ResponseEntity<?> authenticateOtp(@RequestBody OtpRequest otpRequest) {
        SbResponse result = userService.authenticateUser(otpRequest);
        if (result instanceof ErrorResponse) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
