package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.exception.ErrorResponse;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.UserRepository;
import com.auth.user.security.JwtAuthenticationResponse;
import com.auth.user.security.JwtUtils;
import com.auth.user.service.model.OtpRequest;
import com.auth.user.service.model.SbResponse;
import com.auth.user.service.model.UserDetailsImpl;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.RegisterRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserService {
    private static final String REGEX_SANITIZE_PH_CODE = "^\\+?63";
    private static final String PH_NUM_PREFIX = "0";
    public static final String DEFAULT_ROLE_USER = "ROLE_USER";
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private OtpRepository otpRepository;

    public User registerUser(RegisterRequest registerRequest) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(registerRequest.getPhoneNumber());
        userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber)
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Phone number already in use!");
                });
        User user = new User();
        user.setPhoneNumber(sanitizedPhoneNumber);
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setRole(DEFAULT_ROLE_USER); // static for now
        if (registerRequest.getPassword() != null) { // password is optional for otp typed users
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        }

        return userRepository.save(user);
    }

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        return getJwtAuthenticationResponse(loginRequest.getPhoneNumber(), loginRequest.getPassword());
    }

    public SbResponse authenticateUser(OtpRequest otpRequest) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(otpRequest.getPhoneNumber());
        Optional<Otp> otp = otpRepository.findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
                otpRequest.getOtp(),
                LocalDateTime.now(),
                sanitizedPhoneNumber
        );

        // invalid if otp is not linked to the phone number
        // invalid if the otp is expired
        // invalid if the otp is not found
        if (otp.isEmpty()) {
            return ErrorResponse.builder().error("Invalid OTP! not found or expired").build();
        }

        Otp otpEntity = otp.get();
        updatePhoneNumberVerification(otpEntity);
        JwtAuthenticationResponse jwtAuthenticationResponse = getJwtAuthenticationResponse(
                otpRequest.getPhoneNumber(), otpEntity.getOtp()
        );

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);
        return jwtAuthenticationResponse;
    }

    // this will be used for OTP login since the user will not go through registration process
    public User findByPhoneNumberOrRegisterUser(String phoneNumber) {
        // sanitize phone number to remove country code
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(phoneNumber);
        return userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber)
                .orElseGet(() -> {
                    RegisterRequest registerRequest = RegisterRequest.builder()
                            .phoneNumber(sanitizedPhoneNumber)
                            .build();
                    return registerUser(registerRequest);
                });
    }

    public User findByPhoneNumber(String phoneNumber) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(phoneNumber);
        return userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found with phone number: " + phoneNumber));
    }

    private JwtAuthenticationResponse getJwtAuthenticationResponse(String username, String password) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(username);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(sanitizedPhoneNumber, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);
    }

    private void updatePhoneNumberVerification(Otp otpEntity) {
        User user = otpEntity.getUser();
        if (!user.getPhoneNumberVerified()) {
            user.setPhoneNumberVerified(true);
            userRepository.save(user);
        }
    }

    public static String getSanitizedPhoneNumber(String phoneNumber) {
        return phoneNumber.replaceFirst(REGEX_SANITIZE_PH_CODE, PH_NUM_PREFIX);
    }
}
