package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.UserRepository;
import com.auth.user.security.JwtAuthenticationResponse;
import com.auth.user.security.JwtUtils;
import com.auth.user.service.model.OtpRequest;
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

@AllArgsConstructor
@Service
public class UserService {
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private OtpRepository otpRepository;

    public User registerUser(RegisterRequest registerRequest) {
        userRepository.findByPhoneNumber(registerRequest.getPhoneNumber())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Phone number already in use!");
                });
        User user = new User();
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole("ROLE_USER"); // static for now

        return userRepository.save(user);
    }

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhoneNumber(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);
    }

    public JwtAuthenticationResponse authenticateUser(OtpRequest otpRequest) {
        Otp otp = otpRepository.findByOtpAndExpiryTimeAfterAndUser_PhoneNumber(
                otpRequest.getOtp(),
                LocalDateTime.now(),
                otpRequest.getPhoneNumber()
        )
                // invalid if otp is not linked to the phone number
                // invalid if the otp is expired
                // invalid if the otp is not found
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP!"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        otpRequest.getPhoneNumber(),
                        otp.getOtp()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);
    }

    // this will be used for OTP login since the user will not go through registration process
    public User findByPhoneNumberOrRegisterUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    RegisterRequest registerRequest = RegisterRequest.builder().phoneNumber(phoneNumber).build();
                    return registerUser(registerRequest);
                });
    }

    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found with phone number: " + phoneNumber));
    }
}
